package br.pucpr.biblioteca.interfaceusuario;

import br.pucpr.biblioteca.academico.iEstudante;
import br.pucpr.biblioteca.biblioteca.tratadores.iDevolucao;
import br.pucpr.biblioteca.biblioteca.tratadores.iEmprestimo;
import br.pucpr.biblioteca.biblioteca.tratadores.iLivro;
import br.pucpr.biblioteca.config.iAdministracao;
import br.pucpr.biblioteca.rh.iAtendente;
import br.pucpr.biblioteca.rh.iPessoa;
import java.util.Objects;

/**
 * Tudo o que a CLI conhece do sistema: as interfaces dos tratadores (nomes do Trabalho 02 + {@code iAdministracao}).
 * Nenhuma entidade, repositório ou serviço chega à interface.
 */
public record Tratadores(iLivro acervo, iPessoa pessoa, iEstudante estudante, iAtendente atendente,
                         iEmprestimo emprestimo, iDevolucao devolucao, iAdministracao administracao) {

    public Tratadores {
        Objects.requireNonNull(acervo, "acervo");
        Objects.requireNonNull(pessoa, "pessoa");
        Objects.requireNonNull(estudante, "estudante");
        Objects.requireNonNull(atendente, "atendente");
        Objects.requireNonNull(emprestimo, "emprestimo");
        Objects.requireNonNull(devolucao, "devolucao");
        Objects.requireNonNull(administracao, "administracao");
    }
}
