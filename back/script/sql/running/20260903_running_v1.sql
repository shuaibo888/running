-- 燃赛路跑首版业务表。
-- 执行前请确认当前数据库为 running，并先备份已有数据。

CREATE TABLE IF NOT EXISTS run_user_profile
(
    id                bigint       NOT NULL COMMENT '主键',
    tenant_id         varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id           bigint       NOT NULL COMMENT '关联平台用户ID',
    nickname          varchar(64)  NULL COMMENT '昵称',
    avatar_url        varchar(500) NULL COMMENT '头像地址',
    gender            char(1)      NULL DEFAULT '0' COMMENT '性别：0未知，1男，2女',
    birth_date        date         NULL COMMENT '出生日期',
    height_cm         decimal(5,2) NULL COMMENT '身高（厘米）',
    weight_kg         decimal(5,2) NULL COMMENT '体重（千克）',
    province_code     varchar(32)  NULL COMMENT '常驻省份行政区划编码',
    province_name     varchar(64)  NULL COMMENT '常驻省份名称',
    city_code         varchar(32)  NULL COMMENT '常驻城市行政区划编码',
    city_name         varchar(64)  NULL COMMENT '常驻城市名称',
    profile_completed tinyint(1)   NOT NULL DEFAULT 0 COMMENT '档案是否完整',
    version           bigint       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_dept       bigint       NULL COMMENT '创建部门',
    create_by         bigint       NULL COMMENT '创建者',
    create_time       datetime     NULL COMMENT '创建时间',
    update_by         bigint       NULL COMMENT '更新者',
    update_time       datetime     NULL COMMENT '更新时间',
    del_flag          bigint       NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_user_profile_tenant_user (tenant_id, user_id),
    KEY idx_run_user_profile_city (tenant_id, city_code)
) ENGINE = InnoDB COMMENT = '小程序用户运动档案';

CREATE TABLE IF NOT EXISTS run_wechat_identity
(
    id              bigint      NOT NULL COMMENT '主键',
    tenant_id       varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    app_id          varchar(32) NOT NULL COMMENT '微信小程序AppID',
    openid_hash     char(64)    NOT NULL COMMENT 'openid的HMAC-SHA-256摘要',
    unionid_hash    char(64)    NULL COMMENT 'unionid的HMAC-SHA-256摘要',
    user_id         bigint      NOT NULL COMMENT '关联平台用户ID',
    last_login_time datetime    NULL COMMENT '最近登录时间',
    create_dept     bigint      NULL COMMENT '创建部门',
    create_by       bigint      NULL COMMENT '创建者',
    create_time     datetime    NULL COMMENT '创建时间',
    update_by       bigint      NULL COMMENT '更新者',
    update_time     datetime    NULL COMMENT '更新时间',
    del_flag        bigint      NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_wechat_tenant_app_openid (tenant_id, app_id, openid_hash),
    KEY idx_run_wechat_tenant_app_user (tenant_id, app_id, user_id),
    KEY idx_run_wechat_unionid (tenant_id, unionid_hash)
) ENGINE = InnoDB COMMENT = '微信小程序身份映射';

CREATE TABLE IF NOT EXISTS run_phone_identity
(
    id              bigint      NOT NULL COMMENT '主键',
    tenant_id       varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    phone_hash      char(64)    NOT NULL COMMENT '手机号HMAC-SHA-256摘要',
    user_id         bigint      NOT NULL COMMENT '关联平台用户ID',
    last_login_time datetime    NULL COMMENT '最近登录时间',
    create_dept     bigint      NULL COMMENT '创建部门',
    create_by       bigint      NULL COMMENT '创建者',
    create_time     datetime    NULL COMMENT '创建时间',
    update_by       bigint      NULL COMMENT '更新者',
    update_time     datetime    NULL COMMENT '更新时间',
    del_flag        bigint      NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_phone_tenant_phone (tenant_id, phone_hash),
    UNIQUE KEY uk_run_phone_tenant_user (tenant_id, user_id)
) ENGINE = InnoDB COMMENT = '手机号身份摘要映射';

