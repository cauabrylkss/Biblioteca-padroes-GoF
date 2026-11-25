package br.pucpr.biblioteca.biblioteca.acervo;

/**
 * Tipo de um grupo da árvore do acervo. É informativo: a profundidade é variável (subseção pode conter subseção);
 * {@code ACERVO} é reservado para a raiz da {@link OrganizacaoDoAcervo}.
 */
public enum TipoGrupo {
    ACERVO,
    UNIDADE,
    SECAO,
    SUBSECAO
}
