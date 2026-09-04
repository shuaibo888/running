-- 燃赛路跑 V2：连续运动与城市足迹成就定义
-- 前置条件：已执行 20260903_running_v1.sql
-- 迁移原则：v1 已进入本地环境，禁止回改；后续数据库变化只能新增版本文件。

INSERT INTO run_achievement
    (id, tenant_id, achievement_code, achievement_name, description, metric_type,
     threshold_value, medal_level, status, sort_order, create_time, del_flag)
VALUES
    (2007, '000000', 'STREAK_3', '三日燃动', '最长连续运动达到 3 天', 'CONSECUTIVE_DAYS', 3.00, 'BRONZE', '0', 70, NOW(), 0),
    (2008, '000000', 'STREAK_7', '七日不辍', '最长连续运动达到 7 天', 'CONSECUTIVE_DAYS', 7.00, 'SILVER', '0', 80, NOW(), 0),
    (2009, '000000', 'CITY_3', '城市初探', '累计在 3 个城市留下有效运动足迹', 'CITY_COUNT', 3.00, 'BRONZE', '0', 90, NOW(), 0),
    (2010, '000000', 'CITY_10', '十城行者', '累计在 10 个城市留下有效运动足迹', 'CITY_COUNT', 10.00, 'GOLD', '0', 100, NOW(), 0)
ON DUPLICATE KEY UPDATE
    achievement_name = VALUES(achievement_name),
    description = VALUES(description),
    metric_type = VALUES(metric_type),
    threshold_value = VALUES(threshold_value),
    medal_level = VALUES(medal_level),
    status = VALUES(status),
    sort_order = VALUES(sort_order);
