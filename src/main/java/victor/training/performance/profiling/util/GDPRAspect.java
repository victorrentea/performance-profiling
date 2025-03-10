package victor.training.performance.profiling.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.dialect.H2Dialect;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
@Aspect
public class GDPRAspect {
  private final RestTemplate restTemplate;

  /**
   * The value of the DTO fields marked by this annotation will be cleared (set to null)
   * if the current user does not have the required jurisdiction.
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface VisibleFor {
    String value();
  }

  @Around("@within(org.springframework.web.bind.annotation.RestController))")
  public Object clearNonVisibleFields(ProceedingJoinPoint pjp) throws Throwable {
    Object responseDto = pjp.proceed(); // 85% = actual method call
    if (responseDto == null) {
      return null;
    }
    if (!responseDto.getClass().getPackageName().startsWith("victor")) {
      return responseDto;
    }

    List<Field> sensitiveFields = getAnnotatedFields(responseDto);
    if (sensitiveFields.isEmpty()) {
      return responseDto; // nothing to do, no annotated fields in Dto
    }

    String userRole = fetchUserRole(); // network call 12% of my total time
    // better solutions to avoid fetching the userRole every time:
    // a) caching for 60 minutes> this cache hit% can be very low
    // b) extract role from a signed JWT token (AT) - no network call

    clearSensitiveFields(responseDto, userRole, sensitiveFields);
    return responseDto;
  }

  private String fetchUserRole() {
    try {
      // DB flagRepo.findById()
      // initially this was a call to my own DB = very fast
      return restTemplate.getForObject("http://localhost:9999/jurisdiction", String.class);
      // lots of requests hit my api
    } catch (RestClientException e) {
      throw new RuntimeException("No Jurisdiction", e);
    }
  }

  private static void clearSensitiveFields(Object result, String userJurisdiction, List<Field> fieldsToClear) throws IllegalAccessException {
    for (Field field : fieldsToClear) {
      String requiredJurisdiction = field.getAnnotation(VisibleFor.class).value();
      if (!requiredJurisdiction.equals(userJurisdiction)) {
        field.set(result, null);
      }
    }
  }

  private static List<Field> getAnnotatedFields(Object result) {
    List<Field> annotatedFields = new ArrayList<>();
    for (Field field : result.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      VisibleFor annot = field.getAnnotation(VisibleFor.class);
      if (annot != null) {
        annotatedFields.add(field);
      }
    }
    return annotatedFields;
  }
}
