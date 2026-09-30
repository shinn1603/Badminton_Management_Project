package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Branch;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
}
