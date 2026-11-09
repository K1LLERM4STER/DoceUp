package br.com.doceup.venda.mapper;

import br.com.doceup.shared.HorarioDoNegocio;
import br.com.doceup.venda.dto.VendaResposta;
import br.com.doceup.venda.entity.ItemVenda;
import br.com.doceup.venda.entity.Recebimento;
import br.com.doceup.venda.entity.Venda;

public final class VendaMapper {
    private VendaMapper() {
    }

    public static VendaResposta paraResposta(Venda venda, Recebimento recebimento) {
        return new VendaResposta(
                venda.getId(),
                HorarioDoNegocio.dataDe(venda.getVendidaEm()),
                venda.getItens().stream().map(VendaMapper::paraItem).toList(),
                venda.getValorTotal(),
                recebimento == null,
                recebimento == null ? null : HorarioDoNegocio.dataDe(recebimento.getRecebidoEm()),
                venda.getObservacao()
        );
    }

    private static VendaResposta.Item paraItem(ItemVenda item) {
        return new VendaResposta.Item(
                item.getProdutoId(), item.getNome(), item.getQuantidade(), item.getPrecoUnitario()
        );
    }
}
