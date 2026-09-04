-- 燃赛路跑 V3：非跑步运动类型与按时长/MET 结算
-- 前置条件：已依次执行 20260903_running_v1.sql、20260904_running_v2.sql
-- 迁移原则：v1、v2 已进入本地环境，禁止回改；本文件执行后也应视为不可变更。

CREATE TABLE IF NOT EXISTS run_sport_type
(
    id                   bigint        NOT NULL COMMENT '主键',
    tenant_id            varchar(20)   NOT NULL DEFAULT '000000' COMMENT '租户编号',
    sport_code           varchar(32)   NOT NULL COMMENT '稳定运动编码',
    sport_name           varchar(64)   NOT NULL COMMENT '前端展示名称',
    tracking_mode        varchar(16)   NOT NULL COMMENT 'GPS/DURATION',
    met_value            decimal(5,2)  NULL COMMENT 'MET 强度快照来源；GPS 运动可为空',
    calorie_algorithm    varchar(64)   NOT NULL COMMENT '卡路里算法版本',
    min_duration_minutes smallint      NOT NULL DEFAULT 5 COMMENT '允许的最短时长（分钟）',
    max_duration_minutes smallint      NOT NULL DEFAULT 600 COMMENT '允许的最长时长（分钟）',
    status               char(1)       NOT NULL DEFAULT '0' COMMENT '状态：0启用，1停用',
    sort_order           int           NOT NULL DEFAULT 0 COMMENT '展示顺序',
    create_dept          bigint        NULL COMMENT '创建部门',
    create_by            bigint        NULL COMMENT '创建者',
    create_time          datetime      NULL COMMENT '创建时间',
    update_by            bigint        NULL COMMENT '更新者',
    update_time          datetime      NULL COMMENT '更新时间',
    del_flag             bigint        NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_sport_type_code (tenant_id, sport_code),
    KEY idx_run_sport_type_status (tenant_id, tracking_mode, status, sort_order)
) ENGINE = InnoDB COMMENT = '可运营运动类型与卡路里规则';

INSERT INTO run_sport_type
    (id, tenant_id, sport_code, sport_name, tracking_mode, met_value, calorie_algorithm,
     min_duration_minutes, max_duration_minutes, status, sort_order, create_time, del_flag)
VALUES
    (3001, '000000', 'RUNNING', '户外跑步', 'GPS', NULL, 'RUNNING_WEIGHT_DISTANCE_V1', 5, 600, '0', 10, NOW(), 0),
    (3002, '000000', 'WALKING', '健走', 'DURATION', 3.80, 'MET_DURATION_V1', 5, 600, '0', 20, NOW(), 0),
    (3003, '000000', 'CYCLING', '骑行', 'DURATION', 7.50, 'MET_DURATION_V1', 5, 600, '0', 30, NOW(), 0),
    (3004, '000000', 'ROPE_SKIPPING', '跳绳', 'DURATION', 11.00, 'MET_DURATION_V1', 5, 180, '0', 40, NOW(), 0),
    (3005, '000000', 'BADMINTON', '羽毛球', 'DURATION', 5.50, 'MET_DURATION_V1', 5, 360, '0', 50, NOW(), 0),
    (3006, '000000', 'STRENGTH', '力量训练', 'DURATION', 5.00, 'MET_DURATION_V1', 5, 240, '0', 60, NOW(), 0),
    (3007, '000000', 'YOGA', '瑜伽', 'DURATION', 2.50, 'MET_DURATION_V1', 5, 240, '0', 70, NOW(), 0)
ON DUPLICATE KEY UPDATE
    sport_name = VALUES(sport_name),
    tracking_mode = VALUES(tracking_mode),
    met_value = VALUES(met_value),
    calorie_algorithm = VALUES(calorie_algorithm),
    min_duration_minutes = VALUES(min_duration_minutes),
    max_duration_minutes = VALUES(max_duration_minutes),
    status = VALUES(status),
    sort_order = VALUES(sort_order);

ALTER TABLE run_workout
    ADD COLUMN sport_name varchar(64) NOT NULL DEFAULT '户外跑步' COMMENT '本次运动名称快照' AFTER sport_type,
    ADD COLUMN record_source varchar(16) NOT NULL DEFAULT 'GPS' COMMENT '记录来源：GPS/MANUAL' AFTER sport_name,
    ADD COLUMN met_value decimal(5,2) NULL COMMENT '本次 MET 规则快照' AFTER calorie_algorithm,
    ADD COLUMN ranking_eligible tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否进入地区排行榜' AFTER met_value,
    MODIFY COLUMN start_latitude decimal(10,7) NULL COMMENT '起点纬度GCJ-02；按时长运动为空',
    MODIFY COLUMN start_longitude decimal(10,7) NULL COMMENT '起点经度GCJ-02；按时长运动为空',
    MODIFY COLUMN last_point_time datetime(3) NULL COMMENT '最后接收轨迹时间；按时长运动为空',
    ADD KEY idx_run_workout_user_completed (tenant_id, user_id, status, finished_at);
