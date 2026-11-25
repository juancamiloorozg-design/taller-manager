package com.taller.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Disponible en las plantillas como ${@fmt.cop(valor)}. */
@Component("fmt")
public class Formato {

    public String cop(BigDecimal valor) {
        if (valor == null) {
            return "$ 0";
        }
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat("#,##0", simbolos);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return "$ " + formato.format(valor);
    }
}
