package com.taller.repository;

import com.taller.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Busca por nombre, documento o telefono. Con q vacio devuelve todos. */
    @Query("SELECT c FROM Cliente c WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR c.numeroDocumento LIKE CONCAT('%', :q, '%') "
            + "OR c.telefono LIKE CONCAT('%', :q, '%') "
            + "OR :q = ''")
    Page<Cliente> buscar(@Param("q") String q, Pageable pageable);

    List<Cliente> findByAnonimizadoFalseOrderByNombreAsc();

    boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(String tipoDocumento, String numeroDocumento, Long id);
}
