package br.pucpr.biblioteca.biblioteca.acervo;

/**
 * Metadados de um livro devolvidos pela consulta ao acervo (R9): só o que identifica e descreve o livro.
 *
 * <p><b>Não tem o estado do livro (de propósito).</b> Por isso pode ficar em cache sem risco: uma informação antiga
 * nunca diz que o livro está LIVRE. Quem decide uma reserva é a fachada, que lê o {@code Livro} atual do repositório.
 */
public record MetadadosLivro(long id, String titulo) {
}
