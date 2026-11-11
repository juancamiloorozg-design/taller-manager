package com.taller;

import com.taller.util.Tiempo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TallerManagerApplication {

    public static void main(String[] args) {
        // Misma zona horaria para la JVM, la conexion JDBC y las fechas guardadas.
        TimeZone.setDefault(TimeZone.getTimeZone(Tiempo.ZONA));
        SpringApplication.run(TallerManagerApplication.class, args);
    }
}
