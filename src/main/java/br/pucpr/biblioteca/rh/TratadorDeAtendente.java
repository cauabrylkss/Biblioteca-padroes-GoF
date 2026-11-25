package br.pucpr.biblioteca.rh;

import br.pucpr.biblioteca.cadastro.CadastroDeAtendente;
import br.pucpr.biblioteca.cadastro.Resultado;
import java.util.Objects;
import java.util.Optional;

/**
 * Tratador de atendentes (Trabalho 02), realiza {@link iAtendente}. Delega a inclusão ao {@link CadastroDeAtendente}
 * (Template Method, R7) e as demais operações ao {@link Relacionamento}.
 */
public class TratadorDeAtendente implements iAtendente {

    private final CadastroDeAtendente cadastro;
    private final Relacionamento relacionamento;

    public TratadorDeAtendente(CadastroDeAtendente cadastro, Relacionamento relacionamento) {
        this.cadastro = Objects.requireNonNull(cadastro, "cadastro");
        this.relacionamento = Objects.requireNonNull(relacionamento, "relacionamento");
    }

    @Override
    public Resultado<FichaDoPapel> inserirAtendente(String cpf) {
        return cadastro.incluir(cpf).mapear(this::ficha);
    }

    @Override
    public Optional<FichaDoPapel> consultarAtendente(long idAtendente) {
        return relacionamento.consultarAtendente(idAtendente).map(this::ficha);
    }

    @Override
    public void excluirAtendente(long idAtendente) {
        relacionamento.excluirAtendente(idAtendente);
    }

    private FichaDoPapel ficha(Atendente atendente) {
        return FichaDoPapel.de(atendente, relacionamento.exigirPessoa(atendente.getIdPessoa()));
    }
}
