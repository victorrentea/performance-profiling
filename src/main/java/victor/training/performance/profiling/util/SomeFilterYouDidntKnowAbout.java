package victor.training.performance.profiling.util;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@Slf4j
@Component
public class SomeFilterYouDidntKnowAbout implements Filter {

   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      // logeaza mai jos urlul accesat cu tot cu rolu userului luat dintrun header http
      log.debug("Request URL: {}, User Role: {}", httpRequest.getRequestURI(), httpRequest.getHeader("X-User-Role"));

      if (httpRequest.getRequestURI().contains("leak8")) {

         log.debug("doFilter stuff");
         chain.doFilter(request, response);
      } else {
         chain.doFilter(request, response);
      }
   }
}
