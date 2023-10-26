package victor.training.performance.profiling;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import victor.training.performance.profiling.dto.CommentDto;

import java.util.List;

@FeignClient(value = "loan-comments", url = "http://localhost:9999/")
public interface CommentsApiClient {

  @Timed
  @RequestMapping(method = RequestMethod.GET, value = "loan-comments/{id}")
  List<CommentDto> fetchComments(@PathVariable Long id);
}
