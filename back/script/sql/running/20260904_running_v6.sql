-- 燃赛路跑 V6：建立迁移登记事实，后续执行 SQL 前必须先查询本表
-- 前置条件：v1 至 v5 已执行且不可变。
-- 本文件执行成功后同样不可回改，后续变化继续新增 v7。

CREATE TABLE run_schema_history
(
    version_no       varchar(16)  NOT NULL COMMENT '迁移版本，例如 v1',
    script_name      varchar(128) NOT NULL COMMENT '迁移脚本文件名',
    checksum_sha256  char(64)     NULL COMMENT '脚本 SHA-256；历史脚本必须核对',
    description      varchar(300) NOT NULL COMMENT '迁移说明',
    installed_on     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    PRIMARY KEY (version_no),
    UNIQUE KEY uk_run_schema_history_script (script_name)
) ENGINE = InnoDB COMMENT = '燃赛路跑数据库迁移登记';

INSERT INTO run_schema_history (version_no, script_name, checksum_sha256, description)
VALUES
    ('v1', '20260903_running_v1.sql', 'B4FEF38A5D16DE28EC3171E7FECE5F6E540EA3395AA7758381329F3EA06421B4', '首版用户、运动、轨迹、足迹、线路与成就表'),
    ('v2', '20260904_running_v2.sql', 'CD80EC0C2B7D19751C28D273F9557503B3494C45C466433A56E19D34FB5E2ECB', '连续运动天数与城市数成就'),
    ('v3', '20260904_running_v3.sql', '0330BBA770E859E0C3790629D996D2DAB7C52A9443AA57C30F7699FAC04ED0C5', '非跑步运动类型与 MET 结算'),
    ('v4', '20260904_running_v4.sql', '9B07E2078CC57CA3F4593793A91A8AA98798010CF8A72070253060BE077CBDA3', '积分账户、流水与成就奖励'),
    ('v5', '20260904_running_v5.sql', 'AE63F94F6666F6A0EB751245C41423CEE09F92D98E9E956A58FB837BF8682A09', '排行榜省市归属快照');

-- v6 是创建本登记表的引导迁移，先登记版本和文件名；其脚本哈希由后续执行器在库外核对。
INSERT INTO run_schema_history (version_no, script_name, checksum_sha256, description)
VALUES ('v6', '20260904_running_v6.sql', NULL, '建立迁移登记与历史脚本哈希基线');
