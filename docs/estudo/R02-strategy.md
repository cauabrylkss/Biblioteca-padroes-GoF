# R2 — Strategy: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/financeiro/` · Testes: `src/test/java/.../financeiro/R02StrategyTest.java`

## A ideia em uma frase
Cada regra de cálculo de multa é uma **classe intercambiável** com o mesmo método `calcular(dias)`; o serviço de
multas usa a que estiver **vigente** sem saber qual é.

## Papéis
| Papel GoF | Classe | O que faz |
|---|---|---|
| Strategy | `PoliticaDeMulta` | Interface: `calcular(diasDeAtraso)`, `codigo()`, `descricao()` |
| ConcreteStrategy | `PoliticaOriginal` | dias × R$ 1,00 |
| ConcreteStrategy | `PoliticaComTeto` | mín(dias × R$ 1,00; R$ 20,00) |
| ConcreteStrategy | `PoliticaComTolerancia` | máx(0; dias − 2) × R$ 1,00 |
| Context | `ServicoDeMultas` | Calcula os dias, pega a política vigente, cria a `Multa` |
| (configuração) | `ConfiguracaoAdministrativa` | Guarda as políticas disponíveis e qual está vigente; o admin troca pela CLI |

## Perguntas do PDF
**Como uma nova política é incluída sem alterar o serviço de multas?**
Escreve-se uma classe nova que implementa `PoliticaDeMulta` e ela é registrada na `ConfiguracaoAdministrativa`
(no composition root, na lista de políticas, ou com `registrar(...)`). O `ServicoDeMultas` só conhece a interface e
chama `configuracao.politicaVigente().calcular(dias)` — não tem `if`/`switch` por política. O teste
`R02_novaPoliticaEntraSemAlterarOServicoDeMultas` faz exatamente isso com uma política "dobro após 10 dias".

**Por que uma troca de política não recalcula multas já emitidas?**
Porque a `Multa` grava, no momento da emissão, o **valor** e o **código da política** (`codigoPolitica`), e esses
campos são `final`. Trocar a vigente só muda a referência guardada na configuração, usada nas próximas emissões.
Além disso, recalcular mudaria uma cobrança que já pode ter virado boleto no banco. Teste:
`R02_multaGravaValorEPoliticaDaEmissaoETrocaDePoliticaNaoRecalcula`.

**Como os dias de atraso e os valores monetários são calculados?**
- Dias: `DiasDeAtraso.entre(vencimento, dataDevolucao)` — dias **corridos** entre as duas datas (`ChronoUnit.DAYS`),
  nunca negativo; devolver no dia do vencimento = 0. É o único lugar do sistema que faz essa conta (o `Emprestimo`
  também usa).
- Valores: `BigDecimal` com 2 casas (`R$ 1,00` = `new BigDecimal("1.00")`), arredondamento `HALF_EVEN`. Nunca
  `double`: `0.1 + 0.2` em `double` dá `0.30000000000000004`.

## Perguntas prováveis além do PDF
- **Por que o serviço lê `politicaVigente()` uma vez só por emissão?** Para que o valor e o código gravados venham da
  mesma política, mesmo que o admin troque a política no meio da operação.
- **O que acontece com 1 ou 2 dias de atraso na política com tolerância?** O valor é zero, então
  `emitirSeHouverAtraso` devolve vazio: não existe multa de R$ 0,00.
- **Qual a diferença para um `switch (politica)` dentro do serviço?** Com `switch`, toda política nova obrigaria a
  editar e retestar o serviço (viola aberto/fechado); com Strategy, o serviço fica fechado e o sistema aberto a
  novas políticas.
- **Alteração ao vivo provável:** "crie uma política X". Roteiro: nova classe em `financeiro` implementando
  `PoliticaDeMulta` → adicioná-la à lista de políticas no composition root → `-Dmulta.politica=<codigo>` ou trocar
  pela CLI.
