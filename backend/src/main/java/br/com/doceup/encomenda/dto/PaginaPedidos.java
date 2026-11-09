package br.com.doceup.encomenda.dto;

import java.util.List;

public record PaginaPedidos(List<PedidoResposta> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {
}
