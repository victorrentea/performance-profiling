package victor.training.performance.profiling.extra;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlamegraphIntro {




  @GetMapping("dummy")
  public void dummy() throws InterruptedException {
    entry();
  }
  private void entry() throws InterruptedException {
    f();
    g();
  }
  private void f() throws InterruptedException {
    Thread.sleep(2010);
  }
  private void g() throws InterruptedException {
    Thread.sleep(4010);
  }

}
