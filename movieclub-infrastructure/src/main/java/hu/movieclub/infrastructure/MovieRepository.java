package hu.movieclub.infrastructure;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MovieRepository extends JpaRepository<MovieEntity,Long> { Optional<MovieEntity> findByExternalId(Long id); }

