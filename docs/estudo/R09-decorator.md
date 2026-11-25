# R9 — Decorator: guia de estudo para a arguição

Código: `src/main/java/br/pucpr/biblioteca/biblioteca/acervo/` (`ConsultaAcervo`, `ConsultaAcervoPadrao`,
`DecoradorDeConsulta`, `ConsultaComMedicao`, `ConsultaComCache`, `Medidor`, `MetadadosLivro`) · Montagem:
`config/Aplicacao.montarConsultaAcervo` (`consulta.medicao`, `consulta.cache` no `application.properties`) · Uso:
`TratadorDeAcervo.consultarMetadados/buscarPorTitulo` (`iLivro`) · Testes: `src/test/java/.../acervo/R09DecoratorTest.java`.

## A ideia em uma frase
Recursos opcionais (medição, cache) são **embrulhos** em volta da consulta original: cada um tem o mesmo tipo
(`ConsultaAcervo`), faz a sua parte e repassa para o de dentro — então dá para ligar, desligar e combinar sem mexer
na consulta original.

## Papéis
| Papel GoF | Classe |
|---|---|
| Component | `ConsultaAcervo` (`consultarMetadados(id)`, `buscarPorTitulo(trecho)`, `livroAlterado(id)`) |
| ConcreteComponent | `ConsultaAcervoPadrao` — a consulta original, sem medição nem cache |
| Decorator | `DecoradorDeConsulta` — abstrato, guarda a `ConsultaAcervo interna` e repassa tudo |
| ConcreteDecorator | `ConsultaComMedicao` (chamadas e tempo no `Medidor`), `ConsultaComCache` (só `MetadadosLivro`) |

## Perguntas do PDF
**Como a cadeia é montada e por que a ordem importa?**
No composition root (`Aplicacao.montarConsultaAcervo`): começa com `new ConsultaAcervoPadrao(acervo)`; se
`consulta.cache=true`, embrulha em `ConsultaComCache`; se `consulta.medicao=true`, embrulha em `ConsultaComMedicao`.
Resultado: **Medição(Cache(Padrão))**. A medição fica **por fora** para medir toda consulta, inclusive as que o cache
responde (`R09_segundaConsultaUsaCacheEGeraMedicao`). Se a ordem fosse invertida, **Cache(Medição(Padrão))**, um acerto
do cache voltaria antes de chegar à medição e não seria medido
(`R09_ordemImporta_comMedicaoPorDentroOAcertoDoCacheNaoEhMedido`). A montagem por configuração é conferida em
`R09_cadeiaMontadaPelaConfiguracaoDaInstalacao`.

**O que acontece se o cache falhar?**
A consulta não falha. `ConsultaComCache` envolve a leitura e a gravação no armazenamento em `try/catch`: se der erro,
registra um aviso no log e responde pela consulta interna, como se não houvesse cache
(`R09_cacheQueFalhaNaoDerrubaAConsulta`, com um armazenamento que sempre lança exceção). A medição continua
funcionando.

**Por que o cache não participa da decisão de reserva?**
Porque ele guarda só `MetadadosLivro` (id e título) — **não há estado do livro** para ficar antigo
(`R09_metadadosNaoGuardamOEstadoDoLivro`). A reserva passa pela fachada, que lê o `Livro` atual do repositório e deixa
o estado decidir (R1). No teste, o livro é consultado (vai para o cache) enquanto está LIVRE, é reservado pela Ana, o
cache continua respondendo os metadados, e a reserva da Carla é recusada (`R09_cacheAntigoNaoAutorizaReserva`).

## Perguntas prováveis além do PDF
- **Por que não herança (`ConsultaComCacheEMedicao extends ...`)?** Teria que existir uma subclasse por combinação
  (cache, medição, cache+medição, ...). Com decoradores, cada recurso é uma classe e as combinações saem da montagem.
- **Como criar um novo recurso (ex.: log de consultas)?** Uma classe nova `extends DecoradorDeConsulta` que sobrescreve
  o que precisa e chama `super...` para repassar; mais uma linha em `montarConsultaAcervo`. Nada existente muda.
- **Quando o cache é limpo?** Sem expiração (C-10). Ao alterar o título ou excluir o livro, o `TratadorDeAcervo` chama
  `consulta.livroAlterado(id)`; o aviso atravessa a cadeia e o cache descarta o livro
  (`R09_alterarTituloOuExcluirTiraOLivroDoCache`). Mudança de estado não precisa invalidar: estado não está no cache.
- **`consultarLivro` também usa o cache?** Não. `iLivro.consultarLivro` devolve a ficha com o estado atual, lida do
  repositório. Os metadados (`consultarMetadados`, `buscarPorTitulo`) é que passam pela cadeia.
- **A busca por título é guardada no cache?** Não: só a consulta por id. A busca é repassada (e medida —
  `R09_buscaPorTituloPassaPelaCadeiaEEhMedida`).
- **Onde ver as medições?** `Aplicacao.medidorDaConsulta().resumo()` (chamadas e tempo total por operação) — a CLI
  vai mostrar isso no menu de administração.
