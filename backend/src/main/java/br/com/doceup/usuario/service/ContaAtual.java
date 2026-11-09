package br.com.doceup.usuario.service;

import br.com.doceup.exception.RegraException;
import br.com.doceup.usuario.repository.ContaRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaAtual {
    private final ContaRepository contas;

    public ContaAtual(ContaRepository contas) {
        this.contas = contas;
    }

    @Transactional(readOnly = true)
    public UUID requireNegocioId(OidcUser usuario) {
        if (usuario == null) {
            throw new RegraException(HttpStatus.UNAUTHORIZED, "SEM_SESSAO", "Entre para continuar.");
        }
        return contas.buscarNegocio(usuario.getIssuer().toString(), usuario.getSubject())
                .orElseThrow(() -> new RegraException(
                        HttpStatus.UNAUTHORIZED, "SEM_SESSAO", "Entre novamente para continuar."));
    }

    @Transactional
    public UUID cadastrarSeNecessario(OidcUser usuario) {
        String emissor = usuario.getIssuer().toString();
        String identificador = usuario.getSubject();
        return contas.buscarNegocio(emissor, identificador).orElseGet(() -> {
            contas.bloquearCadastro(emissor, identificador);
            return contas.buscarNegocio(emissor, identificador).orElseGet(() -> {
                UUID negocio = UUID.randomUUID();
                contas.criarNegocio(negocio);
                contas.criarUsuario(emissor, identificador, negocio);
                return negocio;
            });
        });
    }
}
