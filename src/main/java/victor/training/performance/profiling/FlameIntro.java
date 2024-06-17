package victor.training.performance.profiling;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FlameIntro {
  @GetMapping("dummy")
  private static void entry() throws InterruptedException {
    f();
    g();
  }

  private static void f() throws InterruptedException {
    Thread.sleep(2010);
  }

  private static void g() throws InterruptedException {
    Thread.sleep(4010);
  }

}
