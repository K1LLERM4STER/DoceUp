package br.com.doceup.usuario.dto;

public record CsrfDto(String token, String headerName) {
}
