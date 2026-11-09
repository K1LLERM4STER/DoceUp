package br.com.doceup.estoque.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaEstoque<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
    public static <T> PaginaEstoque<T> de(Page<T> page) {
        return new PaginaEstoque<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
