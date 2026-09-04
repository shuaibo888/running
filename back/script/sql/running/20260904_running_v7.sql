-- 燃赛路跑 V7：头像对象存储关联
-- 前置条件：v1 至 v6 已执行且不可变。
-- 本文件执行成功后不可回改，后续变化继续新增 v8。

ALTER TABLE run_user_profile
    ADD COLUMN avatar_oss_id bigint NULL COMMENT '头像对象存储记录ID' AFTER avatar_url,
    ADD KEY idx_run_user_profile_avatar_oss (tenant_id, avatar_oss_id);

INSERT INTO run_schema_history (version_no, script_name, checksum_sha256, description)
VALUES ('v7', '20260904_running_v7.sql', NULL, '头像对象存储关联');
