package com.taller.repository;

import com.taller.model.Cita;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("""
            SELECT c FROM Cita c JOIN FETCH c.vehiculo v JOIN FETCH v.cliente LEFT JOIN FETCH c.orden
            WHERE c.fechaHora >= :inicio AND c.fechaHora < :fin
            ORDER BY c.fechaHora, c.id
            """)
    List<Cita> enSemana(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT c FROM Cita c JOIN FETCH c.vehiculo v JOIN FETCH v.cliente LEFT JOIN FETCH c.orden WHERE c.id = :id")
    Optional<Cita> conDetalle(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.id = :id")
    Optional<Cita> paraActualizar(@Param("id") Long id);

    boolean existsByVehiculoId(Long vehiculoId);
}
