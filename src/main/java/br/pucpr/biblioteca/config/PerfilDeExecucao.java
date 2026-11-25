package br.pucpr.biblioteca.config;

import java.util.Locale;

/**
 * Perfil de execução (R8, propriedade {@code perfil}): decide, uma única vez, qual família de persistência a
 * aplicação usa.
 */
public enum PerfilDeExecucao {

    /** Família em memória — testes automatizados e demonstração com dados de exemplo. Aceita "teste" ou "memoria". */
    TESTE,

    /** Família relacional (MySQL). */
    PRODUCAO;

    public static PerfilDeExecucao de(String valor) {
        String v = valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "teste", "memoria" -> TESTE;
            case "producao" -> PRODUCAO;
            default -> throw new IllegalArgumentException(
                    "Perfil de execução inválido: '" + valor + "'. Use teste (ou memoria) ou producao.");
        };
    }
}
