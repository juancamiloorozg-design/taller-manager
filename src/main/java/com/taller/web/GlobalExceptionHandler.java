package com.taller.web;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TallerException.class)
    public String negocio(TallerException e, Model model, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("mensaje", e.getMessage());
        return "error/negocio";
    }

    @ExceptionHandler({RecursoNoEncontradoException.class, NoResourceFoundException.class,
            MethodArgumentTypeMismatchException.class})
    public String noEncontrado(Exception e, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "error/404";
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public String faltaDato(Model model, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("mensaje", "Faltan datos en la solicitud. Revisa el formulario e intenta de nuevo.");
        return "error/negocio";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoGrande(Model model, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        model.addAttribute("mensaje", "La foto es demasiado grande. El máximo es 5 MB.");
        return "error/negocio";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integridad(DataIntegrityViolationException e, Model model, HttpServletResponse respuesta) {
        LOG.warn("Violacion de integridad: {}", e.getMostSpecificCause().getMessage());
        respuesta.setStatus(HttpServletResponse.SC_CONFLICT);
        model.addAttribute("mensaje", "No se pudo guardar: ya existe un registro con esos datos o el registro está en uso.");
        return "error/negocio";
    }

    /** Sin esto, el handler generico de abajo se tragaria el 403 de @PreAuthorize. */
    @ExceptionHandler(AccessDeniedException.class)
    public void accesoDenegado(AccessDeniedException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public String inesperado(Exception e, HttpServletResponse respuesta) {
        LOG.error("Error no controlado", e);
        respuesta.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return "error/500";
    }
}
