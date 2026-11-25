package br.pucpr.biblioteca.cadastro;

import java.util.Optional;

/**
 * Papel GoF: <b>AbstractClass</b> (Template Method, R7) — rotina comum de inclusão de cadastros.
 *
 * <p>{@link #incluir(Object)} é o <b>método-modelo</b>: é {@code final}, então nenhuma subclasse muda a ordem dos
 * passos. As subclasses ({@link CadastroDeLivro}, {@link CadastroDePessoa}, {@link CadastroDeEstudante},
 * {@link CadastroDeAtendente}) só implementam os passos variáveis:
 * <ol>
 *   <li>{@link #buscarExistente} — se já existe, devolve o cadastro existente e <b>para</b> (nada é gravado);</li>
 *   <li>{@link #validar} — dados obrigatórios e formato;</li>
 *   <li>{@link #verificarPreRequisitos} — <b>gancho</b> (vazio por padrão): ex. estudante exige pessoa com o CPF;</li>
 *   <li>{@link #criar} → {@link #armazenar} — a única gravação acontece aqui, depois de todas as verificações;</li>
 *   <li>{@link #aposArmazenar} — <b>gancho</b> (vazio por padrão): ex. catalogar o livro num grupo do acervo;</li>
 *   <li>{@link #apresentar} — monta o resultado.</li>
 * </ol>
 * Como validação e pré-requisitos vêm antes de {@code armazenar}, uma falha nesses passos nunca deixa gravação
 * parcial.
 *
 * @param <D> dados de entrada da inclusão
 * @param <E> entidade cadastrada
 */
public abstract class CadastroTemplate<D, E> {

    /** Método-modelo: a ordem dos passos é fixa e não pode ser sobrescrita. */
    public final Resultado<E> incluir(D dados) {
        Optional<E> existente = buscarExistente(dados);
        if (existente.isPresent()) {
            return apresentar(existente.get(), false);
        }
        validar(dados);
        verificarPreRequisitos(dados);
        E novo = criar(dados);
        armazenar(novo);
        aposArmazenar(dados, novo);
        return apresentar(novo, true);
    }

    /** Passo variável: o cadastro já existe? (chave de duplicidade de cada tipo, B-22) */
    protected abstract Optional<E> buscarExistente(D dados);

    /** Passo variável: verifica dados obrigatórios e formato; lança exceção se inválidos. */
    protected abstract void validar(D dados);

    /** Gancho: pré-requisitos em outros cadastros. Por padrão não há nenhum. */
    protected void verificarPreRequisitos(D dados) {
    }

    /** Passo variável: cria a entidade (ainda não gravada). */
    protected abstract E criar(D dados);

    /** Passo variável: grava a entidade no ponto de acesso correspondente. */
    protected abstract void armazenar(E entidade);

    /** Gancho: ação extra depois de gravar. Por padrão não faz nada. */
    protected void aposArmazenar(D dados, E entidade) {
    }

    /** Monta o resultado; {@code novo} diz se o cadastro foi criado agora ou já existia. */
    protected Resultado<E> apresentar(E entidade, boolean novo) {
        return new Resultado<>(entidade, novo);
    }
}
