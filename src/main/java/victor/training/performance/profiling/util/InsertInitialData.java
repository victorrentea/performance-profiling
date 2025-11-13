package victor.training.performance.profiling.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import victor.training.performance.profiling.entity.Loan;
import victor.training.performance.profiling.entity.Loan.ApprovalStep;
import victor.training.performance.profiling.entity.Loan.Status;
import victor.training.performance.profiling.repo.LoanRepo;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsertInitialData {
  private final LoanRepo loanRepo;

  @EventListener(ApplicationStartedEvent.class)
  public void insertInitialData() {
    ApprovalStep step1 = new ApprovalStep().setName("Pre-Scan Client").setStatus(Status.APPROVED);
    ApprovalStep step2 = new ApprovalStep().setName("Credit Registry").setStatus(Status.DECLINED);
    loanRepo.save(new Loan()
        .setId(1L)
        .setTitle("4Porche")
        .setSteps(List.of(step1, step2)));
  }
}
