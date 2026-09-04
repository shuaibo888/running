package org.dromara.running.event;

import lombok.RequiredArgsConstructor;
import org.dromara.running.service.IRunCityFootprintService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 事务提交后异步解析足迹，外部地图故障不会回滚运动结算。 */
@RequiredArgsConstructor
@Component
public class RunWorkoutCompletedEventListener {

    private final IRunCityFootprintService footprintService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompleted(RunWorkoutCompletedEvent event) {
        footprintService.process(event.tenantId(), event.workoutId());
    }
}
