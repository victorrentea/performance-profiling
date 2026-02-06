package victor.training.performance.profiling.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import static java.lang.String.join;
import static java.util.Collections.list;
import static java.util.stream.Collectors.joining;

@Slf4j
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1000) // run before Spring's Security Filter Chain
public class PrintRequestHeadersFilter extends HttpFilter {

  @Override
  protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
    var headersMap = getHeadersString(list(request.getHeaderNames()), name -> list(request.getHeaders(name)));
    log.info("\nRequest Headers for {}\n{}", request.getRequestURI(), headersMap);
    chain.doFilter(request, response);
  }

  private static String getHeadersString(Collection<String> names, Function<String, Collection<String>> valueByName) {
    return names.stream()
        .sorted()
        .map(name -> "\t" + name + ": " + join("; ", valueByName.apply(name)))
        .collect(joining("\n"));
  }

}