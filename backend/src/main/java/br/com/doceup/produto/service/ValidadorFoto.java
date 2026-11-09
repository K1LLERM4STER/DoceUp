package br.com.doceup.produto.service;

import br.com.doceup.exception.RegraException;
import br.com.doceup.produto.dto.ProdutoDtos.Imagem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

final class ValidadorFoto {
    private static final long LIMITE_BYTES = 2L * 1024 * 1024;
    private static final int LIMITE_PIXELS = 1600;

    private ValidadorFoto() {}

    static Imagem ler(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw invalida();
        }
        if (arquivo.getSize() > LIMITE_BYTES) {
            throw grande();
        }
        try (var dados = arquivo.getInputStream(); var entrada = new MemoryCacheImageInputStream(dados)) {
            var leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) {
                throw invalida();
            }
            var leitor = leitores.next();
            try {
                String formato = leitor.getFormatName().toLowerCase(Locale.ROOT);
                validarFormato(arquivo, formato);
                leitor.setInput(entrada, true, true);
                if (leitor.getWidth(0) > LIMITE_PIXELS || leitor.getHeight(0) > LIMITE_PIXELS) {
                    throw new RegraException(HttpStatus.BAD_REQUEST, "FOTO_DIMENSOES",
                            "Use uma foto com até 1600 × 1600 pixels.");
                }
                var imagem = leitor.read(0);
                var saida = new ByteArrayOutputStream();
                if (!ImageIO.write(imagem, formato, saida)) {
                    throw invalida();
                }
                if (saida.size() > LIMITE_BYTES) {
                    throw grande();
                }
                return new Imagem("image/" + formato, saida.toByteArray());
            } finally {
                leitor.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw invalida();
        }
    }

    private static void validarFormato(MultipartFile arquivo, String formato) {
        String nome = arquivo.getOriginalFilename() == null
                ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean png = "png".equals(formato) && nome.endsWith(".png");
        boolean jpeg = "jpeg".equals(formato) && (nome.endsWith(".jpg") || nome.endsWith(".jpeg"));
        if ((!png && !jpeg) || !("image/" + formato).equalsIgnoreCase(arquivo.getContentType())) {
            throw invalida();
        }
    }

    private static RegraException invalida() {
        return new RegraException(HttpStatus.BAD_REQUEST, "FOTO_INVALIDA", "Escolha uma imagem PNG ou JPEG válida.");
    }

    private static RegraException grande() {
        return new RegraException(HttpStatus.PAYLOAD_TOO_LARGE, "FOTO_GRANDE", "Escolha uma foto de até 2 MiB.");
    }
}
