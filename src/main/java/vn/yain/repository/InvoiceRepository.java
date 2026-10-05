package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Invoice;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceCode(String invoiceCode);
    Optional<Invoice> findByBookingCode(String bookingCode);
    List<Invoice> findAllByOrderByInvoiceDateDesc();
}
