package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.StockReceipt;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Long> {
    Optional<StockReceipt> findByReceiptCode(String receiptCode);
    List<StockReceipt> findByBranchCodeOrderByReceiptDateDesc(String branchCode);
}
