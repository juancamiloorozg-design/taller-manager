package com.taller.repository;

import com.taller.model.OrdenFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenFotoRepository extends JpaRepository<OrdenFoto, Long> {

    List<OrdenFoto> findByOrdenIdOrderByIdAsc(Long ordenId);

    Optional<OrdenFoto> findByIdAndOrdenId(Long id, Long ordenId);

    long countByOrdenId(Long ordenId);
}
