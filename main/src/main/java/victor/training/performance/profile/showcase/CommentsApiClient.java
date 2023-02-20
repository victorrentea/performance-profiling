package victor.training.performance.profile.showcase;

import io.micrometer.core.annotation.Timed;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Timed
@FeignClient(value = "loan-comments", url = "http://localhost:9999/")
public interface CommentsApiClient {

  @RequestMapping(method = RequestMethod.GET, value = "loan-comments/{id}")
  List<CommentDto> getCommentsForLoanApplication(@PathVariable Long id);
}
