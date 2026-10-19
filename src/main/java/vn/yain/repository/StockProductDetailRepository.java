package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.StockProductDetail;
import java.util.List;

@Repository
public interface StockProductDetailRepository extends JpaRepository<StockProductDetail, Long> {
    List<StockProductDetail> findByReceiptCode(String receiptCode);
    List<StockProductDetail> findByProductCode(String productCode);
}
