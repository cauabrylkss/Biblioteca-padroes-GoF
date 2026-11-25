# R7 — Template Method: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/cadastro/` (`CadastroTemplate`, `CadastroDeLivro`, `CadastroDePessoa`,
`CadastroDeEstudante`, `CadastroDeAtendente`, `Resultado`) · Interfaces: `iLivro` (`TratadorDeAcervo`), `iPessoa`
(`TratadorDePessoa`), `iEstudante` (`TratadorDeEstudante`), `iAtendente` (`TratadorDeAtendente`) ·
Testes: `src/test/java/.../cadastro/R07TemplateMethodTest.java`

## A ideia em uma frase
Todo cadastro segue a **mesma receita** (buscar existente → validar → pré-requisitos → criar → armazenar →
apresentar); a receita fica numa classe abstrata com método `final`, e cada cadastro só preenche os passos que mudam.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| AbstractClass | `CadastroTemplate<D, E>` | `incluir(D)` **final** = método-modelo |
| ConcreteClass | `CadastroDeLivro` | Duplicidade pelo id; ganchos: grupo existe / catalogar no grupo (R10) |
| | `CadastroDePessoa` | Duplicidade pelo CPF; valida nome e CPF (`rh.Cpf`) |
| | `CadastroDeEstudante`, `CadastroDeAtendente` | Duplicidade = pessoa já tem o papel; pré-requisito = pessoa com o CPF |

```java
public final Resultado<E> incluir(D dados) {
    Optional<E> existente = buscarExistente(dados);
    if (existente.isPresent()) return apresentar(existente.get(), false);   // duplicado: devolve o existente
    validar(dados);
    verificarPreRequisitos(dados);      // gancho
    E novo = criar(dados);
    armazenar(novo);                    // única gravação
    aposArmazenar(dados, novo);         // gancho
    return apresentar(novo, true);
}
```

## Perguntas do PDF
**Quais passos são fixos e quais variam?**
Fixa é a **ordem** (o corpo de `incluir`, que é `final` — `R07_metodoModeloEhFinal`). Variam: `buscarExistente`,
`validar`, `criar`, `armazenar` (abstratos) e os ganchos. `R07_inclusaoNovaPercorreOsPassosNaOrdemFixa` anota os
passos e confere a sequência.

**O que é um gancho e onde é usado?**
Método com implementação padrão **vazia**, que a subclasse sobrescreve só se precisar. Aqui: `verificarPreRequisitos`
(usado por Estudante/Atendente: exige pessoa com o CPF; e por Livro: o grupo informado existe) e `aposArmazenar`
(usado por Livro: catalogar o livro no grupo da árvore). `CadastroDePessoa` não sobrescreve nenhum.

**Por que falha de pré-requisito não permite gravação parcial?**
Porque o único passo que grava (`armazenar`) vem **depois** de `validar` e `verificarPreRequisitos`, e a ordem não pode
ser mudada por subclasse. `R07_cpfNaoCadastradoFalhaAntesDeGravar` mostra que, com CPF sem pessoa, os passos
executados param em `verificarPreRequisitos` e nada foi gravado.

## Perguntas prováveis além do PDF
- **Estudante duplicado?** `buscarExistente` acha a pessoa pelo CPF e o estudante dessa pessoa → devolve o existente
  com `novo = false`, sem validar nem gravar (`R07_estudanteDuplicadoRetornaExistente`).
- **Por que `aposArmazenar` recebe os dados?** O cadastro de livro precisa do grupo informado na entrada.
- **Os nomes `iLivro`/`iPessoa`... foram mantidos?** Sim; os tratadores realizam essas interfaces e delegam a
  inclusão ao cadastro. Devolvem fichas imutáveis (`FichaDoLivro`...), não as entidades.
