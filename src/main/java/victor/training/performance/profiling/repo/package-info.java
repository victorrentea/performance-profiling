@NonNullApi // blocks any repo interface in this package from ever returning null.
// Instead of returning null, and Repo method would throw an exception. automat
// => you'll have to use Optional<>
package victor.training.performance.profiling.repo;

import org.springframework.lang.NonNullApi;