CREATE TABLE IF NOT EXISTS run_workout
(
    id                    bigint         NOT NULL COMMENT '主键',
    tenant_id             varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id               bigint         NOT NULL COMMENT '平台用户ID',
    client_workout_id     varchar(64)    NOT NULL COMMENT '客户端创建幂等ID',
    sport_type            varchar(32)    NOT NULL DEFAULT 'RUNNING' COMMENT '运动类型',
    status                varchar(20)    NOT NULL COMMENT 'RUNNING/PAUSED/COMPLETED/FAILED',
    active_flag           tinyint        NULL COMMENT '进行中固定为1，结束后置空，用于唯一约束',
    started_at            datetime(3)    NOT NULL COMMENT '开始时间',
    paused_started_at     datetime(3)    NULL COMMENT '本次暂停开始时间',
    finished_at           datetime(3)    NULL COMMENT '结束时间',
    elapsed_seconds       int unsigned   NOT NULL DEFAULT 0 COMMENT '有效运动时长秒',
    paused_seconds        int unsigned   NOT NULL DEFAULT 0 COMMENT '累计暂停秒',
    distance_meters       decimal(12,2)  NOT NULL DEFAULT 0 COMMENT '服务端有效距离米',
    calories_kcal         decimal(10,2)  NOT NULL DEFAULT 0 COMMENT '服务端估算卡路里',
    avg_pace_seconds      int unsigned   NULL COMMENT '平均配速秒/公里',
    weight_kg             decimal(5,2)   NOT NULL COMMENT '本次计算采用体重',
    calorie_algorithm     varchar(64)    NOT NULL COMMENT '卡路里算法版本',
    route_id              bigint         NULL COMMENT '本次运动计入的虚拟文化线路ID',
    start_latitude        decimal(10,7)  NOT NULL COMMENT '起点纬度GCJ-02',
    start_longitude       decimal(10,7)  NOT NULL COMMENT '起点经度GCJ-02',
    end_latitude          decimal(10,7)  NULL COMMENT '终点纬度GCJ-02',
    end_longitude         decimal(10,7)  NULL COMMENT '终点经度GCJ-02',
    point_count           int unsigned   NOT NULL DEFAULT 1 COMMENT '收到轨迹点数',
    invalid_point_count   int unsigned   NOT NULL DEFAULT 0 COMMENT '无效轨迹点数',
    last_track_seq        int            NOT NULL DEFAULT 0 COMMENT '最后接收轨迹序号',
    last_point_time       datetime(3)    NOT NULL COMMENT '最后接收轨迹时间',
    finish_request_id     varchar(64)    NULL COMMENT '客户端结束幂等ID',
    city_resolve_status   varchar(16)    NULL COMMENT '城市解析状态：PENDING/PROCESSING/RETRY/COMPLETED/FAILED',
    city_resolve_attempts tinyint unsigned NOT NULL DEFAULT 0 COMMENT '城市解析尝试次数',
    city_resolve_next_at  datetime       NULL COMMENT '下次城市解析时间',
    city_resolved_at      datetime       NULL COMMENT '城市解析完成时间',
    city_resolve_error    varchar(64)    NULL COMMENT '城市解析错误分类，不保存敏感响应',
    version               bigint         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_dept           bigint         NULL COMMENT '创建部门',
    create_by             bigint         NULL COMMENT '创建者',
    create_time           datetime       NULL COMMENT '创建时间',
    update_by             bigint         NULL COMMENT '更新者',
    update_time           datetime       NULL COMMENT '更新时间',
    del_flag              bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_workout_client (tenant_id, user_id, client_workout_id),
    UNIQUE KEY uk_run_workout_active (tenant_id, user_id, active_flag),
    KEY idx_run_workout_user_started (tenant_id, user_id, started_at),
    KEY idx_run_workout_route (tenant_id, route_id, started_at),
    KEY idx_run_workout_status (tenant_id, status, started_at),
    KEY idx_run_workout_city_resolve (tenant_id, city_resolve_status, city_resolve_next_at)
) ENGINE = InnoDB COMMENT = '单次运动记录';

