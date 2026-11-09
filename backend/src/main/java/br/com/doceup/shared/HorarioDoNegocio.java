package br.com.doceup.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class HorarioDoNegocio {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private HorarioDoNegocio() {
    }

    public static LocalDate dataDe(Instant instante) {
        return instante.atZone(FUSO).toLocalDate();
    }

    public static Instant inicioDe(LocalDate data) {
        return data.atStartOfDay(FUSO).toInstant();
    }
}
