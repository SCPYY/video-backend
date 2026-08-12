package com.project.module.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.common.exception.BusinessException;
import com.project.common.exception.ErrorCode;
import com.project.module.admin.service.AdminLogService;
import com.project.module.entitlement.service.EntitlementService;
import com.project.module.order.dto.OrderVO;
import com.project.module.order.entity.Order;
import com.project.module.order.mapper.OrderMapper;
import com.project.module.product.entity.Product;
import com.project.module.product.mapper.ProductMapper;
import com.project.module.user.mapper.UserMapper;
import com.project.module.wallet.dto.WalletAdjustRequest;
import com.project.module.wallet.dto.WalletTransactionVO;
import com.project.module.wallet.dto.WalletVO;
import com.project.module.wallet.entity.UserWallet;
import com.project.module.wallet.entity.WalletTransaction;
import com.project.module.wallet.mapper.UserWalletMapper;
import com.project.module.wallet.mapper.WalletTransactionMapper;
import com.project.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    public static final String PLATFORM_COIN = "PLATFORM_COIN";

    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final EntitlementService entitlementService;
    private final AdminLogService adminLogService;

    @Override
    @Transactional
    public List<WalletVO> listWallets(Long userId) {
        userWalletMapper.ensureWallet(userId, PLATFORM_COIN);
        return userWalletMapper.selectList(new LambdaQueryWrapper<UserWallet>()
                        .eq(UserWallet::getUserId, userId)
                        .orderByAsc(UserWallet::getCurrency))
                .stream().map(this::toWalletVO).collect(Collectors.toList());
    }

    @Override
    public Page<WalletTransactionVO> pageTransactions(Long userId, String currency, String type,
                                                      Integer pageNum, Integer size) {
        String normalizedCurrency = currency == null || currency.isBlank() ? null : normalizeCurrency(currency);
        String normalizedType = normalizeOptional(type);
        int safePage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safeSize = size == null ? 20 : Math.max(1, Math.min(size, 100));

        Page<WalletTransaction> source = walletTransactionMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<WalletTransaction>()
                        .eq(WalletTransaction::getUserId, userId)
                        .eq(normalizedCurrency != null, WalletTransaction::getCurrency, normalizedCurrency)
                        .eq(normalizedType != null, WalletTransaction::getType, normalizedType)
                        .orderByDesc(WalletTransaction::getId));

        Page<WalletTransactionVO> result = new Page<>();
        BeanUtils.copyProperties(source, result, "records");
        result.setRecords(source.getRecords().stream().map(this::toTransactionVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    @Transactional
    public OrderVO payOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        String idempotencyKey = "WALLET_PAYMENT:" + orderId;
        WalletTransaction completed = findByIdempotencyKey(idempotencyKey);
        if (completed != null && order.getStatus() == 1) {
            return toOrderVO(order);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_PAY);
        }
        if (order.getExpiredAt() != null && order.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ORDER_EXPIRED);
        }

        String currency = PLATFORM_COIN;
        UserWallet wallet = lockWallet(userId, currency);
        ensureWalletAvailable(wallet);

        BigDecimal before = wallet.getAvailableBalance();
        if (before.compareTo(order.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }
        BigDecimal after = before.subtract(order.getAmount());
        userWalletMapper.updateAvailableBalance(wallet.getId(), after);
        insertTransaction(wallet, "PAYMENT", "OUT", order.getAmount(), before, after,
                "ORDER", String.valueOf(orderId), idempotencyKey, "钱包支付订单 " + order.getOrderNo());

        order.setPaymentMethod("WALLET");
        order.setStatus(1);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);
        entitlementService.grant(userId, order.getProductId());

        log.info("钱包支付成功: userId={}, orderId={}, amount={} {}", userId, orderId,
                order.getAmount(), currency);
        return toOrderVO(order);
    }

    @Override
    @Transactional
    public void refundOrder(Long orderId) {
        Order order = orderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == 3) {
            return;
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_REFUND);
        }

        if ("WALLET".equalsIgnoreCase(order.getPaymentMethod())) {
            String idempotencyKey = "WALLET_REFUND:" + orderId;
            if (findByIdempotencyKey(idempotencyKey) == null) {
                String currency = PLATFORM_COIN;
                UserWallet wallet = lockWallet(order.getUserId(), currency);
                ensureWalletAvailable(wallet, true);
                BigDecimal before = wallet.getAvailableBalance();
                BigDecimal after = before.add(order.getAmount());
                userWalletMapper.updateAvailableBalance(wallet.getId(), after);
                insertTransaction(wallet, "REFUND", "IN", order.getAmount(), before, after,
                        "ORDER", String.valueOf(orderId), idempotencyKey, "订单退款 " + order.getOrderNo());
            }
        }

        order.setStatus(3);
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public WalletVO adjustBalance(Long adminId, WalletAdjustRequest request) {
        if (userMapper.selectById(request.getUserId()) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String currency = PLATFORM_COIN;
        String direction = request.getDirection().toUpperCase(Locale.ROOT);
        String transactionType = request.getTransactionType().toUpperCase(Locale.ROOT);
        if ("RECHARGE".equals(transactionType) && !"IN".equals(direction)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String idempotencyKey = "ADMIN_" + transactionType + ":" + request.getRequestNo();

        WalletTransaction existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            UserWallet existingWallet = userWalletMapper.selectById(existing.getWalletId());
            return toWalletVO(existingWallet);
        }

        UserWallet wallet = lockWallet(request.getUserId(), currency);
        if (Integer.valueOf(3).equals(wallet.getStatus())
                || (Integer.valueOf(2).equals(wallet.getStatus())
                && !("RECHARGE".equals(transactionType) && "IN".equals(direction)))) {
            throw new BusinessException(ErrorCode.WALLET_DISABLED);
        }
        BigDecimal before = wallet.getAvailableBalance();
        BigDecimal after = "IN".equals(direction)
                ? before.add(request.getAmount())
                : before.subtract(request.getAmount());
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.WALLET_INSUFFICIENT_BALANCE);
        }

        userWalletMapper.updateAvailableBalance(wallet.getId(), after);
        insertTransaction(wallet, transactionType, direction, request.getAmount(), before, after,
                "ADMIN", String.valueOf(adminId), idempotencyKey, request.getRemark());

        wallet.setAvailableBalance(after);
        adminLogService.log(adminId, "ADJUST", "WALLET", String.valueOf(wallet.getId()), before, after);
        return toWalletVO(wallet);
    }

    @Override
    public Page<WalletVO> pageAdminWallets(String currency, Integer status, Integer pageNum, Integer size) {
        String normalized = currency == null || currency.isBlank() ? null : normalizeCurrency(currency);
        Page<UserWallet> source = userWalletMapper.selectPage(new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                        size == null ? 20 : Math.min(100, Math.max(1, size))),
                new LambdaQueryWrapper<UserWallet>().eq(normalized != null, UserWallet::getCurrency, normalized)
                        .eq(status != null, UserWallet::getStatus, status).orderByDesc(UserWallet::getId));
        Page<WalletVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toWalletVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Page<WalletTransactionVO> pageAdminTransactions(Long userId, String currency, String type,
                                                            Integer pageNum, Integer size) {
        String normalizedCurrency = currency == null || currency.isBlank() ? null : normalizeCurrency(currency);
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toUpperCase(Locale.ROOT);
        Page<WalletTransaction> source = walletTransactionMapper.selectPage(
                new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum, size == null ? 20 : Math.min(100, Math.max(1, size))),
                new LambdaQueryWrapper<WalletTransaction>().eq(userId != null, WalletTransaction::getUserId, userId)
                        .eq(normalizedCurrency != null, WalletTransaction::getCurrency, normalizedCurrency)
                        .eq(normalizedType != null, WalletTransaction::getType, normalizedType)
                        .orderByDesc(WalletTransaction::getId));
        Page<WalletTransactionVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toTransactionVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<WalletVO> listAdminUserWallets(Long userId) {
        if (userMapper.selectById(userId) == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return listWallets(userId);
    }

    @Override
    @Transactional
    public WalletVO updateWalletStatus(Long adminId, Long userId, String currency, Integer status, String reason) {
        if (status == null || status < 1 || status > 3) throw new BusinessException(ErrorCode.PARAM_ERROR);
        UserWallet wallet = lockWallet(userId, normalizeCurrency(currency));
        if (status == 3 && (wallet.getAvailableBalance().signum() != 0 || wallet.getFrozenBalance().signum() != 0)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Integer before = wallet.getStatus();
        wallet.setStatus(status);
        wallet.setStatusReason(status == 1 ? null : reason);
        wallet.setDisabledAt(status == 1 ? null : LocalDateTime.now());
        wallet.setDisabledBy(status == 1 ? null : adminId);
        userWalletMapper.updateById(wallet);
        adminLogService.log(adminId, "STATUS", "WALLET", String.valueOf(wallet.getId()), before, status);
        return toWalletVO(wallet);
    }

    private UserWallet lockWallet(Long userId, String currency) {
        userWalletMapper.ensureWallet(userId, currency);
        UserWallet wallet = userWalletMapper.selectForUpdate(userId, currency);
        if (wallet == null) {
            throw new BusinessException(ErrorCode.WALLET_NOT_FOUND);
        }
        return wallet;
    }

    private void ensureWalletAvailable(UserWallet wallet) {
        ensureWalletAvailable(wallet, false);
    }

    private void ensureWalletAvailable(UserWallet wallet, boolean allowFrozen) {
        if (Integer.valueOf(3).equals(wallet.getStatus())
                || (!Integer.valueOf(1).equals(wallet.getStatus()) && !(allowFrozen && Integer.valueOf(2).equals(wallet.getStatus())))) {
            throw new BusinessException(ErrorCode.WALLET_DISABLED);
        }
    }

    private WalletTransaction findByIdempotencyKey(String key) {
        return walletTransactionMapper.selectOne(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getIdempotencyKey, key));
    }

    private void insertTransaction(UserWallet wallet, String type, String direction, BigDecimal amount,
                                   BigDecimal before, BigDecimal after, String relatedType,
                                   String relatedId, String idempotencyKey, String remark) {
        WalletTransaction tx = new WalletTransaction();
        tx.setTransactionNo("WLT" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT));
        tx.setWalletId(wallet.getId());
        tx.setUserId(wallet.getUserId());
        tx.setCurrency(wallet.getCurrency());
        tx.setType(type);
        tx.setDirection(direction);
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setRelatedType(relatedType);
        tx.setRelatedId(relatedId);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setRemark(remark);
        try {
            walletTransactionMapper.insert(tx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.WALLET_DUPLICATE_TRANSACTION);
        }
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
        if (!PLATFORM_COIN.equals(normalized)) {
            throw new BusinessException(ErrorCode.WALLET_CURRENCY_NOT_SUPPORTED);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private WalletVO toWalletVO(UserWallet wallet) {
        WalletVO vo = new WalletVO();
        BeanUtils.copyProperties(wallet, vo);
        return vo;
    }

    private WalletTransactionVO toTransactionVO(WalletTransaction tx) {
        WalletTransactionVO vo = new WalletTransactionVO();
        BeanUtils.copyProperties(tx, vo);
        return vo;
    }

    private OrderVO toOrderVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        Product product = productMapper.selectById(order.getProductId());
        vo.setProductName(product != null ? product.getName() : null);
        return vo;
    }
}
