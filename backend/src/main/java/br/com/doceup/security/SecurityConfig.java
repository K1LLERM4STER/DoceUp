package br.com.doceup.security;

import br.com.doceup.exception.Problemas;
import br.com.doceup.usuario.service.ContaAtual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
class SecurityConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain rotas(HttpSecurity http, ContaAtual conta, Problemas problemas) {
        var api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
        var login = new LoginUrlAuthenticationEntryPoint("/login");
        http
            .authorizeHttpRequests(rotas -> rotas
                .requestMatchers("/login", "/oauth2/authorization/google", "/login/oauth2/code/google",
                        "/assets/**", "/favicon.ico", "/favicon.svg", "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint((req, res, erro) -> {
                    if (api.matches(req)) {
                        problemas.responder(res, HttpStatus.UNAUTHORIZED, "SEM_SESSAO", "Entre para continuar.");
                    } else {
                        login.commence(req, res, erro);
                    }
                })
                .accessDeniedHandler((req, res, erro) -> {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (api.matches(req) && (auth == null || auth instanceof AnonymousAuthenticationToken)) {
                        problemas.responder(res, HttpStatus.UNAUTHORIZED, "SEM_SESSAO", "Entre para continuar.");
                        return;
                    }
                    LOG.info("Acesso negado");
                    problemas.responder(res, HttpStatus.FORBIDDEN, "ACESSO_NEGADO",
                            "Atualize a página antes de tentar novamente.");
                }))
            .oauth2Login(oauth -> oauth.loginPage("/login")
                .successHandler((req, res, auth) -> {
                    try {
                        conta.cadastrarSeNecessario((OidcUser) auth.getPrincipal());
                        res.sendRedirect(req.getContextPath() + "/login");
                    } catch (RuntimeException erro) {
                        LOG.error("Falha ao cadastrar a conta: {}", erro.getClass().getSimpleName());
                        SecurityContextHolder.clearContext();
                        var sessao = req.getSession(false);
                        if (sessao != null) sessao.invalidate();
                        res.sendRedirect(req.getContextPath() + "/login?erro=1");
                    }
                })
                .failureHandler((req, res, erro) -> {
                    LOG.info("Falha no login Google: {}", erro.getClass().getSimpleName());
                    res.sendRedirect(req.getContextPath() + "/login?erro=1");
                }))
            .requestCache(cache -> cache.disable())
            .logout(logout -> logout.deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value())))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' blob: data:; "
                    + "connect-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; form-action 'self'")))
            .addFilterBefore(new CorrelacaoFilter(), SecurityContextHolderFilter.class);
        return http.build();
    }
}
