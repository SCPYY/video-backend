-- 内容分类测试数据及历史内容分类迁移，可重复执行
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO content_categories (type, name, description, sort_order, status) VALUES
(1, '都市甜宠', '现代都市甜宠短剧', 10, 1),
(1, '古装言情', '古装爱情短剧', 20, 1),
(1, '都市爽文', '都市逆袭和爽文短剧', 30, 1),
(1, '重生复仇', '重生复仇题材短剧', 40, 1),
(1, '悬疑推理', '悬疑推理短剧', 50, 1),
(2, '都市恋爱', '都市恋爱互动影游', 10, 1),
(2, '仙侠恋爱', '仙侠恋爱互动影游', 20, 1),
(2, '悬疑惊悚', '悬疑惊悚互动影游', 30, 1),
(2, '宫斗策略', '宫斗策略互动影游', 40, 1),
(2, '科幻冒险', '科幻冒险互动影游', 50, 1),
(2, '校园恋爱', '校园恋爱互动影游', 60, 1)
ON DUPLICATE KEY UPDATE description=VALUES(description), status=1;

-- 将现有内容的分类名称映射到分类 ID；未匹配的历史分类自动建立为同类型分类。
INSERT INTO content_categories (type, name, sort_order, status)
SELECT DISTINCT type, TRIM(category), 100, 1
FROM contents
WHERE status <> -1 AND category IS NOT NULL AND TRIM(category) <> ''
  AND NOT EXISTS (SELECT 1 FROM content_categories c WHERE c.type=contents.type AND c.name=TRIM(contents.category));

UPDATE contents x
JOIN content_categories c ON c.type=x.type AND c.name=TRIM(x.category)
SET x.category_id=c.id
WHERE x.category IS NOT NULL AND TRIM(x.category) <> '';

COMMIT;
