package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.WorkShift;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Long> {
    Optional<WorkShift> findByShiftCode(String shiftCode);
    List<WorkShift> findByBranchCode(String branchCode);
    List<WorkShift> findByStaffCode(String staffCode);
    void deleteByStaffCode(String staffCode);
    void deleteByDayOfWeekAndShiftTypeAndStaffCode(String dayOfWeek, String shiftType, String staffCode);
    void deleteByBranchCode(String branchCode);
}
