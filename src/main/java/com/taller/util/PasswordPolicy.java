package com.taller.util;

public final class PasswordPolicy {

    public static final int MINIMO = 10;
    public static final int MAXIMO = 72; // limite practico de BCrypt

    private PasswordPolicy() {
    }

    /** Devuelve el mensaje de error, o null si la contrasena cumple la politica. */
    public static String validar(String password) {
        if (password == null || password.length() < MINIMO) {
            return "La contraseña debe tener al menos " + MINIMO + " caracteres.";
        }
        if (password.length() > MAXIMO) {
            return "La contraseña no puede superar los " + MAXIMO + " caracteres.";
        }
        boolean hayLetra = false;
        boolean hayDigito = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hayLetra = true;
            } else if (Character.isDigit(c)) {
                hayDigito = true;
            }
        }
        if (!hayLetra || !hayDigito) {
            return "La contraseña debe incluir letras y números.";
        }
        return null;
    }
}
