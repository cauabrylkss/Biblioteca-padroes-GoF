package br.pucpr.biblioteca.rh;

/** Papel de atendente da biblioteca: obrigatório para registrar empréstimos e devoluções (B-26). */
public class Atendente extends Papel {

    public Atendente(long idPessoa) {
        super(idPessoa);
    }

    /** Reconstitui um atendente gravado (usado pelos repositórios). */
    public static Atendente reconstituir(long id, long idPessoa) {
        Atendente a = new Atendente(idPessoa);
        a.atribuirId(id);
        return a;
    }
}
