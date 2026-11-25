package com.taller.security;

import com.taller.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Cuenta los intentos fallidos por usuario y siempre muestra el mismo mensaje,
 * exista o no el correo, para no revelar quien tiene cuenta.
 */
@Component
public class LoginFallidoHandler implements AuthenticationFailureHandler {

    private final UsuarioService usuarioService;

    public LoginFallidoHandler(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (exception instanceof BadCredentialsException) {
            String correo = request.getParameter("username");
            if (correo != null && !correo.isBlank()) {
                usuarioService.registrarFallo(correo.trim());
            }
        }
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
