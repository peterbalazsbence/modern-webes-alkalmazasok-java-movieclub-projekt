package hu.movieclub.infrastructure;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WatchRepository extends JpaRepository<WatchEntity,Long> {
    Optional<WatchEntity> findByUserIdAndMovieId(Long userId,Long movieId);
    List<WatchEntity> findByUserId(Long userId);
}

