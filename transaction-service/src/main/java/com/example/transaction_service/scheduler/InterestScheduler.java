package com.example.transaction_service.scheduler;
import com.example.transaction_service.service.impl.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterestScheduler {

    private final InterestService interestService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyInterest() {

        interestService.creditDailyInterest();

    }

//    @Scheduled(fixedRate = 60000)
//    public void runDailyInterest() {
//        interestService.creditDailyInterest();
//    }
}
