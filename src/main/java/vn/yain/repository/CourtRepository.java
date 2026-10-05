package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Court;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {
    List<Court> findByBranchCode(String branchCode);
    Optional<Court> findByCourtCode(String courtCode);
}
