import org.junit.jupiter.api.Test;

public class DummyTest {

  @Test
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
