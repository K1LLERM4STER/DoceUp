package br.com.doceup.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class AcessoPrivado {
    private final Set<String> emails;

    public AcessoPrivado(@Value("${doceup.emails-autorizados:}") String configuracao) {
        this.emails = Arrays.stream(configuracao.split(","))
                .map(String::trim).filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    public boolean permite(Object principal) {
        if (!(principal instanceof OidcUser usuario) || !Boolean.TRUE.equals(usuario.getEmailVerified())
                || usuario.getEmail() == null) {
            return false;
        }
        return emails.contains(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
    }
}
