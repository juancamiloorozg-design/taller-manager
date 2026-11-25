package com.taller.repository;

import com.taller.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByOrdenIdOrderByIdAsc(Long ordenId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.orden.id = :ordenId")
    BigDecimal totalDeOrden(@Param("ordenId") Long ordenId);

    @Query("SELECT p.orden.id, SUM(p.monto) FROM Pago p GROUP BY p.orden.id")
    List<Object[]> totalesPorOrden();

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fecha >= :desde AND p.fecha < :hasta")
    BigDecimal sumaEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
