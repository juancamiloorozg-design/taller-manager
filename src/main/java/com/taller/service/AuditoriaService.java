package com.taller.service;

import com.taller.model.Auditoria;
import com.taller.repository.AuditoriaRepository;
import com.taller.util.Tiempo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private static final int TAMANO_PAGINA = 30;
    private static final int MAX_DETALLE = 500;

    private final AuditoriaRepository repositorio;

    public AuditoriaService(AuditoriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Si se llama dentro de una transaccion, el registro se guarda o se revierte junto con ella. */
    @Transactional
    public void registrar(String usuario, String accion, String entidad, Long entidadId, String detalle) {
        String texto = detalle;
        if (texto != null && texto.length() > MAX_DETALLE) {
            texto = texto.substring(0, MAX_DETALLE);
        }
        repositorio.save(new Auditoria(Tiempo.ahora(), usuario, accion, entidad, entidadId, texto));
    }

    @Transactional(readOnly = true)
    public Page<Auditoria> listar(int pagina) {
        int p = Math.max(pagina, 0);
        return repositorio.findAll(PageRequest.of(p, TAMANO_PAGINA, Sort.by(Sort.Direction.DESC, "id")));
    }
}
