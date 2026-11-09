# DoceUp

Controle de ingredientes, doces prontos, produção e vendas para quem vende doces como renda extra. O painel mostra **recebimentos menos gastos**, por dia ou mês, em reais. O custo de produção é uma referência de preço.

[Painel no celular](docs/painel-mobile.png) · [Encomendas no celular](docs/encomendas-mobile.png). As capturas mostram dados fictícios.

## Escopo

- **P0.** Login Google, negócio por conta, ingredientes e compras, catálogo com foto opcional, produção, vendas, recebimentos e painel. O fluxo principal é manual.
- **P1a.** Encomendas com vários produtos. Reservar separa os doces prontos, cancelar os devolve para venda e **Concluir e receber** registra a entrega e o recebimento integral uma única vez.
- **P1b.** Venda por voz como rascunho revisável. A IA sugere os produtos, quantidades e estado do pagamento. Somente a confirmação normal da venda movimenta estoque e dinheiro.
- **P2.** Produção vinculada à encomenda, reversões guiadas, pagamentos parciais ou antecipados, histórico geral e operação offline. Funcionalidades fiscais ficam fora do escopo.

## Rodar localmente

Requisitos: Java 21, Node.js 22, npm, PostgreSQL 18 ou Docker com Compose e um cliente OAuth do Google para aplicativo Web. Maven é executado pelo wrapper em `backend/`.

1. Copie `.env.example` para `.env` e troque as senhas de exemplo. Para o banco do Compose, use a mesma senha em `POSTGRES_PASSWORD` e `DATABASE_PASSWORD`.
2. Use `docker compose up -d db` na raiz, ou crie manualmente o banco e o usuário em um PostgreSQL local.
3. No cliente OAuth, adicione **`http://localhost:5173/login/oauth2/code/google`** às URLs de redirecionamento autorizadas. Preencha `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` no arquivo local. Use `localhost` também no navegador.
4. Na raiz, inicie o back-end no PowerShell:

   ```powershell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
           [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
       }
   }
   Set-Location backend
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
   ```

5. Em outro terminal, entre em `frontend/`, execute `npm ci` e `npm run dev -- --strictPort`. Abra `http://localhost:5173`. O Vite encaminha API e OAuth ao Spring na porta 8080.

A aplicação sobe com Flyway e valida o esquema. O perfil `dev` habilita o OpenAPI autenticado no endereço do back-end. Para mudar o host local ou a porta do front-end, ajuste também `GOOGLE_REDIRECT_URI` e o cliente OAuth.

### Dados fictícios para demonstrar

1. Cadastre “Chocolate” em KG e adicione uma compra de 1 kg por R$ 30.
2. Cadastre “Brigadeiro” a R$ 3 e produza 20 usando 0,3 kg. O custo de referência é R$ 0,45 por unidade.
3. Venda cinco brigadeiros por R$ 12,50 e marque o pagamento como pendente. O estoque cai para 15 e o saldo fica em −R$ 30.
4. Marque a venda como recebida. Se ocorrer no mesmo mês, o saldo passa para −R$ 17,50. Se for no mês seguinte, os R$ 12,50 aparecem apenas naquele período.
5. Em **Pedidos**, reserve quatro brigadeiros para uma cliente fictícia. Os quatro continuam no estoque, mas aparecem separados. Conclua o pedido para retirar as unidades e registrar R$ 12 de recebimento. Em outro pedido, experimente cancelar e confira se os doces voltaram a ficar disponíveis.

## Arquitetura e decisões

- `frontend/` usa React, Vite, React Router e CSS. Os formulários consultam a API no mesmo domínio e o servidor confirma os valores.
- `backend/` usa Java 21 e Spring Boot 4.1.2. Spring MVC recebe as requisições. Bean Validation valida entradas. Spring Security e OAuth2 Client cuidam da sessão. Spring Data JPA e consultas JDBC parametrizadas operam no PostgreSQL. Flyway aplica as migrações. Actuator expõe o health e springdoc 3.1.1 documenta a API no ambiente de desenvolvimento.
- Os módulos separam controllers, DTOs, serviços, repositórios, modelos e mapeamento. O painel soma compras e recebimentos persistidos no fuso `America/Sao_Paulo`.
- Fotos opcionais ficam no PostgreSQL com acesso restrito por negócio. O produto usa uma imagem padrão quando não tem foto.
- Quantidades de insumos aceitam frações. Dinheiro confirmado usa duas casas decimais. O custo unitário de referência mantém precisão maior e fica gravado na produção.
- Entrada gratuita e ajuste preservam o último preço pago. Embalagens formam um estoque único por conta, e o custo atribuído na produção é informado à parte. A produção não gera outra despesa.
- Venda já registrada mantém seu histórico. A confirmação de recebimento é uma operação própria. Edição retroativa ou estorno completo fica para uma regra de produto posterior.

