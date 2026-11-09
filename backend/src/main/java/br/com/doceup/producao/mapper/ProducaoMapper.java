package br.com.doceup.producao.mapper;

import br.com.doceup.producao.dto.ProducaoDtos;
import br.com.doceup.producao.entity.Producao;

public final class ProducaoMapper {
    private ProducaoMapper() {}

    public static ProducaoDtos.Base base(Producao producao) {
        var ingredientes = producao.getIngredientes().stream()
                .map(item -> new ProducaoDtos.Ingrediente(item.getInsumoId(), item.getQuantidade()))
                .toList();
        return new ProducaoDtos.Base(producao.getRendimento(), ingredientes, producao.getQuantidadeEmbalagem(),
                producao.getCustoEmbalagem());
    }
}
