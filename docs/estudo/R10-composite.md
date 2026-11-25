# R10 — Composite: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/biblioteca/acervo/` (`NoDoAcervo`, `GrupoDoAcervo`, `ItemDoAcervo`,
`OrganizacaoDoAcervo`, `TipoGrupo`) · Testes: `src/test/java/.../acervo/R10CompositeTest.java`

## A ideia em uma frase
Unidades, seções e subseções formam uma **árvore**; grupos e livros são tratados pela **mesma interface**, então
"quantos livros tem aqui?" é a mesma pergunta para uma unidade inteira ou para um único item.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Component | `NoDoAcervo` | `contarLivros()`, `listarLivros()`, `localizarItem(idLivro)`, `descricao()`; conhece o pai |
| Composite | `GrupoDoAcervo` | Guarda filhos; repassa as operações a eles; valida `adicionar` |
| Leaf | `ItemDoAcervo` | Referência a um livro **pelo id**; conta 1 e lista a si mesmo |
| (gerência) | `OrganizacaoDoAcervo` | Raiz "Acervo" e operações de alto nível: criar grupo, catalogar, mover, retirar |

```
Acervo (raiz)
└── UNIDADE Biblioteca Central        contarLivros() = 3 + 2 = 5
    ├── SECAO Exatas                  contarLivros() = 1 + 1 + 1 = 3
    │   ├── Livro 1   Livro 2   Livro 3
    └── SECAO Humanas                 contarLivros() = 1 + 1 = 2
        ├── Livro 4   Livro 5
```

## Perguntas do PDF
**Como a contagem percorre a árvore?**
`GrupoDoAcervo.contarLivros()` soma `filho.contarLivros()` de cada filho. Se o filho é outro grupo, ele faz o mesmo
com os filhos dele (recursão); se é um `ItemDoAcervo`, devolve 1 e a recursão para ali. O grupo não pergunta o tipo
do filho — por isso funciona com qualquer profundidade (`R10_arvoreDeProfundidadeVariavel`).

**Quais regras impedem ciclos e livros em dois grupos?**
Ambas estão em `GrupoDoAcervo.adicionar`:
- **ciclo**: `filho.ehIgualOuAncestralDe(this)` sobe do grupo de destino até a raiz; se encontrar o nó que está
  entrando, recusa (um grupo não entra em si mesmo nem num descendente);
- **livro em dois grupos**: junta os livros que já estão na árvore (`raiz().listarLivros()`) e recusa se algum livro
  do nó que está entrando já estiver lá, dizendo em qual grupo ele está;
- além disso, um nó que já tem pai não é adicionado em outro lugar — para mudar de lugar existe `mover`.

**Por que mover um item não toca no Livro nem na Movimentacao?**
Porque a árvore **não tem referência** a eles: `ItemDoAcervo` guarda só o `idLivro` (um `long`). `mover` tira o
mesmo item de um grupo e o coloca no outro; não há como mudar estado, empréstimo ou reserva a partir daí. O teste
`R10_moverLivroEntreSecoesNaoAlteraIdEstadoNemEmprestimo` usa um livro EMPRESTADO de verdade e confere que id,
situação, vencimento e renovações ficam iguais.

## Perguntas prováveis além do PDF
- **Onde está o "tratar igual"?** Em `R10_grupoEItemRespondemAsMesmasOperacoes`: uma lista de `NoDoAcervo` com uma
  seção e um item somados no mesmo laço.
- **E se o destino do `mover` recusar?** `OrganizacaoDoAcervo.mover` devolve o item à origem antes de repassar o erro
  (`R10_moverParaDestinoQueRecusaMantemOLivroNoLugar`).
- **Por que não há um índice `idLivro → grupo`?** Para a regra ficar no próprio grupo, como pede o enunciado
  ("o grupo recusa…"). O custo de percorrer a árvore é irrelevante para o tamanho de uma biblioteca.
- **A árvore vai para o MySQL?** Não: fica em memória (decisão C-08); o R8 só exige repositórios de Livro,
  Movimentacao e Relacionamento.
- **Para que serve `TipoGrupo`?** Só para exibição; a profundidade é livre (subseção dentro de subseção é aceita).
