package br.pucpr.biblioteca.rh;

/** Dados de uma pessoa para exibição na interface (cópia imutável; não dá acesso à entidade). */
public record FichaDaPessoa(long id, String nome, String sobrenome, String cpf) {

    public static FichaDaPessoa de(Pessoa p) {
        return new FichaDaPessoa(p.getId(), p.getNome(), p.getSobrenome(), p.getCpfFormatado());
    }
}
