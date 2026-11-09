package br.com.doceup.estoque.mapper;

import br.com.doceup.estoque.dto.InsumoDtos;
import br.com.doceup.estoque.entity.Insumo;

public final class InsumoMapper {
    private InsumoMapper() {}

    public static InsumoDtos.Insumo resposta(Insumo insumo) {
        return new InsumoDtos.Insumo(insumo.getId(), insumo.getNome(), insumo.getUnidade(),
                insumo.getTipo(), insumo.getQuantidade(), insumo.getUltimoCustoUnitario());
    }
}
