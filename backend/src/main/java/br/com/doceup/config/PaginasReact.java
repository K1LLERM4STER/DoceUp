package br.com.doceup.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class PaginasReact {
    @GetMapping({"/", "/login", "/produtos", "/produtos/novo", "/produtos/{id}/editar",
        "/ingredientes", "/producao/{id}", "/vendas", "/vendas/{id}", "/pedidos", "/pedidos/novo"})
    ResponseEntity<Resource> pagina() {
        Resource index = new ClassPathResource("static/index.html");
        if (!index.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).cacheControl(CacheControl.noStore()).body(index);
    }
}