CREATE TABLE IF NOT EXISTS run_track_batch
(
    id                      bigint         NOT NULL COMMENT '主键',
    tenant_id               varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    workout_id              bigint         NOT NULL COMMENT '运动记录ID',
    user_id                 bigint         NOT NULL COMMENT '平台用户ID',
    client_batch_id         varchar(64)    NOT NULL COMMENT '客户端批次幂等ID',
    start_sequence          int            NOT NULL COMMENT '起始轨迹序号',
    end_sequence            int            NOT NULL COMMENT '结束轨迹序号',
    point_count             int unsigned   NOT NULL COMMENT '批次轨迹点数',
    invalid_point_count     int unsigned   NOT NULL DEFAULT 0 COMMENT '批次无效点数',
    distance_meters         decimal(10,2)  NOT NULL DEFAULT 0 COMMENT '批次有效距离米',
    create_dept             bigint         NULL COMMENT '创建部门',
    create_by               bigint         NULL COMMENT '创建者',
    create_time             datetime       NULL COMMENT '创建时间',
    update_by               bigint         NULL COMMENT '更新者',
    update_time             datetime       NULL COMMENT '更新时间',
    del_flag                bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_track_batch_client (tenant_id, workout_id, client_batch_id),
    KEY idx_run_track_batch_workout (tenant_id, workout_id, start_sequence)
) ENGINE = InnoDB COMMENT = '运动轨迹上传批次';

CREATE TABLE IF NOT EXISTS run_track_point
(
    id                      bigint         NOT NULL COMMENT '主键',
    tenant_id               varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    workout_id              bigint         NOT NULL COMMENT '运动记录ID',
    batch_id                bigint         NULL COMMENT '轨迹批次ID，起点为空',
    sequence_no             int            NOT NULL COMMENT '客户端单调递增序号',
    recorded_at             datetime(3)    NOT NULL COMMENT '客户端定位时间',
    latitude                decimal(10,7)  NOT NULL COMMENT '纬度GCJ-02',
    longitude               decimal(10,7)  NOT NULL COMMENT '经度GCJ-02',
    accuracy_meters         decimal(8,2)   NULL COMMENT '定位精度米',
    reported_speed_mps      decimal(8,3)   NULL COMMENT '设备上报速度米/秒，仅供审计',
    altitude_meters         decimal(9,2)   NULL COMMENT '海拔米',
    direction_degrees       decimal(6,2)   NULL COMMENT '方向角度',
    valid_flag              tinyint(1)     NOT NULL DEFAULT 1 COMMENT '是否计入距离',
    invalid_reason          varchar(32)    NULL COMMENT '无效原因',
    segment_distance_meters decimal(10,2)  NOT NULL DEFAULT 0 COMMENT '相对上个有效点计入距离',
    create_dept             bigint         NULL COMMENT '创建部门',
    create_by               bigint         NULL COMMENT '创建者',
    create_time             datetime       NULL COMMENT '创建时间',
    update_by               bigint         NULL COMMENT '更新者',
    update_time             datetime       NULL COMMENT '更新时间',
    del_flag                bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_track_point_sequence (tenant_id, workout_id, sequence_no),
    KEY idx_run_track_point_time (tenant_id, workout_id, recorded_at),
    KEY idx_run_track_point_batch (tenant_id, batch_id)
) ENGINE = InnoDB COMMENT = '运动轨迹点';

