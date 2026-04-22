package com.compicar.checkin;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    @Query("SELECT c FROM Checkin c WHERE c.parada.id = :paradaId")
    List<Checkin> findByParadaId(@Param("paradaId") Long paradaId);
}
