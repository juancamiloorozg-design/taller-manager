package com.taller.service;

import com.taller.exception.RecursoNoEncontradoException;
import com.taller.exception.TallerException;
import com.taller.model.Rol;
import com.taller.model.Usuario;
import com.taller.repository.UsuarioRepository;
import com.taller.util.PasswordPolicy;
import com.taller.util.Tiempo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    public static final int MAX_INTENTOS = 5;
    public static final int MINUTOS_BLOQUEO = 15;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarActivos() {
        return usuarios.findAllByOrderByNombreAsc().stream().filter(Usuario::isActivo).toList();
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Usuario crear(String email, String nombre, String password, Rol rol, String actor) {
        Usuario usuario = crearInterno(email, nombre, password, rol);
        auditoria.registrar(actor, "CREAR", "Usuario", usuario.getId(),
                "Usuario " + usuario.getEmail() + " con rol " + rol.name());
        return usuario;
    }

    /** Usado tambien al arrancar para crear el administrador inicial. */
    @Transactional
    public Usuario crearInterno(String email, String nombre, String password, Rol rol) {
        String correo = email == null ? "" : email.trim().toLowerCase();
        if (correo.isEmpty()) {
            throw new TallerException("El correo es obligatorio.");
        }
        if (usuarios.existsByEmailIgnoreCase(correo)) {
            throw new TallerException("Ya existe un usuario con el correo " + correo + ".");
        }
        String error = PasswordPolicy.validar(password);
        if (error != null) {
            throw new TallerException(error);
        }
        return usuarios.save(new Usuario(correo, nombre.trim(), encoder.encode(password), rol));
    }

    @Transactional
    public void cambiarEstado(Long id, String actor) {
        Usuario usuario = buscar(id);
        if (usuario.getEmail().equalsIgnoreCase(actor)) {
            throw new TallerException("No puedes desactivar tu propia cuenta.");
        }
        usuario.setActivo(!usuario.isActivo());
        auditoria.registrar(actor, usuario.isActivo() ? "ACTIVAR" : "DESACTIVAR", "Usuario", id,
                "Usuario " + usuario.getEmail());
    }

    @Transactional
    public void restablecerPassword(Long id, String nueva, String actor) {
        Usuario usuario = buscar(id);
        String error = PasswordPolicy.validar(nueva);
        if (error != null) {
            throw new TallerException(error);
        }
        usuario.setPasswordHash(encoder.encode(nueva));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        auditoria.registrar(actor, "RESTABLECER_CLAVE", "Usuario", id, "Usuario " + usuario.getEmail());
    }

    @Transactional
    public void cambiarPassword(String email, String actual, String nueva) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario."));
        if (!encoder.matches(actual, usuario.getPasswordHash())) {
            throw new TallerException("La contraseña actual no es correcta.");
        }
        String error = PasswordPolicy.validar(nueva);
        if (error != null) {
            throw new TallerException(error);
        }
        usuario.setPasswordHash(encoder.encode(nueva));
        auditoria.registrar(usuario.getEmail(), "CAMBIAR_CLAVE", "Usuario", usuario.getId(), null);
    }

    @Transactional
    public void registrarFallo(String email) {
        usuarios.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            int intentos = usuario.getIntentosFallidos() + 1;
            if (intentos >= MAX_INTENTOS) {
                usuario.setBloqueadoHasta(Tiempo.ahora().plusMinutes(MINUTOS_BLOQUEO));
                usuario.setIntentosFallidos(0);
                auditoria.registrar(usuario.getEmail(), "BLOQUEO", "Usuario", usuario.getId(),
                        "Bloqueada por " + MAX_INTENTOS + " intentos fallidos durante " + MINUTOS_BLOQUEO + " minutos");
            } else {
                usuario.setIntentosFallidos(intentos);
            }
        });
    }

    @Transactional
    public void registrarAcceso(String email) {
        usuarios.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuario.setUltimoAcceso(Tiempo.ahora());
        });
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con id " + id + "."));
    }
}