CREATE TABLE IF NOT EXISTS run_workout_city
(
    id               bigint         NOT NULL COMMENT '主键',
    tenant_id        varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    workout_id       bigint         NOT NULL COMMENT '运动记录ID',
    user_id          bigint         NOT NULL COMMENT '平台用户ID',
    province_code    varchar(16)    NULL COMMENT '省级行政区划码',
    province_name    varchar(64)    NULL COMMENT '运动时省份名称快照',
    city_code        varchar(16)    NOT NULL COMMENT '市级行政区划码',
    city_name        varchar(64)    NOT NULL COMMENT '运动时城市名称快照',
    sample_latitude  decimal(10,7)  NOT NULL COMMENT '命中该城市的采样纬度GCJ-02',
    sample_longitude decimal(10,7)  NOT NULL COMMENT '命中该城市的采样经度GCJ-02',
    resolved_at      datetime       NOT NULL COMMENT '腾讯位置服务解析时间',
    create_dept      bigint         NULL COMMENT '创建部门',
    create_by        bigint         NULL COMMENT '创建者',
    create_time      datetime       NULL COMMENT '创建时间',
    update_by        bigint         NULL COMMENT '更新者',
    update_time      datetime       NULL COMMENT '更新时间',
    del_flag         bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_workout_city (tenant_id, workout_id, city_code),
    KEY idx_run_workout_city_user (tenant_id, user_id, city_code)
) ENGINE = InnoDB COMMENT = '单次运动覆盖城市';

CREATE TABLE IF NOT EXISTS run_city_footprint
(
    id               bigint         NOT NULL COMMENT '主键',
    tenant_id        varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id          bigint         NOT NULL COMMENT '平台用户ID',
    province_code    varchar(16)    NULL COMMENT '省级行政区划码',
    province_name    varchar(64)    NULL COMMENT '最近解析省份名称',
    city_code        varchar(16)    NOT NULL COMMENT '市级行政区划码',
    city_name        varchar(64)    NOT NULL COMMENT '最近解析城市名称',
    first_workout_id bigint         NOT NULL COMMENT '首次覆盖运动ID',
    first_reached_at datetime       NOT NULL COMMENT '首次覆盖时间',
    last_workout_id  bigint         NOT NULL COMMENT '最近覆盖运动ID',
    last_reached_at  datetime       NOT NULL COMMENT '最近覆盖时间',
    workout_count    int unsigned   NOT NULL DEFAULT 1 COMMENT '覆盖该城市的有效运动次数',
    version          bigint         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_dept      bigint         NULL COMMENT '创建部门',
    create_by        bigint         NULL COMMENT '创建者',
    create_time      datetime       NULL COMMENT '创建时间',
    update_by        bigint         NULL COMMENT '更新者',
    update_time      datetime       NULL COMMENT '更新时间',
    del_flag         bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_city_footprint (tenant_id, user_id, city_code),
    KEY idx_run_city_footprint_recent (tenant_id, user_id, last_reached_at)
) ENGINE = InnoDB COMMENT = '用户城市运动足迹';

CREATE TABLE IF NOT EXISTS run_virtual_route
(
    id                    bigint         NOT NULL COMMENT '主键',
    tenant_id             varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    route_code            varchar(32)    NOT NULL COMMENT '线路业务编码',
    route_name            varchar(64)    NOT NULL COMMENT '线路名称',
    subtitle              varchar(128)   NULL COMMENT '线路副标题',
    description           varchar(1000)  NULL COMMENT '线路介绍',
    start_city            varchar(64)    NOT NULL COMMENT '起点城市',
    end_city              varchar(64)    NOT NULL COMMENT '终点城市',
    total_distance_meters decimal(14,2)  NOT NULL COMMENT '线路总里程米',
    theme_color           varchar(16)    NOT NULL DEFAULT '#ff6a00' COMMENT '主题色',
    cover_url             varchar(500)   NULL COMMENT '审核后的正式封面地址',
    status                char(1)        NOT NULL DEFAULT '0' COMMENT '0启用1停用',
    sort_order            int            NOT NULL DEFAULT 0 COMMENT '排序',
    create_dept           bigint         NULL COMMENT '创建部门',
    create_by             bigint         NULL COMMENT '创建者',
    create_time           datetime       NULL COMMENT '创建时间',
    update_by             bigint         NULL COMMENT '更新者',
    update_time           datetime       NULL COMMENT '更新时间',
    del_flag              bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_virtual_route_code (tenant_id, route_code),
    KEY idx_run_virtual_route_status (tenant_id, status, sort_order)
) ENGINE = InnoDB COMMENT = '虚拟文化线路';

