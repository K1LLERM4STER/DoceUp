package br.com.doceup.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PainelResposta(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal gastos,
        BigDecimal recebimentos,
        BigDecimal saldo,
        long pagamentosPendentes
) {
}
