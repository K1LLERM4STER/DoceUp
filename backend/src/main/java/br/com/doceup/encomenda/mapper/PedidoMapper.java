package br.com.doceup.encomenda.mapper;

import br.com.doceup.encomenda.dto.PedidoResposta;
import br.com.doceup.encomenda.entity.Pedido;

public final class PedidoMapper {
    private PedidoMapper() {
    }

    public static PedidoResposta paraResposta(Pedido pedido) {
        var itens = pedido.getItens().stream()
                .map(item -> new PedidoResposta.Item(item.getProdutoId(), item.getNome(),
                        item.getQuantidade(), item.getPrecoUnitario()))
                .toList();
        return new PedidoResposta(pedido.getId(), pedido.getCliente(), pedido.getDataEntrega(),
                itens, pedido.getValorTotal(), pedido.getStatus(), pedido.getObservacao(), pedido.getVendaId());
    }
}
