package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Booking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByBranchCodeOrderByBookingDateDesc(String branchCode);
    List<Booking> findAllByOrderByCreatedAtDesc();
    List<Booking> findByBranchCodeAndBookingDate(String branchCode, LocalDate bookingDate);
    List<Booking> findByCourtCodeAndBookingDate(String courtCode, LocalDate bookingDate);
}
