package victor.training.performance.profiling.util;

import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
@Slf4j
@Component
@Aspect
public class GDPRAspect {
  private final RestTemplate restTemplate;

  @Retention(RetentionPolicy.RUNTIME)
  public @interface VisibleFor {
    String value();
  }

  @Around("@within(org.springframework.web.bind.annotation.RestController))")
  public Object clearNonVisibleFields(ProceedingJoinPoint pjp) throws Throwable {
    Object responseDto = pjp.proceed();
    if (responseDto == null) {
      return null;
    }
    if (!responseDto.getClass().getPackageName().startsWith("victor")) {
      return responseDto; // not my class
    }

    String userRole = fetchUserRole(); // API call

    List<Field> sensitiveFields = getAnnotatedFields(responseDto, VisibleFor.class);
    if (sensitiveFields.isEmpty()) {
      return responseDto; // no fields annotated
    }

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
      String requiredJurisdiction = field.getAnnotation(VisibleFor.class).value();
      if (!requiredJurisdiction.equals(userJurisdiction)) {
        field.set(result, null);
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<Field> getAnnotatedFields(Object result, Class annotation) {
    List<Field> annotatedFields = new ArrayList<>();
    for (Field field : result.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      if (field.getAnnotation(annotation) != null) {
        annotatedFields.add(field);
      }
    }
    return annotatedFields;
  }
}
