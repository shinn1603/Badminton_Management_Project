package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.PricingProposal;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingProposalRepository extends JpaRepository<PricingProposal, Long> {
    Optional<PricingProposal> findByProposalCode(String proposalCode);
    List<PricingProposal> findByBranchCode(String branchCode);
    List<PricingProposal> findByStatus(String status);
}