CREATE TABLE IF NOT EXISTS run_virtual_route_node
(
    id                        bigint         NOT NULL COMMENT '主键',
    tenant_id                 varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    route_id                  bigint         NOT NULL COMMENT '线路ID',
    node_code                 varchar(32)    NOT NULL COMMENT '节点业务编码',
    node_name                 varchar(64)    NOT NULL COMMENT '节点名称',
    threshold_distance_meters decimal(14,2)  NOT NULL COMMENT '累计到达里程米',
    story_title               varchar(128)   NULL COMMENT '典故标题',
    story_content             varchar(1500)  NULL COMMENT '审核后的典故内容',
    medal_name                varchar(64)    NULL COMMENT '节点勋章名称',
    reward_points             int            NOT NULL DEFAULT 0 COMMENT '节点积分奖励',
    sort_order                int            NOT NULL DEFAULT 0 COMMENT '节点顺序',
    create_dept               bigint         NULL COMMENT '创建部门',
    create_by                 bigint         NULL COMMENT '创建者',
    create_time               datetime       NULL COMMENT '创建时间',
    update_by                 bigint         NULL COMMENT '更新者',
    update_time               datetime       NULL COMMENT '更新时间',
    del_flag                  bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_route_node_code (tenant_id, route_id, node_code),
    UNIQUE KEY uk_run_route_node_order (tenant_id, route_id, sort_order),
    KEY idx_run_route_node_threshold (tenant_id, route_id, threshold_distance_meters)
) ENGINE = InnoDB COMMENT = '虚拟文化线路节点';

CREATE TABLE IF NOT EXISTS run_user_route_progress
(
    id                          bigint         NOT NULL COMMENT '主键',
    tenant_id                   varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id                     bigint         NOT NULL COMMENT '平台用户ID',
    route_id                    bigint         NOT NULL COMMENT '线路ID',
    accumulated_distance_meters decimal(14,2)  NOT NULL DEFAULT 0 COMMENT '已累计有效里程米',
    reached_node_order          int            NOT NULL DEFAULT 0 COMMENT '已到达最高节点顺序',
    selected_flag               tinyint        NULL COMMENT '当前线路固定为1，非当前置空',
    last_workout_id             bigint         NULL COMMENT '最后计入的运动ID',
    version                     bigint         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_dept                 bigint         NULL COMMENT '创建部门',
    create_by                   bigint         NULL COMMENT '创建者',
    create_time                 datetime       NULL COMMENT '创建时间',
    update_by                   bigint         NULL COMMENT '更新者',
    update_time                 datetime       NULL COMMENT '更新时间',
    del_flag                    bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_user_route (tenant_id, user_id, route_id),
    UNIQUE KEY uk_run_user_selected_route (tenant_id, user_id, selected_flag),
    KEY idx_run_user_route_progress (tenant_id, user_id, accumulated_distance_meters)
) ENGINE = InnoDB COMMENT = '用户虚拟线路进度';

