package org.dromara.running.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启用城市足迹持久化重试任务。 */
@Configuration
@EnableScheduling
public class RunSchedulingConfig {
}
