package hu.movieclub.infrastructure;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AccountRepository extends JpaRepository<AccountEntity,Long> { Optional<AccountEntity> findByUsername(String name); }

