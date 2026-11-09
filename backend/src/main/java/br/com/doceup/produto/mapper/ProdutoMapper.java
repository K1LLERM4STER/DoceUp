package br.com.doceup.produto.mapper;

import br.com.doceup.produto.dto.ProdutoDtos;
import br.com.doceup.produto.entity.Produto;

public final class ProdutoMapper {
    private ProdutoMapper() {}

    public static ProdutoDtos.Produto resposta(Produto produto) {
        String foto = produto.isTemFoto() ? "/api/v1/produtos/" + produto.getId() + "/foto" : null;
        return new ProdutoDtos.Produto(produto.getId(), produto.getNome(), produto.getPrecoVenda(),
                produto.getQuantidadeTotal(), produto.getQuantidadeReservada(), produto.getQuantidadeDisponivel(),
                produto.getCustoUnitario(), foto, produto.isAtivo());
    }
}
