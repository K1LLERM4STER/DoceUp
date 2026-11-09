package br.com.doceup.financeiro.service;

import br.com.doceup.estoque.repository.CompraRepository;
import br.com.doceup.exception.RegraException;
import br.com.doceup.financeiro.dto.PainelResposta;
import br.com.doceup.shared.HorarioDoNegocio;
import br.com.doceup.venda.repository.RecebimentoRepository;
import br.com.doceup.venda.repository.VendaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class PainelService {
    private static final BigDecimal ZERO_REAIS = new BigDecimal("0.00");

    private final CompraRepository compras;
    private final RecebimentoRepository recebimentos;
    private final VendaRepository vendas;
    private final Clock clock;

    public PainelService(CompraRepository compras, RecebimentoRepository recebimentos,
                         VendaRepository vendas, Clock clock) {
        this.compras = compras;
        this.recebimentos = recebimentos;
        this.vendas = vendas;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PainelResposta consultar(UUID negocioId, String mes, String dia) {
        var periodo = periodo(mes, dia);
        var inicio = HorarioDoNegocio.inicioDe(periodo.inicio());
        var fimExclusivo = HorarioDoNegocio.inicioDe(periodo.fim().plusDays(1));
        var gastos = zeroSeAusente(compras.sumByNegocioIdAndPeriodo(negocioId, inicio, fimExclusivo));
        var entradas = zeroSeAusente(recebimentos.somarNoPeriodo(negocioId, inicio, fimExclusivo));
        return new PainelResposta(periodo.inicio(), periodo.fim(), gastos, entradas, entradas.subtract(gastos),
                vendas.contarPendentes(negocioId));
    }

    private Periodo periodo(String mes, String dia) {
        if (mes != null && dia != null) {
            throw periodoInvalido();
        }
        try {
            if (dia != null) {
                if (!dia.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw periodoInvalido();
                }
                LocalDate data = LocalDate.parse(dia);
                return new Periodo(data, data);
            }
            if (mes != null && !mes.matches("\\d{4}-\\d{2}")) {
                throw periodoInvalido();
            }
            var mesRef = mes == null ? YearMonth.from(HorarioDoNegocio.dataDe(clock.instant())) : YearMonth.parse(mes);
            return new Periodo(mesRef.atDay(1), mesRef.atEndOfMonth());
        } catch (DateTimeException erro) {
            throw periodoInvalido();
        }
    }

    private static BigDecimal zeroSeAusente(BigDecimal valor) {
        return valor == null ? ZERO_REAIS : valor;
    }

    private static RegraException periodoInvalido() {
        return new RegraException(HttpStatus.BAD_REQUEST, "PERIODO_INVALIDO",
                "Informe um mês (AAAA-MM) ou um dia (AAAA-MM-DD).");
    }

    private record Periodo(LocalDate inicio, LocalDate fim) {
    }
}
