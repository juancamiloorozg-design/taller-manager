package com.taller.repository;

import com.taller.model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {

    List<Repuesto> findAllByOrderByNombreAsc();

    List<Repuesto> findByActivoTrueOrderByNombreAsc();

    @Query("SELECT r FROM Repuesto r WHERE r.activo = true AND r.stock <= r.stockMinimo ORDER BY r.nombre")
    List<Repuesto> findStockBajo();

    /**
     * Descuenta stock en una sola instruccion SQL. Si no alcanza, no toca la fila y devuelve 0.
     * Al ser atomica en la base de datos, dos ordenes simultaneas no pueden dejar el stock en negativo.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Repuesto r SET r.stock = r.stock - :cantidad WHERE r.id = :id AND r.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Repuesto r SET r.stock = r.stock + :cantidad WHERE r.id = :id")
    int reponerStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
