package victor.training.performance.profile.showcase.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@Component
public class SomeFilterYouDidntKnowAbout implements Filter {

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      if (httpRequest.getRequestURI().contains("leak8")) {
         log.debug("doFilter stuff");
         chain.doFilter(request, response);
      } else {
         chain.doFilter(request, response);
      }
   }
}
