package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.PricingRule;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, String> {
    List<PricingRule> findByBranchCode(String branchCode);
    Optional<PricingRule> findByPricingCode(String pricingCode);
    boolean existsByPricingCode(String pricingCode);
}
