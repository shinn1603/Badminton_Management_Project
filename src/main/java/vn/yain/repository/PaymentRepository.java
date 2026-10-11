package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentCode(String paymentCode);
    List<Payment> findByBookingCode(String bookingCode);
    List<Payment> findAllByOrderByPaymentTimeDesc();
    List<Payment> findByPaymentTimeBetween(LocalDateTime start, LocalDateTime end);
}
