package br.com.doceup.produto.service;

import br.com.doceup.estoque.dto.PaginaEstoque;
import br.com.doceup.estoque.entity.TipoMovimento;
import br.com.doceup.estoque.service.MovimentoService;
import br.com.doceup.exception.RegraException;
import br.com.doceup.produto.dto.ProdutoDtos;
import br.com.doceup.produto.entity.FotoProduto;
import br.com.doceup.produto.entity.Produto;
import br.com.doceup.produto.mapper.ProdutoMapper;
import br.com.doceup.produto.repository.FotoRepository;
import br.com.doceup.produto.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProdutoService {
    private final ProdutoRepository produtos;
    private final FotoRepository fotos;
    private final MovimentoService movimentos;

    public ProdutoService(ProdutoRepository produtos, FotoRepository fotos, MovimentoService movimentos) {
        this.produtos = produtos;
        this.fotos = fotos;
        this.movimentos = movimentos;
    }

    @Transactional(readOnly = true)
    public PaginaEstoque<ProdutoDtos.Produto> listar(UUID negocioId, String busca, Pageable pagina) {
        var ordenada = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(), Sort.by("nome", "id"));
        return PaginaEstoque.de(produtos.findByNegocioIdAndAtivoTrueAndNomeContainingIgnoreCase(
                negocioId, busca.strip(), ordenada).map(ProdutoMapper::resposta));
    }

    @Transactional(readOnly = true)
    public ProdutoDtos.Produto obter(UUID negocioId, UUID id) {
        return ProdutoMapper.resposta(produtos.findByIdAndNegocioId(id, negocioId).orElseThrow(ProdutoService::naoEncontrado));
    }

    @Transactional
    public ProdutoDtos.Produto criar(UUID negocioId, ProdutoDtos.Formulario entrada) {
        String nome = entrada.nome().strip();
        if (produtos.existsByNegocioIdAndNomeIgnoreCase(negocioId, nome)) {
            throw duplicado();
        }
        return ProdutoMapper.resposta(produtos.save(new Produto(negocioId, nome, entrada.precoVenda())));
    }

    @Transactional
    public ProdutoDtos.Produto editar(UUID negocioId, UUID id, ProdutoDtos.Formulario entrada) {
        Produto produto = bloquear(negocioId, id);
        produto.exigirAtivo();
        String nome = entrada.nome().strip();
        if (produtos.existsByNegocioIdAndNomeIgnoreCaseAndIdNot(negocioId, nome, id)) {
            throw duplicado();
        }
        produto.editar(nome, entrada.precoVenda());
        return ProdutoMapper.resposta(produto);
    }

    @Transactional
    public ProdutoDtos.Produto ajustar(UUID negocioId, UUID id, ProdutoDtos.Ajuste entrada) {
        Produto produto = bloquear(negocioId, id);
        produto.exigirAtivo();
        int diferenca = entrada.quantidade() - produto.getQuantidadeTotal();
        produto.ajustar(entrada.quantidade());
        if (diferenca != 0) {
            movimentos.registrarProduto(negocioId, id, TipoMovimento.AJUSTE, BigDecimal.valueOf(diferenca),
                    UUID.randomUUID(), entrada.motivo());
        }
        return ProdutoMapper.resposta(produto);
    }

    @Transactional
    public ProdutoDtos.Produto desativar(UUID negocioId, UUID id) {
        Produto produto = bloquear(negocioId, id);
        produto.desativar();
        return ProdutoMapper.resposta(produto);
    }

    @Transactional
    public ProdutoDtos.Produto salvarFoto(UUID negocioId, UUID id, MultipartFile arquivo) {
        Produto produto = bloquear(negocioId, id);
        produto.exigirAtivo();
        ProdutoDtos.Imagem imagem = ValidadorFoto.ler(arquivo);
        fotos.findByProdutoIdAndNegocioId(id, negocioId).ifPresentOrElse(
                foto -> foto.substituir(imagem.formato(), imagem.conteudo()),
                () -> fotos.save(new FotoProduto(id, negocioId, imagem.formato(), imagem.conteudo())));
        produto.registrarFoto();
        return ProdutoMapper.resposta(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoDtos.Imagem obterFoto(UUID negocioId, UUID id) {
        var foto = fotos.findByProdutoIdAndNegocioId(id, negocioId).orElseThrow(ProdutoService::naoEncontrado);
        return new ProdutoDtos.Imagem(foto.getFormato(), foto.getConteudo());
    }

    private Produto bloquear(UUID negocioId, UUID id) {
        return produtos.findForUpdateByIdAndNegocioId(id, negocioId).orElseThrow(ProdutoService::naoEncontrado);
    }

    public static RegraException naoEncontrado() {
        return new RegraException(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO", "Produto não encontrado.");
    }

    private static RegraException duplicado() {
        return new RegraException(HttpStatus.CONFLICT, "PRODUTO_DUPLICADO", "Já existe um produto com esse nome.");
    }
}
