package victor.training.performance.profiling;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name("victor.training.performance.LoanLookup")
@Label("Loan Lookup")
@Category({"Application", "Loan"})
@Description("Emitted after a Loan is loaded by id; visible in JFR recordings (JMC / async-profiler --jfr).")
public class LoanLookupEvent extends Event {
  @Label("Loan id")
  public long loanId;

  @Label("Status")
  public String status;
}
