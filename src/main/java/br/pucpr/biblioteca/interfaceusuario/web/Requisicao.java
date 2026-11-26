package br.pucpr.biblioteca.interfaceusuario.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parâmetros de uma chamada à API: os da URL ({@code ?idLivro=1}) e, no {@code POST}, os do formulário enviado pela
 * página ({@code application/x-www-form-urlencoded}). Só lê e valida o formato — nenhuma regra de domínio.
 */
final class Requisicao {

    private final Map<String, String> parametros;

    private Requisicao(Map<String, String> parametros) {
        this.parametros = parametros;
    }

    /** Junta os parâmetros da URL e do corpo; se o mesmo nome vier nos dois, vale o do corpo. */
    static Requisicao de(String query, String corpo) {
        Map<String, String> parametros = new HashMap<>();
        lerFormulario(query, parametros);
        lerFormulario(corpo, parametros);
        return new Requisicao(parametros);
    }

    private static void lerFormulario(String texto, Map<String, String> destino) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        for (String par : texto.split("&")) {
            int igual = par.indexOf('=');
            String nome = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            destino.put(URLDecoder.decode(nome, StandardCharsets.UTF_8),
                    URLDecoder.decode(valor, StandardCharsets.UTF_8).strip());
        }
    }

    /** Texto obrigatório. */
    String texto(String nome) {
        return textoOpcional(nome).orElseThrow(() -> new IllegalArgumentException("informe " + nome));
    }

    /** Texto que pode ficar em branco. */
    Optional<String> textoOpcional(String nome) {
        return Optional.ofNullable(parametros.get(nome)).filter(valor -> !valor.isEmpty());
    }

    /** Número inteiro obrigatório (ids, dias). */
    long numero(String nome) {
        return numeroOpcional(nome).orElseThrow(() -> new IllegalArgumentException("informe " + nome));
    }

    /** Número que pode ficar em branco (ex.: grupo do acervo no cadastro do livro). */
    Optional<Long> numeroOpcional(String nome) {
        return textoOpcional(nome).map(valor -> {
            try {
                return Long.parseLong(valor);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(nome + " deve ser um número inteiro: " + valor);
            }
        });
    }

    /** Sim/não de uma caixa de seleção: "true", "sim", "s" ou "on" = sim; ausente = não. */
    boolean simNao(String nome) {
        return textoOpcional(nome)
                .map(valor -> switch (valor.toLowerCase()) {
                    case "true", "sim", "s", "on" -> true;
                    case "false", "nao", "não", "n", "off" -> false;
                    default -> throw new IllegalArgumentException(nome + " deve ser sim ou não: " + valor);
                })
                .orElse(false);
    }
}
