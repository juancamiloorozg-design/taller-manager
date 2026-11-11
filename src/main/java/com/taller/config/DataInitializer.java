package com.taller.config;

import com.taller.model.Rol;
import com.taller.repository.UsuarioRepository;
import com.taller.service.UsuarioService;
import com.taller.util.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Crea el administrador inicial la primera vez que arranca la aplicacion.
 * Si no se define ADMIN_PASSWORD, genera una clave aleatoria y la imprime una sola vez en el log.
 */
@Component
@Order(1)
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UsuarioRepository usuarios;
    private final UsuarioService usuarioService;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UsuarioRepository usuarios,
                           UsuarioService usuarioService,
                           @Value("${taller.admin.email}") String adminEmail,
                           @Value("${taller.admin.password:}") String adminPassword) {
        this.usuarios = usuarios;
        this.usuarioService = usuarioService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        boolean generada = adminPassword == null || adminPassword.isBlank();
        String clave = generada ? generarClave() : adminPassword;

        usuarioService.crearInterno(adminEmail, "Administrador", clave, Rol.ADMIN);

        if (generada) {
            log.warn("==============================================================");
            log.warn("Se creó el administrador inicial.");
            log.warn("Usuario: {}", adminEmail);
            log.warn("Contraseña: {}", clave);
            log.warn("Esta clave solo se muestra ahora. Cámbiala al ingresar.");
            log.warn("==============================================================");
        } else {
            log.info("Se creó el administrador inicial {} con la clave definida en ADMIN_PASSWORD.", adminEmail);
        }
    }

    private String generarClave() {
        SecureRandom azar = new SecureRandom();
        String clave;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
            }
            clave = sb.toString();
        } while (PasswordPolicy.validar(clave) != null);
        return clave;
    }
}
