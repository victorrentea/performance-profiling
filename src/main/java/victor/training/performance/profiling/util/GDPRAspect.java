package victor.training.performance.profiling.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
public class GDPRAspect {  // could be an HttpFilter running before/after your endpoint
  private final RestTemplate restTemplate;

  @Retention(RetentionPolicy.RUNTIME)
  public @interface VisibleFor {
    String value();
  }
//  @AfterReturning("execution(* victor.training.performance.profiling..*.*(..))")
//  public Object clearNonVisibleFields(JoinPoint pjp, Object responseDto) throws Throwable {

  @Around("@within(org.springframework.web.bind.annotation.RestController))") // intercept all methods of a @RestController
  public Object clearNonVisibleFields(ProceedingJoinPoint pjp) throws Throwable {
    Object responseDto = pjp.proceed(); // delegate to the real method that was actually invoked
    if (responseDto == null) {
      return null;
    }
    if (!responseDto.getClass().getPackageName().startsWith("victor")) {
      return responseDto;
    }

    List<Field> sensitiveFields = getAnnotatedFields(responseDto);
    if (sensitiveFields.isEmpty()) {
      return responseDto; // TODO move earlier
    }

    String userRole = fetchUserRole(); // 10% network call

    clearSensitiveFields(responseDto, userRole, sensitiveFields);

    return responseDto;
  }

  private String fetchUserRole() {
    try {
      return restTemplate.getForObject("http://localhost:9999/jurisdiction", String.class);
    } catch (RestClientException e) {
      throw new RuntimeException("No Jurisdiction", e);
    }
  }

  private static void clearSensitiveFields(Object result, String userJurisdiction, List<Field> fieldsToClear) throws IllegalAccessException {
    for (Field field : fieldsToClear) {
      String requiredJurisdiction = field.getAnnotation(GDPRAspect.VisibleFor.class).value();
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
