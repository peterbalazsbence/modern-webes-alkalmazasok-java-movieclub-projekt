package hu.movieclub.infrastructure;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RatingRepository extends JpaRepository<RatingEntity,Long> {
    Optional<RatingEntity> findByUserIdAndMovieId(Long userId,Long movieId);
}

