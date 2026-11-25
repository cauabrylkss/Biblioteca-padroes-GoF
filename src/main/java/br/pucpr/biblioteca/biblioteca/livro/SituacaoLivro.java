package br.pucpr.biblioteca.biblioteca.livro;

/**
 * Nome do estado do livro como ele é <b>gravado</b> na persistência e mostrado na interface.
 *
 * <p>Não tem comportamento: quem decide o que cada evento faz são as classes de estado (R1). Na leitura do banco,
 * {@link EstadoLivro#de(SituacaoLivro)} transforma este valor de volta no objeto de estado correspondente.
 */
public enum SituacaoLivro {
    LIVRE,
    RESERVADO,
    EMPRESTADO,
    RENOVADO,
    DEVOLVIDO,
    REPARANDO,
    INCINERADO
}
