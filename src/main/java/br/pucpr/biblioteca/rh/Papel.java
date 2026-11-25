package br.pucpr.biblioteca.rh;

/**
 * Papel que uma {@link Pessoa} desempenha na instituição (modelo do Trabalho 02: Pessoa 1 — 0..* Papel).
 * Subclasses: {@link Atendente} (RH) e {@code Estudante} (Acadêmico). O papel guarda só o id da pessoa; os dados
 * pessoais são consultados no {@link Relacionamento}.
 */
public abstract class Papel {

    private Long id;
    private final long idPessoa;

    protected Papel(long idPessoa) {
        if (idPessoa <= 0) {
            throw new IllegalArgumentException("O papel precisa de uma pessoa gravada (id positivo)");
        }
        this.idPessoa = idPessoa;
    }

    /** Chamado pelo repositório ao gravar; um id nunca é trocado. */
    public void atribuirId(long novoId) {
        if (id != null) {
            throw new IllegalStateException(getClass().getSimpleName() + " já possui id " + id);
        }
        this.id = novoId;
    }

    public Long getId() {
        return id;
    }

    public long getIdPessoa() {
        return idPessoa;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + ", pessoa " + idPessoa + "]";
    }
}
