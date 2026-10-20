package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Supplier;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    Optional<Supplier> findBySupplierName(String supplierName);
    List<Supplier> findAllByOrderBySupplierCodeAsc();
    List<Supplier> findByStatus(String status);
}
