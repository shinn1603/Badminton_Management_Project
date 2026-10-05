package vn.yain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.yain.entity.Tournament;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    Optional<Tournament> findByTournamentCode(String tournamentCode);
    List<Tournament> findByStatus(String status);
}
