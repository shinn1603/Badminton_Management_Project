package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Booking;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBranchCodeOrderByBookingDateDesc(String branchCode);
    List<Booking> findAllByOrderByCreatedAtDesc();
}
