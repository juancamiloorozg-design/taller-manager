package com.taller.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Tiempo {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private Tiempo() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA);
    }
}