CREATE TABLE IF NOT EXISTS run_achievement
(
    id                bigint         NOT NULL COMMENT '主键',
    tenant_id         varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    achievement_code  varchar(32)    NOT NULL COMMENT '成就业务编码',
    achievement_name  varchar(64)    NOT NULL COMMENT '成就名称',
    description       varchar(300)   NOT NULL COMMENT '达成说明',
    metric_type       varchar(32)    NOT NULL COMMENT 'TOTAL_CALORIES/TOTAL_DISTANCE/WORKOUT_COUNT/CITY_COUNT/CONSECUTIVE_DAYS',
    threshold_value   decimal(14,2)  NOT NULL COMMENT '达成阈值',
    medal_level       varchar(16)    NOT NULL DEFAULT 'BRONZE' COMMENT '勋章等级',
    icon_url          varchar(500)   NULL COMMENT '审核后的正式图标地址',
    status            char(1)        NOT NULL DEFAULT '0' COMMENT '0启用1停用',
    sort_order        int            NOT NULL DEFAULT 0 COMMENT '排序',
    create_dept       bigint         NULL COMMENT '创建部门',
    create_by         bigint         NULL COMMENT '创建者',
    create_time       datetime       NULL COMMENT '创建时间',
    update_by         bigint         NULL COMMENT '更新者',
    update_time       datetime       NULL COMMENT '更新时间',
    del_flag          bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_achievement_code (tenant_id, achievement_code),
    KEY idx_run_achievement_status (tenant_id, status, sort_order)
) ENGINE = InnoDB COMMENT = '里程碑成就定义';

CREATE TABLE IF NOT EXISTS run_user_achievement
(
    id                bigint         NOT NULL COMMENT '主键',
    tenant_id         varchar(20)    NOT NULL DEFAULT '000000' COMMENT '租户编号',
    user_id           bigint         NOT NULL COMMENT '平台用户ID',
    achievement_id    bigint         NOT NULL COMMENT '成就ID',
    unlocked_at       datetime       NOT NULL COMMENT '解锁时间',
    achieved_value    decimal(14,2)  NOT NULL COMMENT '解锁时实际累计值',
    source_workout_id bigint         NULL COMMENT '触发解锁的运动ID',
    create_dept       bigint         NULL COMMENT '创建部门',
    create_by         bigint         NULL COMMENT '创建者',
    create_time       datetime       NULL COMMENT '创建时间',
    update_by         bigint         NULL COMMENT '更新者',
    update_time       datetime       NULL COMMENT '更新时间',
    del_flag          bigint         NOT NULL DEFAULT 0 COMMENT '删除标志',
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_user_achievement (tenant_id, user_id, achievement_id),
    KEY idx_run_user_achievement_time (tenant_id, user_id, unlocked_at)
) ENGINE = InnoDB COMMENT = '用户已解锁成就';

INSERT INTO run_virtual_route
    (id, tenant_id, route_code, route_name, subtitle, description, start_city, end_city,
     total_distance_meters, theme_color, status, sort_order, create_time, del_flag)
VALUES
    (1001, '000000', 'GRAND_CANAL', '京杭大运河', '千年水脉 · 南北远征', '沿京杭大运河的重要城市推进，逐站了解运河文化。', '北京', '杭州', 1794000.00, '#176b87', '0', 10, NOW(), 0),
    (1002, '000000', 'GREAT_WALL', '万里长城', '山河长卷 · 守望前行', '以真实运动里程推进长城文化线路，解锁沿线关隘故事。', '山海关', '嘉峪关', 21196000.00, '#b95f32', '0', 20, NOW(), 0),
    (1003, '000000', 'LONG_MARCH', '重走长征路', '不忘初心 · 坚定前行', '用每天的真实运动积累推进长征文化线路。', '瑞金', '延安', 12500000.00, '#9f2f2b', '0', 30, NOW(), 0)
ON DUPLICATE KEY UPDATE
    route_name = VALUES(route_name), subtitle = VALUES(subtitle), description = VALUES(description),
    start_city = VALUES(start_city), end_city = VALUES(end_city),
    total_distance_meters = VALUES(total_distance_meters), theme_color = VALUES(theme_color),
    status = VALUES(status), sort_order = VALUES(sort_order);

