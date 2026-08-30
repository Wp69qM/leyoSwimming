ALTER TABLE `custom_package_config`
ADD COLUMN `config_key` VARCHAR(20) NOT NULL DEFAULT 'global' COMMENT '配置唯一标识，全局仅允许一条';

ALTER TABLE `custom_package_config`
ADD CONSTRAINT `uk_custom_package_config_key` UNIQUE (`config_key`);
