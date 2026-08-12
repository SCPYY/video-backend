package com.project.module.wallet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.module.order.dto.OrderVO;
import com.project.module.wallet.dto.WalletAdjustRequest;
import com.project.module.wallet.dto.WalletTransactionVO;
import com.project.module.wallet.dto.WalletVO;

import java.util.List;

public interface WalletService {

    List<WalletVO> listWallets(Long userId);

    Page<WalletTransactionVO> pageTransactions(Long userId, String currency, String type,
                                               Integer page, Integer size);

    OrderVO payOrder(Long userId, Long orderId);

    void refundOrder(Long orderId);

    WalletVO adjustBalance(Long adminId, WalletAdjustRequest request);
    Page<WalletVO> pageAdminWallets(String currency, Integer status, Integer pageNum, Integer size);
    Page<WalletTransactionVO> pageAdminTransactions(Long userId, String currency, String type, Integer pageNum, Integer size);

    List<WalletVO> listAdminUserWallets(Long userId);

    WalletVO updateWalletStatus(Long adminId, Long userId, String currency, Integer status, String reason);
}
