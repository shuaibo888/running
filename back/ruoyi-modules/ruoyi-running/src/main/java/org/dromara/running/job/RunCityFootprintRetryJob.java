package org.dromara.running.job;

import lombok.RequiredArgsConstructor;
import org.dromara.running.service.IRunCityFootprintService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期重试因腾讯位置服务或网络故障未解析的城市足迹。 */
@RequiredArgsConstructor
@Component
public class RunCityFootprintRetryJob {

    private final IRunCityFootprintService footprintService;

    @Scheduled(initialDelayString = "${running.city-footprint.initial-delay-ms:60000}",
        fixedDelayString = "${running.city-footprint.retry-delay-ms:300000}")
    public void retryPending() {
        footprintService.processPending();
    }
}