INSERT INTO run_virtual_route_node
    (id, tenant_id, route_id, node_code, node_name, threshold_distance_meters, story_title,
     story_content, medal_name, reward_points, sort_order, create_time, del_flag)
VALUES
    (1101, '000000', 1001, 'BEIJING', '北京', 0.00, '运河北首', '从北京出发，沿千年水脉开启远征。', '运河启程者', 0, 1, NOW(), 0),
    (1102, '000000', 1001, 'LIAOCHENG', '聊城', 450000.00, '江北水城', '抵达聊城，解锁运河沿线城市故事。', '水城行者', 100, 2, NOW(), 0),
    (1103, '000000', 1001, 'HANGZHOU', '杭州', 1794000.00, '运河南端', '抵达杭州，完成京杭大运河线路。', '运河领航者', 500, 3, NOW(), 0),
    (1201, '000000', 1002, 'SHANHAIGUAN', '山海关', 0.00, '天下第一关', '从山海关开始长城文化远征。', '长城启程者', 0, 1, NOW(), 0),
    (1202, '000000', 1002, 'JIAYUGUAN', '嘉峪关', 21196000.00, '河西雄关', '完成万里长城线路。', '长城守望者', 800, 2, NOW(), 0),
    (1301, '000000', 1003, 'RUIJIN', '瑞金', 0.00, '长征起点', '从瑞金开始用真实运动重走长征路。', '初心行者', 0, 1, NOW(), 0),
    (1302, '000000', 1003, 'YANAN', '延安', 12500000.00, '胜利会师', '抵达延安，完成重走长征路线路。', '长征践行者', 800, 2, NOW(), 0)
ON DUPLICATE KEY UPDATE
    node_name = VALUES(node_name), threshold_distance_meters = VALUES(threshold_distance_meters),
    story_title = VALUES(story_title), story_content = VALUES(story_content),
    medal_name = VALUES(medal_name), reward_points = VALUES(reward_points), sort_order = VALUES(sort_order);

INSERT INTO run_achievement
    (id, tenant_id, achievement_code, achievement_name, description, metric_type,
     threshold_value, medal_level, status, sort_order, create_time, del_flag)
VALUES
    (2001, '000000', 'FIRST_RUN', '燃动初程', '完成第一次有效运动', 'WORKOUT_COUNT', 1.00, 'BRONZE', '0', 10, NOW(), 0),
    (2002, '000000', 'CALORIE_500', '热力初燃', '累计消耗达到 500 千卡', 'TOTAL_CALORIES', 500.00, 'BRONZE', '0', 20, NOW(), 0),
    (2003, '000000', 'CALORIE_5000', '燃力进阶', '累计消耗达到 5000 千卡', 'TOTAL_CALORIES', 5000.00, 'SILVER', '0', 30, NOW(), 0),
    (2004, '000000', 'DISTANCE_10K', '十公里行者', '累计有效距离达到 10 公里', 'TOTAL_DISTANCE', 10000.00, 'BRONZE', '0', 40, NOW(), 0),
    (2005, '000000', 'DISTANCE_100K', '百公里征途', '累计有效距离达到 100 公里', 'TOTAL_DISTANCE', 100000.00, 'GOLD', '0', 50, NOW(), 0),
    (2006, '000000', 'WORKOUT_30', '持之以恒', '累计完成 30 次有效运动', 'WORKOUT_COUNT', 30.00, 'SILVER', '0', 60, NOW(), 0)
ON DUPLICATE KEY UPDATE
    achievement_name = VALUES(achievement_name), description = VALUES(description),
    metric_type = VALUES(metric_type), threshold_value = VALUES(threshold_value),
    medal_level = VALUES(medal_level), status = VALUES(status), sort_order = VALUES(sort_order);
