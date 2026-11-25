package br.pucpr.biblioteca.rh;

/**
 * Dados de um papel (estudante ou atendente) para exibição na interface.
 *
 * @param id       id do papel (id do estudante ou do atendente — é o id usado nas operações de circulação)
 * @param idPessoa id da pessoa que desempenha o papel
 */
public record FichaDoPapel(long id, long idPessoa, String nomeCompleto, String cpf) {

    public static FichaDoPapel de(Papel papel, Pessoa pessoa) {
        return new FichaDoPapel(papel.getId(), pessoa.getId(), pessoa.getNomeCompleto(), pessoa.getCpfFormatado());
    }
}
