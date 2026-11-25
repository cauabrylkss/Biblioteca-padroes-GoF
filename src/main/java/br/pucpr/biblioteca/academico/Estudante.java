package br.pucpr.biblioteca.academico;

import br.pucpr.biblioteca.rh.Papel;

/** Papel de estudante: quem reserva, pega emprestado, renova e devolve livros. */
public class Estudante extends Papel {

    public Estudante(long idPessoa) {
        super(idPessoa);
    }

    /** Reconstitui um estudante gravado (usado pelos repositórios). */
    public static Estudante reconstituir(long id, long idPessoa) {
        Estudante e = new Estudante(idPessoa);
        e.atribuirId(id);
        return e;
    }
}
