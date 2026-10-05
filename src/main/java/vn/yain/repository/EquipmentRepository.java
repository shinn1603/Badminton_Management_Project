package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Equipment;

import java.util.List;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, String> {
    List<Equipment> findByBranchCode(String branchCode);
}
