package victor.training.performance.profile.showcase.util;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
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
public class GDPRFilterAspect {

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

    String userJurisdiction;
    try {
      userJurisdiction = new RestTemplate().getForObject("http://localhost:9999/fast20ms", String.class);
    } catch (RestClientException e) {
      log.debug("WARN: No jurisdiction");
      return result;
    }


    // TODO move these pre-checks BEFORE the expensive network call
    if (!result.getClass().getPackageName().startsWith("victor")) {
      return result;
    }
    List<Field> fieldsToClear = annotatedFields(result);
    if (fieldsToClear.isEmpty()) return result;


    clearFields(result, userJurisdiction, fieldsToClear);
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

  @NotNull
  private static List<Field> annotatedFields(Object result) {
    List<Field> fieldsToClear = new ArrayList<>();
    for (Field field : result.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      VisibleFor annot = field.getAnnotation(VisibleFor.class);
      if (annot != null) {
        fieldsToClear.add(field);
      }
    }
    return fieldsToClear;
  }
}
