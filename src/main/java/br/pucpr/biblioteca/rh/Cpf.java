package br.pucpr.biblioteca.rh;

import br.pucpr.biblioteca.compartilhado.RegraDeNegocioException;
import java.util.Optional;

/**
 * Validação e formatação de CPF (decisão B-23): 11 dígitos, não todos iguais, com os dois dígitos verificadores
 * corretos. Internamente o CPF é guardado só com dígitos; a pontuação é aceita na entrada e usada na exibição.
 */
public final class Cpf {

    private Cpf() {
    }

    /**
     * @return o CPF só com dígitos
     * @throws RegraDeNegocioException se o CPF for inválido
     */
    public static String normalizar(String cpf) {
        return normalizarSeValido(cpf).orElseThrow(() -> new RegraDeNegocioException("CPF inválido: '" + cpf + "'."));
    }

    /** O CPF só com dígitos, ou vazio se ele for inválido (não lança exceção). */
    public static Optional<String> normalizarSeValido(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }
        String digitos = cpf.replaceAll("[.\\-\\s]", "");
        if (!digitos.matches("\\d{11}") ||digitos.chars().distinct().count() == 1) {
            return Optional.empty();
        }
        if (digitoVerificador(digitos, 9) != digitos.charAt(9) - '0'
                || digitoVerificador(digitos, 10) != digitos.charAt(10) - '0') {
            return Optional.empty();
        }
        return Optional.of(digitos);
    }

    /** "12345678909" → "123.456.789-09". */
    public static String formatar(String onzeDigitos) {
        String d = normalizar(onzeDigitos);
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    /** Dígito verificador na posição {@code posicao} (9 ou 10), calculado sobre os dígitos anteriores. */
    private static int digitoVerificador(String digitos, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
