package com.taller.repository;

import com.taller.model.Vehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    @Query(value = "SELECT v FROM Vehiculo v JOIN FETCH v.cliente c "
            + "WHERE LOWER(v.placa) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR :q = ''",
            countQuery = "SELECT COUNT(v) FROM Vehiculo v JOIN v.cliente c "
                    + "WHERE LOWER(v.placa) LIKE LOWER(CONCAT('%', :q, '%')) "
                    + "OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :q, '%')) "
                    + "OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :q, '%')) "
                    + "OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) "
                    + "OR :q = ''")
    Page<Vehiculo> buscar(@Param("q") String q, Pageable pageable);

    @Query("SELECT v FROM Vehiculo v JOIN FETCH v.cliente WHERE v.id = :id")
    Optional<Vehiculo> findByIdConCliente(@Param("id") Long id);

    @Query("SELECT v FROM Vehiculo v JOIN FETCH v.cliente ORDER BY v.placa")
    List<Vehiculo> findAllConCliente();

    List<Vehiculo> findByClienteIdOrderByPlacaAsc(Long clienteId);

    boolean existsByPlaca(String placa);

    boolean existsByPlacaAndIdNot(String placa, Long id);
}
