-- 燃赛路跑 V4：正式积分账户与不可重复积分流水
-- 前置条件：已依次执行 v1、v2、v3；这些历史迁移禁止回改。
-- 本文件执行成功后同样视为不可变，后续调整继续新增 v5。

CREATE TABLE IF NOT EXISTS run_point_account
(
    id                       bigint       NOT NULL COMMENT '主键',
    tenant_id                varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id                  bigint       NOT NULL COMMENT '平台用户ID',
    balance                  int          NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    total_earned             int          NOT NULL DEFAULT 0 COMMENT '历史累计获得积分',
    current_checkin_streak   int          NOT NULL DEFAULT 0 COMMENT '当前连续签到天数',
    last_checkin_date        date         NULL COMMENT '最近签到日期（Asia/Shanghai）',
    version                  bigint       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_dept              bigint       NULL COMMENT '创建部门',
    create_by                bigint       NULL COMMENT '创建者',
    create_time              datetime     NULL COMMENT '创建时间',
    update_by                bigint       NULL COMMENT '更新者',
    update_time              datetime     NULL COMMENT '更新时间',
    del_flag                 bigint       NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_point_account_user (tenant_id, user_id)
) ENGINE = InnoDB COMMENT = '用户积分账户';

CREATE TABLE IF NOT EXISTS run_point_transaction
(
    id                bigint        NOT NULL COMMENT '主键',
    tenant_id         varchar(20)   NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id           bigint        NOT NULL COMMENT '平台用户ID',
    biz_type          varchar(32)   NOT NULL COMMENT 'CHECKIN/ROUTE_NODE/ACHIEVEMENT',
    biz_key           varchar(96)   NOT NULL COMMENT '同类业务幂等键',
    delta_points      int           NOT NULL COMMENT '本次积分变化',
    balance_after     int           NOT NULL COMMENT '变化后余额快照',
    title             varchar(64)   NOT NULL COMMENT '流水标题',
    description       varchar(200)  NULL COMMENT '流水说明',
    occurred_at       datetime      NOT NULL COMMENT '积分发生时间',
    source_workout_id bigint        NULL COMMENT '触发运动ID',
    create_dept       bigint        NULL COMMENT '创建部门',
    create_by         bigint        NULL COMMENT '创建者',
    create_time       datetime      NULL COMMENT '创建时间',
    update_by         bigint        NULL COMMENT '更新者',
    update_time       datetime      NULL COMMENT '更新时间',
    del_flag          bigint        NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_point_transaction_biz (tenant_id, user_id, biz_type, biz_key),
    KEY idx_run_point_transaction_user_time (tenant_id, user_id, occurred_at)
) ENGINE = InnoDB COMMENT = '用户积分流水';

ALTER TABLE run_achievement
    ADD COLUMN reward_points int NOT NULL DEFAULT 0 COMMENT '解锁积分奖励' AFTER icon_url;

UPDATE run_achievement
SET reward_points = CASE achievement_code
    WHEN 'FIRST_RUN' THEN 50
    WHEN 'CALORIE_500' THEN 50
    WHEN 'CALORIE_5000' THEN 120
    WHEN 'DISTANCE_10K' THEN 50
    WHEN 'DISTANCE_100K' THEN 200
    WHEN 'WORKOUT_30' THEN 200
    WHEN 'STREAK_3' THEN 40
    WHEN 'STREAK_7' THEN 100
    WHEN 'CITY_3' THEN 80
    WHEN 'CITY_10' THEN 300
    ELSE reward_points
END
WHERE tenant_id = '000000' AND del_flag = 0;
