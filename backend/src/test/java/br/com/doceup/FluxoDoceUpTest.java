package br.com.doceup;

import br.com.doceup.usuario.service.ContaAtual;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoDoceUpTest {
    private static final String API = "/api/v1";
    private static final Instant NOVEMBRO = Instant.parse("2026-11-30T23:59:00Z");
    private static final Instant DEZEMBRO = Instant.parse("2026-12-01T03:01:00Z");
    private static final AtomicReference<Instant> AGORA = new AtomicReference<>(NOVEMBRO);

    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired ContaAtual contas;

    private OidcUser dona;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) {
        String bancoLocal = System.getenv("DOCEUP_TEST_DATABASE_URL");
        if (bancoLocal != null && !bancoLocal.isBlank()) {
            propriedades.add("spring.datasource.url", () -> bancoLocal);
            propriedades.add("spring.datasource.username", () -> System.getenv("DOCEUP_TEST_DATABASE_USER"));
            propriedades.add("spring.datasource.password", () -> System.getenv("DOCEUP_TEST_DATABASE_PASSWORD"));
        } else {
            var postgres = new PostgreSQLContainer("postgres:18");
            postgres.start();
            propriedades.add("spring.datasource.url", postgres::getJdbcUrl);
            propriedades.add("spring.datasource.username", postgres::getUsername);
            propriedades.add("spring.datasource.password", postgres::getPassword);
        }
        propriedades.add("spring.security.oauth2.client.registration.google.client-id", () -> "doceup-test");
        propriedades.add("spring.security.oauth2.client.registration.google.client-secret", () -> "only-test");
    }

    @BeforeEach
    void prepararConta() {
        AGORA.set(NOVEMBRO);
        dona = novaConta();
    }

    @Test
    void compraProducaoVendaPendenteERecebimentoNoMesSeguinte() throws Exception {
        String ingrediente = ingrediente();
        escrever("/ingredientes/" + ingrediente + "/compras", "{\"quantidade\":1,\"valorTotal\":30}")
                .andExpect(status().isCreated());
        String produto = produto();
        String producao = producao(ingrediente, "0.3", 20, false);
        escrever("/producao/" + produto, producao).andExpect(status().isCreated())
                .andExpect(jsonPath("$.custoUnitario").value(0.45));

        escrever("/ingredientes/" + ingrediente + "/compras", "{\"quantidade\":1,\"valorTotal\":40}")
                .andExpect(status().isCreated());
        mvc.perform(get(API + "/produtos/" + produto).with(oidcLogin().oidcUser(dona)))
                .andExpect(jsonPath("$.custoUnitario").value(0.45));

        UUID chave = UUID.randomUUID();
        String venda = venda(produto, 5, "12.50", true);
        String id = id(escrever(dona, "/vendas", venda, chave).andExpect(status().isCreated()));
        assertThat(id(escrever(dona, "/vendas", venda, chave).andExpect(status().isCreated()))).isEqualTo(id);
        mvc.perform(get(API + "/produtos/" + produto).with(oidcLogin().oidcUser(dona)))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(15));
        painel("2026-11", 70, 0, -70);

        AGORA.set(DEZEMBRO);
        escrever("/vendas/" + id + "/recebimento", "{}").andExpect(status().isOk());
        escrever("/vendas/" + id + "/recebimento", "{}").andExpect(status().isOk());
        painel("2026-11", 70, 0, -70);
        painel("2026-12", 0, 12.50, 12.50);
        mvc.perform(get(API + "/painel").param("dia", "2026-12-01").with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recebimentos").value(12.5));
    }

    @Test
    void avisaFaltaEPreservaEstoqueAteConfirmacaoExplicita() throws Exception {
        String ingrediente = ingrediente();
        escrever("/ingredientes/" + ingrediente + "/compras", "{\"quantidade\":0.2,\"valorTotal\":0}")
                .andExpect(status().isCreated());
        String produto = produto();
        String rota = "/producao/" + produto;
        escrever(rota, producao(ingrediente, "0.3", 20, false)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ESTOQUE_INSUFICIENTE"));
        mvc.perform(get(API + "/produtos/" + produto).with(oidcLogin().oidcUser(dona)))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(0));
        escrever(rota, producao(ingrediente, "0.3", 20, true)).andExpect(status().isCreated());
        mvc.perform(get(API + "/ingredientes/" + ingrediente).with(oidcLogin().oidcUser(dona)))
                .andExpect(jsonPath("$.quantidade").value(-0.1));
        escrever("/ingredientes/" + ingrediente + "/ajustes", "{\"quantidade\":1}")
                .andExpect(status().isOk());
        painel("2026-11", 0, 0, 0);
    }

    @Test
    void exigeSessaoCsrfEIsolaVinculosEntreContas() throws Exception {
        mvc.perform(get(API + "/produtos")).andExpect(status().isUnauthorized());
        mvc.perform(post(API + "/produtos").with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isForbidden());

        MvcResult csrfResposta = mvc.perform(get(API + "/auth/csrf").with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = json.readTree(csrfResposta.getResponse().getContentAsByteArray());
        mvc.perform(post(API + "/produtos").with(oidcLogin().oidcUser(dona))
                        .session((MockHttpSession) csrfResposta.getRequest().getSession(false))
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Brownie\",\"precoVenda\":5}"))
                .andExpect(status().isCreated());
        escrever("/produtos", "{\"nome\":\"Brownie\",\"precoVenda\":5,\"negocioId\":\"outro\"}")
                .andExpect(status().isBadRequest());
        escrever("/produtos", "{\"nome\":\"" + "a".repeat(65_536) + "\",\"precoVenda\":5}")
                .andExpect(status().is(413));
        escrever("/vendas", "{\"itens\":[{\"produtoId\":\"" + UUID.randomUUID()
                + "\",\"quantidade\":1.5}]}").andExpect(status().isBadRequest());

        String produto = produto();
        String ingredienteAlheio = id(escrever(novaConta(), "/ingredientes",
                "{\"nome\":\"Farinha\",\"unidade\":\"KG\"}", UUID.randomUUID()).andExpect(status().isCreated()));
        escrever("/producao/" + produto, producao(ingredienteAlheio, "1", 2, true))
                .andExpect(status().isNotFound());
        mvc.perform(get(API + "/produtos/" + produto).with(oidcLogin().oidcUser(novaConta())))
                .andExpect(status().isNotFound());
    }

    @Test
    void concorrenciaNaoVendeOsMesmosDoces() throws Exception {
        String produto = produto();
        escrever("/produtos/" + produto + "/ajustes", "{\"quantidade\":1}").andExpect(status().isOk());
        String entrada = venda(produto, 1, "3.00", false);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> escrever("/vendas", entrada).andReturn().getResponse().getStatus());
            var b = executor.submit(() -> escrever("/vendas", entrada).andReturn().getResponse().getStatus());
            assertThat(new int[]{a.get(), b.get()}).containsExactlyInAnyOrder(201, 409);
        }
        painel("2026-11", 0, 3, 3);
    }

    @Test
    void primeiraEntradaSimultaneaUsaOMesmoNegocio() throws Exception {
        var usuaria = identidadeFicticia();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primeira = executor.submit(() -> contas.cadastrarSeNecessario(usuaria));
            var segunda = executor.submit(() -> contas.cadastrarSeNecessario(usuaria));
            assertThat(primeira.get()).isEqualTo(segunda.get());
        }
        mvc.perform(get(API + "/ingredientes").with(oidcLogin().oidcUser(usuaria)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void encomendaReservaVariosProdutosEConcluiComUmUnicoRecebimento() throws Exception {
        String brigadeiro = produto();
        String brownie = id(escrever("/produtos", "{\"nome\":\"Brownie\",\"precoVenda\":5}")
                .andExpect(status().isCreated()));
        escrever("/produtos/" + brigadeiro + "/ajustes", "{\"quantidade\":10}").andExpect(status().isOk());
        escrever("/produtos/" + brownie + "/ajustes", "{\"quantidade\":5}").andExpect(status().isOk());
        String entrada = pedido(brigadeiro, 4, brownie, 2);
        UUID chaveReserva = UUID.randomUUID();
        String encomenda = id(escrever(dona, "/pedidos", entrada, chaveReserva).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA")).andExpect(jsonPath("$.valorTotal").value(22)));
        assertThat(id(escrever(dona, "/pedidos", entrada, chaveReserva).andExpect(status().isCreated())))
                .isEqualTo(encomenda);
        estoque(brigadeiro, 10, 4, 6);
        estoque(brownie, 5, 2, 3);
        mvc.perform(get(API + "/pedidos").param("status", "ABERTA").param("dataEntrega", "2026-12-01")
                        .with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put(API + "/produtos/" + brigadeiro).with(oidcLogin().oidcUser(dona)).with(csrf().asHeader())
                        .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Brigadeiro\",\"precoVenda\":4}"))
                .andExpect(status().isOk());
        escrever("/vendas", venda(brigadeiro, 7, "21.00", false)).andExpect(status().isConflict());
        painel("2026-11", 0, 0, 0);

        AGORA.set(DEZEMBRO);
        var outraConta = novaConta();
        escrever(outraConta, "/pedidos/" + encomenda + "/conclusao", "{}", UUID.randomUUID())
                .andExpect(status().isNotFound());
        String concluir = "/pedidos/" + encomenda + "/conclusao";
        UUID chaveConclusao = UUID.randomUUID();
        MvcResult resultado = escrever(dona, concluir, "{}", chaveConclusao).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.vendaId").isNotEmpty()).andReturn();
        String vendaId = json.readTree(resultado.getResponse().getContentAsByteArray()).get("vendaId").asText();
        escrever(dona, concluir, "{}", chaveConclusao).andExpect(status().isOk())
                .andExpect(jsonPath("$.vendaId").value(vendaId));
        escrever(concluir, "{}").andExpect(status().isOk()).andExpect(jsonPath("$.vendaId").value(vendaId));
        estoque(brigadeiro, 6, 0, 6);
        estoque(brownie, 3, 0, 3);
        painel("2026-11", 0, 0, 0);
        painel("2026-12", 0, 22, 22);
        mvc.perform(get(API + "/vendas/" + vendaId).with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagamentoPendente").value(false));
    }

    @Test
    void cancelamentoLiberaReservaEFalhaEmUmItemNaoReservaNenhum() throws Exception {
        mvc.perform(get(API + "/pedidos").param("status", "INVALIDO").with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isBadRequest());
        String brigadeiro = produto();
        String brownie = id(escrever("/produtos", "{\"nome\":\"Brownie\",\"precoVenda\":5}")
                .andExpect(status().isCreated()));
        escrever("/produtos/" + brigadeiro + "/ajustes", "{\"quantidade\":8}").andExpect(status().isOk());
        escrever("/produtos/" + brownie + "/ajustes", "{\"quantidade\":2}").andExpect(status().isOk());
        escrever("/pedidos", pedido(brigadeiro, 3, brownie, 5)).andExpect(status().isConflict());
        estoque(brigadeiro, 8, 0, 8);
        estoque(brownie, 2, 0, 2);

        String id = id(escrever("/pedidos", pedido(brigadeiro, 3, brownie, 1))
                .andExpect(status().isCreated()));
        escrever("/pedidos/" + id + "/cancelamento", "{}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
        escrever("/pedidos/" + id + "/conclusao", "{}").andExpect(status().isConflict());
        estoque(brigadeiro, 8, 0, 8);
        estoque(brownie, 2, 0, 2);
        painel("2026-11", 0, 0, 0);
    }

    private void estoque(String produto, int total, int reservado, int disponivel) throws Exception {
        mvc.perform(get(API + "/produtos/" + produto).with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeTotal").value(total))
                .andExpect(jsonPath("$.quantidadeReservada").value(reservado))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(disponivel));
    }

    private static String pedido(String primeiro, int qtdPrimeiro, String segundo, int qtdSegundo) {
        return """
                {"cliente":"Ana","dataEntrega":"2026-12-01",
                 "itens":[{"produtoId":"%s","quantidade":%d},{"produtoId":"%s","quantidade":%d}]}
                """.formatted(primeiro, qtdPrimeiro, segundo, qtdSegundo);
    }

    @Test
    void embalagemCompartilhadaBaixaEstoqueSemContarNovoGasto() throws Exception {
        MvcResult lista = mvc.perform(get(API + "/ingredientes").param("busca", "Embalagens")
                        .with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("EMBALAGEM")).andReturn();
        String embalagem = json.readTree(lista.getResponse().getContentAsByteArray())
                .get("content").get(0).get("id").asText();
        String ingrediente = ingrediente();
        escrever("/ingredientes/" + ingrediente + "/compras", "{\"quantidade\":1,\"valorTotal\":30}")
                .andExpect(status().isCreated());
        escrever("/ingredientes/" + embalagem + "/compras", "{\"quantidade\":10,\"valorTotal\":20}")
                .andExpect(status().isCreated());

        String produto = produto();
        String entrada = """
                {"rendimento":10,"ingredientes":[{"insumoId":"%s","quantidade":0.3}],
                 "quantidadeEmbalagem":4,"custoEmbalagem":3}
                """.formatted(ingrediente);
        escrever("/producao/" + produto, entrada).andExpect(status().isCreated())
                .andExpect(jsonPath("$.custoIngredientes").value(9))
                .andExpect(jsonPath("$.custoEmbalagem").value(3))
                .andExpect(jsonPath("$.custoUnitario").value(1.2));
        mvc.perform(get(API + "/ingredientes/" + embalagem).with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(6));
        painel("2026-11", 50, 0, -50);
    }

    @Test
    void salvaPngValidoEProtegeFotoEntreContas() throws Exception {
        String produto = produto();
        String rota = API + "/produtos/" + produto + "/foto";
        var imagem = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        imagem.setRGB(0, 0, 0xff884422);
        var bytes = new ByteArrayOutputStream();
        assertThat(ImageIO.write(imagem, "png", bytes)).isTrue();
        var png = new MockMultipartFile("file", "brigadeiro.png", "image/png", bytes.toByteArray());

        mvc.perform(multipart(HttpMethod.PUT, rota).file(png)
                        .with(oidcLogin().oidcUser(dona)).with(csrf().asHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(rota));
        var outraConta = novaConta();
        mvc.perform(get(rota).with(oidcLogin().oidcUser(outraConta))).andExpect(status().isNotFound());
        mvc.perform(multipart(HttpMethod.PUT, rota).file(png)
                        .with(oidcLogin().oidcUser(outraConta)).with(csrf().asHeader()))
                .andExpect(status().isNotFound());

        var falso = new MockMultipartFile("file", "brigadeiro.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart(HttpMethod.PUT, rota).file(falso)
                        .with(oidcLogin().oidcUser(dona)).with(csrf().asHeader()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("FOTO_INVALIDA"));
        byte[] conteudo = mvc.perform(get(rota).with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageIO.read(new ByteArrayInputStream(conteudo)).getRGB(0, 0)).isEqualTo(0xff884422);
    }

    private void painel(String mes, double gastos, double recebimentos, double saldo) throws Exception {
        mvc.perform(get(API + "/painel").param("mes", mes).with(oidcLogin().oidcUser(dona)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.gastos").value(gastos))
                .andExpect(jsonPath("$.recebimentos").value(recebimentos)).andExpect(jsonPath("$.saldo").value(saldo));
    }

    private String ingrediente() throws Exception {
        return id(escrever("/ingredientes", "{\"nome\":\"Chocolate\",\"unidade\":\"KG\"}")
                .andExpect(status().isCreated()));
    }

    private String produto() throws Exception {
        return id(escrever("/produtos", "{\"nome\":\"Brigadeiro\",\"precoVenda\":3}")
                .andExpect(status().isCreated()));
    }

    private static String producao(String ingrediente, String quantidade, int rendimento, boolean aceitar) {
        return """
                {"rendimento":%d,"ingredientes":[{"insumoId":"%s","quantidade":%s}],"aceitarEstoqueNegativo":%b}
                """.formatted(rendimento, ingrediente, quantidade, aceitar);
    }

    private static String venda(String produto, int quantidade, String total, boolean pendente) {
        return """
                {"itens":[{"produtoId":"%s","quantidade":%d}],"valorTotal":%s,"pagamentoPendente":%b}
                """.formatted(produto, quantidade, total, pendente);
    }

    private String id(ResultActions resposta) throws Exception {
        return json.readTree(resposta.andReturn().getResponse().getContentAsByteArray()).get("id").asText();
    }

    private ResultActions escrever(String rota, String corpo) throws Exception {
        return escrever(dona, rota, corpo, UUID.randomUUID());
    }

    private ResultActions escrever(OidcUser usuaria, String rota, String corpo, UUID chave) throws Exception {
        return mvc.perform(post(API + rota).with(oidcLogin().oidcUser(usuaria)).with(csrf().asHeader())
                .header("Idempotency-Key", chave).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private OidcUser novaConta() {
        var usuaria = identidadeFicticia();
        contas.cadastrarSeNecessario(usuaria);
        return usuaria;
    }

    private OidcUser identidadeFicticia() {
        var token = new OidcIdToken("test", NOVEMBRO, DEZEMBRO,
                Map.of("sub", UUID.randomUUID().toString(), "iss", "https://accounts.google.com"));
        return new DefaultOidcUser(java.util.List.of(new SimpleGrantedAuthority("OIDC_USER")), token);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HorarioTeste {
        @Bean
        @Primary
        Clock relogioDaDemo() {
            return new Clock() {
                @Override
                public ZoneId getZone() { return ZoneOffset.UTC; }

                @Override
                public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }

                @Override
                public Instant instant() { return AGORA.get(); }
            };
        }
    }
}
