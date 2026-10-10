package campus_lost_found.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import campus_lost_found.model.Claim;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
}