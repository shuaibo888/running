-- 燃赛路跑 V5：冻结每次运动的排行榜地区归属，避免用户改档案后历史成绩跨地区漂移
-- 前置条件：v1、v2、v3、v4 均已执行并视为不可变。
-- 本文件执行成功后同样不可回改，后续调整继续新增 v6。

ALTER TABLE run_workout
    ADD COLUMN ranking_province_code varchar(32) NULL COMMENT '运动创建时排行榜省份编码快照' AFTER ranking_eligible,
    ADD COLUMN ranking_province_name varchar(64) NULL COMMENT '运动创建时排行榜省份名称快照' AFTER ranking_province_code,
    ADD COLUMN ranking_city_code varchar(32) NULL COMMENT '运动创建时排行榜城市编码快照' AFTER ranking_province_name,
    ADD COLUMN ranking_city_name varchar(64) NULL COMMENT '运动创建时排行榜城市名称快照' AFTER ranking_city_code;

-- 历史记录只能按迁移当时的用户档案补录；迁移后的新记录均在创建运动时冻结地区。
UPDATE run_workout w
INNER JOIN run_user_profile p
        ON p.tenant_id = w.tenant_id
       AND p.user_id = w.user_id
       AND p.del_flag = 0
SET w.ranking_province_code = p.province_code,
    w.ranking_province_name = p.province_name,
    w.ranking_city_code = p.city_code,
    w.ranking_city_name = p.city_name
WHERE w.del_flag = 0
  AND (w.ranking_province_code IS NULL OR w.ranking_city_code IS NULL);

CREATE INDEX idx_run_workout_ranking_city
    ON run_workout (tenant_id, ranking_eligible, status, ranking_city_code, finished_at, user_id);

CREATE INDEX idx_run_workout_ranking_province
    ON run_workout (tenant_id, ranking_eligible, status, ranking_province_code, finished_at, user_id);
