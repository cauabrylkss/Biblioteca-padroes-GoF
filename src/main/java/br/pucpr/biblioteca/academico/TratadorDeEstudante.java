package br.pucpr.biblioteca.academico;

import br.pucpr.biblioteca.cadastro.CadastroDeEstudante;
import br.pucpr.biblioteca.cadastro.Resultado;
import br.pucpr.biblioteca.rh.FichaDoPapel;
import br.pucpr.biblioteca.rh.Relacionamento;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador de estudantes (Trabalho 02), realiza {@link iEstudante}. Delega a inclusão ao {@link CadastroDeEstudante}
 * (Template Method, R7) e as demais operações ao {@link Relacionamento} do RH (o Acadêmico requer {@code iPessoa}).
 */
public class TratadorDeEstudante implements iEstudante {

    private final CadastroDeEstudante cadastro;
    private final Relacionamento relacionamento;

    public TratadorDeEstudante(CadastroDeEstudante cadastro, Relacionamento relacionamento) {
        this.cadastro = Objects.requireNonNull(cadastro, "cadastro");
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
    }

    @Override
    public Resultado<FichaDoPapel> inserirEstudante(String cpf) {
        return cadastro.incluir(cpf).mapear(this::ficha);
    }

    @Override
    public Optional<FichaDoPapel> consultarEstudante(long idEstudante) {
        return relacionamento.consultarEstudante(idEstudante).map(this::ficha);
    }

    @Override
    public void excluirEstudante(long idEstudante) {
        relacionamento.excluirEstudante(idEstudante);
    }

    private FichaDoPapel ficha(Estudante estudante) {
        return FichaDoPapel.de(estudante, relacionamento.exigirPessoa(estudante.getIdPessoa()));
    }
}
