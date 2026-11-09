package br.com.doceup.venda.dto;

import java.util.List;

public record PaginaVendas(List<VendaResposta> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {
}
