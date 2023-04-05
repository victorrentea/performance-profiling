package victor.training.performance.profiling.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
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

@Slf4j
@Component
@Aspect
public class GDPRAspect {

  @Retention(RetentionPolicy.RUNTIME)
  public @interface VisibleFor {
    String value();
  }

  @Around("@within(org.springframework.web.bind.annotation.RestController))")
  public Object clearNonVisibleFields(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();
    if (result == null) {
      return result;
    }
    if (!result.getClass().getPackageName().startsWith("victor")) {
      return result;
    }

    String userJurisdiction;
    try {
      userJurisdiction = new RestTemplate().getForObject("http://localhost:9999/jurisdiction", String.class);
    } catch (RestClientException e) {
      log.debug("WARN: No jurisdiction");
      return result;
    }

    List<Field> annotatedFields = getAnnotatedFields(result);
    if (annotatedFields.isEmpty()) {
      return result; // TODO move this pre-check BEFORE the expensive network call
    }

    clearFields(result, userJurisdiction, annotatedFields);
    return result;
  }

  private static void clearFields(Object result, String userJurisdiction, List<Field> fieldsToClear) throws IllegalAccessException {
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