## Segurança e testes

- O Google identifica a conta por emissor e identificador estável. O DoceUp não armazena senhas.
- O servidor deriva o negócio da sessão. Consultas e alterações filtram pelo negócio para impedir acesso cruzado.
- A sessão usa cookie HttpOnly, SameSite=Lax e Secure em produção. CSRF permanece ligado e o React usa o token entregue pela API.
- Validação de DTOs e rejeição de campos extras impedem que valores inesperados sejam aplicados ao modelo. Erros da API usam ProblemDetail.
- Transações, bloqueios de estoque e chaves de idempotência persistidas protegem contra baixas concorrentes e reenvio. Compras, lotes e pagamentos guardam seus valores históricos.
- Segredos ficam fora do repositório. Em produção, apenas o health mínimo fica exposto. Aplique HTTPS e mantenha a instância atrás do proxy da hospedagem.
- Limitação distribuída de requisições, rotinas de retenção, backup e restauração precisam de revisão antes de usar dados reais. Esta é uma demonstração de portfólio.

Em `backend/`, execute `./mvnw test` ou `./mvnw.cmd test`. Por padrão o teste de integração sobe PostgreSQL via Testcontainers e requer Docker. Para usar um PostgreSQL de teste exclusivo já iniciado, exporte `DOCEUP_TEST_DATABASE_URL`, `DOCEUP_TEST_DATABASE_USER` e `DOCEUP_TEST_DATABASE_PASSWORD`. Os testes aplicam migrações e criam registros. Use um banco descartável. Os testes simulam a identidade Google e verificam a sessão e o CSRF na API, sem enviar dados ao Google.

Em `frontend/`, execute `npm ci` e `npm run build` para verificar a compilação de produção.

Se o controle de aplicativos do Windows bloquear o módulo nativo do Rollup, use a distribuição WebAssembly oficial no ambiente local e rode o build novamente:

```powershell
npm install --no-save --package-lock=false "rollup@npm:@rollup/wasm-node@4.59.0"
npm run build
```

Essa instalação local não modifica o manifesto nem o lockfile. No Docker, o build usa as dependências padrão do lockfile.

Verificação local: a suíte de integração cobre estoque, pagamentos, isolamento entre contas, reserva, cancelamento e conclusão de encomendas no PostgreSQL 18. O pacote com o React foi verificado no perfil de produção. A entrada completa com Google depende de um cliente OAuth real.

## Publicação

O `Dockerfile` junta React e Spring na mesma origem. O `render.yaml` prepara um serviço Docker no Render com o plano gratuito. Como o disco do serviço é temporário, use PostgreSQL externo persistente. **Render Free + Neon Free** é a opção proposta para a demo. Koyeb Free também roda Docker, com restrições semelhantes para armazenamento local. Confira as condições vigentes antes de criar recursos. Render Free pode demorar no primeiro acesso após inatividade, e o Postgres gratuito do próprio Render expira após 30 dias.

1. Conecte um repositório autorizado ao Render e aplique o `render.yaml`.
2. Crie o PostgreSQL, configure `DATABASE_URL` JDBC com TLS, usuário e senha no painel de segredos.
3. Configure `GOOGLE_REDIRECT_URI=https://<host>/login/oauth2/code/google` e cadastre esse endereço no cliente Google.
4. Configure as credenciais do Google no painel de segredos e publique. Verifique health, HTTPS, login real e persistência do exemplo fictício após reiniciar a aplicação.

Referências: [Render Free](https://render.com/docs/free), [Neon](https://neon.com/docs/introduction/plans), [Koyeb](https://www.koyeb.com/docs/reference/instances), [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect).

**Estado de publicação:** adiada a pedido do usuário. O Git local aponta para o repositório informado. O envio ao GitHub, o endereço público e o teste com um cliente Google real ficam para a etapa de implantação.
