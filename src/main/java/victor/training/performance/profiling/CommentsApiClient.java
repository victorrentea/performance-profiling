package victor.training.performance.profiling;

import io.micrometer.core.annotation.Timed;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import victor.training.performance.profiling.dto.CommentDto;

import java.util.List;

@FeignClient("loan-comments")
public interface CommentsApiClient {

  @Timed
  @GetMapping("loan-comments/{id}")
  List<CommentDto> fetchComments(@PathVariable Long id);
}
