package campus_lost_found.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import campus_lost_found.model.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
}
