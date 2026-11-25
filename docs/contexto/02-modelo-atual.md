# 02 — Modelo UML atual (extraído dos .asta)

Extraído em 24/11/2026 com `tools/astah/extrair.sh` (API do Astah). PNGs do estado original em
`docs/diagramas/png-original/`. Este arquivo descreve **o que existe hoje**, não o alvo — os problemas estão em
`03-revisao-uml.md` e o alvo em `04-arquitetura-alvo.md`.

| Arquivo | Diagramas |
|---|---|
| `DiagramaDeCasosDeUso03.asta` | UseCase Diagram0 |
| `DiagramaDeDominio8.asta` | Class Diagram0 (mesmas classes do diagrama de classes, sem compartimentos) |
| `DiagramaDeClasses19.asta` | Class Diagram0 (pacote `Domain`) |
| `DiagramaDeEstados05.asta` | Statemachine Diagram0 (Livro) |
| `DiagramaDeAtividades05.asta` | AtividadesNaBiblioteca, Cadastros |
| `DiagramaDeSequencia19.asta` | Consultar/Emprestar/Renovar/Reservar livro; Registrar devolucao; Emitir boleto de multa; Manter Livro, Pessoa, Estudante e Atendente |
| `DiagramaDeComponentes03.asta` | Component Diagram0 |
| `DiagramaDeImplantacao02.asta` | Deployment Diagram0 |

## Atores e casos de uso
- **Estudante**: Renovar empréstimo, Consultar empréstimo, Registrar reserva, Consultar reserva de livros, Efetivar empréstimo.
- **Atendente**: Registrar devolução, Emitir multa (`<<extend>>` Registrar devolução), Consultar reserva de livros.
- **Administrador**: CRUD de Livro (Adicionar/Atualizar/Excluir + Consultar Acervo), CRUD de Pessoa
  (+ Consultar Pessoas cadastradas), CRUD de Estudante e de Atendente.

## Diagrama de classes (pacote Domain)

### Pessoas e papéis
- `Pessoa` — `id:int, nome, sobrenome, cpf:String, dataCriacao:LocalDateTime`. Pessoa **1 —Tem→ 0..\*** Papel.
- `Papel` {abstract} — `id:int`, `getPessoa():Pessoa`. Subclasses: **`Administrador`, `Estudante`, `Atendente`**.

### Pontos internos de acesso (Trabalho 02)
- **`Relacionamento`** — `id`; CRUD de Pessoa, Estudante, Atendente:
  `inserirPessoa(Pessoa):Pessoa`, `inserirEstudante(Pessoa):Estudante`, `inserirAtendente(Pessoa):Atendente`,
  `atualizar*`, `consultar*(id)`, `excluir*`. Agrega Pessoa, Estudante, Atendente ("contém").
- **`Acervo`** — `id`; `inserirLivro(Livro):Livro`, `atualizarLivro(Livro)`, `consultarLivro(id):Livro`,
  `excluirLivro(Livro)`. Realiza `iLivro`. Agrega `Livro` (0..\*).
- **`Movimentacao`** — `id`; `inserirTransacao(Transacao)`, `removerTransacao(Transacao)`,
  `consultarReservaLivro(idLivro,idEstudante):Reserva`, `consultarEmprestimo(...):Emprestimo`,
  `consultarDevolucao(...):Devolucao`. Agrega Reserva, Emprestimo, Devolucao.

### Livro e transações
- `Livro` — `id:int, titulo:String, estado:EstadoLivro`; `getAcervo()`, `setEstado(EstadoLivro)`, `getEstado()`,
  `inspecionarLivro(emBomEstado)`, `retornarDoReparo()`, `enviarParaReparo()`. Livro **1 —movimentado em→ 0..\*** Movimentacao.
- `<<enumeration>> EstadoLivro` — LIVRE, RESERVADO, EMPRESTADO, RENOVADO, DEVOLVIDO, REPARANDO, INCINERADO
  (modelados como `int`). **Não há classes de estado (State) no modelo atual.**
- `Transacao` {abstract} — `id:int, dataTransacao:LocalDateTime`. Subclasses: `Reserva`, `Emprestimo`, `Devolucao`.
- `Emprestimo` — `dataVencimento:LocalDateTime, numRenovacoes:int`; `diasDeAtraso(dataDevolucao):int`,
  `renovar()`, `estaAtrasado(dataDevolucao):boolean`.
- `Devolucao` — `dataVencimento:LocalDateTime`; Devolucao **1 —tem→ 0..1** Multa.
- `Multa` — `id:int, valor:float, estado:EstadoMulta`; `quitar()`, `calcularValor(dataVencimento, dataTransacao):float`.
- `<<enumeration>> EstadoMulta` — EMITIDA, QUITADA.

### Tratadores e interfaces
| Tratador | Realiza | Delega para | Operações |
|---|---|---|---|
| `TratadorDeEmprestimo` | `iEmprestimo` | Movimentacao, Acervo, Relacionamento; `<<create>>` Reserva/Emprestimo; `<<use>>` Livro | `consultarReservaLivro(idLivro,idEstudante):Reserva`, `solicitarReserva(idLivro,idEstudante)`, `realizarEmprestimo(idLivro,idEstudante,idAtendente)`, `renovarEmprestimo(idLivro,idEstudante)` |
| `TratadorDeDevolucao` | `iDevolucao` | Movimentacao, Acervo, Relacionamento, Livro; `<<create>>` Devolucao/Multa | `registrarDevolucao(idLivro,idEstudante,idAtendente)`, `emitirBoletoDeMulta(idLivro,idEstudante):Multa`, `inspecionarLivro(idLivro,emBomEstado)`, `retornarDoReparo(idLivro)`, `enviarParaReparo(idLivro)` |
| `TratadorDeAcervo` | `iLivro` | Acervo | `inserirLivro`, `atualizarLivro`, `consultarLivro`, `excluirLivro` |
| `TratadorDePessoa` | `iPessoa` | Relacionamento | `inserirPessoa`, `atualizarPessoa`, `consultarPessoa`, `excluirPessoa` |
| `TratadorDeEstudante` | `iEstudante` | Relacionamento | `inserirEstudante(Pessoa)`, `atualizarEstudante`, `consultarEstudante`, `excluirEstudante` |
| `TratadorDeAtendente` | `iAtendente` | Relacionamento | `inserirAtendente(Pessoa)`, `atualizarAtendente`, `consultarAtendente`, `excluirAtendente` |

Atores-classe usam os tratadores: Administrador → TratadorDeAcervo/Pessoa/Estudante/Atendente;
Estudante → TratadorDeEmprestimo; Atendente → TratadorDeDevolucao.

**Elementos "fantasmas" no modelo** (existem no .asta mas não aparecem no diagrama): interface `CRUD`, `Exemplar`,
`GrupoDeEstudantes`, `GrupoDeAcademicos`, `GrupoDeColaboradores`, `GrupoDePessoas`, `Class4`, classes minúsculas
(`livro`, `pessoa`, `adminsitrador`…), generalizações `Papel → Pessoa`, `Reserva/Emprestimo/Devolucao → Movimentacao`,
e dezenas de associações sem nome. Ver `03-revisao-uml.md`.

## Máquina de estados de Livro (atual)
```
[*] --> Livre                       (nota "inserirLivro()" presa à transição inicial)
Livre --> Reservado      : solicitarReserva() [estudante cadastrado]
Reservado --> Emprestado : realizarEmprestimo() [reserva registrada para o mesmo estudante]
Emprestado --> Renovado  : renovarEmprestimo() [numRenovacoes = 0]
Renovado --> Renovado    : renovarEmprestimo() [numRenovacoes < 2]       Renovado: entry / numRenovacoes++
Emprestado --> Choice1   : registrarDevolucao()
Renovado --> Choice1     : registrarDevolucao()
Choice1 --> Devolvido    : [emprestimo.estaAtrasado(dataDevolucao)] / emitirBoletoDeMulta
Choice1 --> Devolvido    : [not emprestimo.estaAtrasado(dataDevolucao)]
Devolvido --> Livre      : inspecionarLivro() [emBomEstado = true]
Devolvido --> Reparando  : inspecionarLivro() [emBomEstado = false]
Livre --> Reparando      : enviarParaReparo() [dano identificado no acervo]
Reparando --> Livre      : retornarDoReparo()
Reparando --> Incinerado : [reparo inviável]                 (sem evento!)
Incinerado --> [*]
Livre --> [*]            : excluirLivro()
```

## Sequências (atual, resumo)
- **Reservar** (TratadorDeEmprestimo): `Acervo.consultarLivro` → `livro.getEstado()` → `Relacionamento.consultarEstudante`
  → `create Reserva` → `livro.setEstado(RESERVADO)` → `Movimentacao.inserirTransacao(reserva)`.
- **Emprestar**: `Movimentacao.consultarReservaLivro` → `Acervo.consultarLivro` → `livro.setEstado(EMPRESTADO)` →
  `removerTransacao(reserva)` → `create Emprestimo` → `inserirTransacao(emprestimo)`.
- **Renovar**: `consultarEmprestimo` → `consultarLivro` → `getEstado` → `alt [numRenovacoes<2]` → `setEstado(RENOVADO)` → `emprestimo.renovar()` (duplicado: msgs 4.5 e 4.6).
- **Registrar devolução** (TratadorDeDevolucao): `consultarEmprestimo` → `create Devolucao` → `inserirTransacao` →
  `estaAtrasado` → `emitirBoletoDeMulta` (self) → `Acervo.inspecionarLivro` (→ setEstado DEVOLVIDO → inspecionarLivro →
  enviarParaReparo/ setEstado LIVRE) → `removerTransacao(emprestimo)` → `delete Emprestimo`.
- **Emitir boleto de multa**: `consultarEmprestimo` → `estaAtrasado` → `alt [True]` `diasDeAtraso` → `create Multa(dias)` → `calcularValor`.
- **Manter Livro/Pessoa/Estudante/Atendente**: 4 interações CRUD no mesmo diagrama; Tratador → `consultar*` → `alt [existe/não existe]` → Relacionamento/Acervo.

## Componentes (atual)
- **Interface Web** (portas `tratadorWeb`) requer iLivro, iEmprestimo, iDevolucao, iAtendente, iEstudante, iPessoa.
- **Biblioteca** (Livro, Acervo, Movimentacao, TratadorDeEmprestimo, TratadorDeAcervo, TratadorDeDevolucao):
  provê iLivro, iEmprestimo, iDevolucao; requer iMulta, iEstudante, iAtendente.
- **Acadêmico** (TratadorDeEstudante, Relacionamento): provê iEstudante; requer iPessoa.
- **RH** (Relacionamento, TratadorDePessoa, TratadorDeAtendente): provê iPessoa, iAtendente.
- **Financeiro** (Multa, TratadorDeDevolucao): provê iMulta, iQuitacao; requer iCobranca, iPessoa.
- **`<<external>>` Banco**: provê iCobranca; requer iQuitacao.

## Implantação (atual)
`pc1 : Computador do Usuário` [WebBrowser: index.html] —HTTPS (0..\*:1)— `srvApp : Servidor de Aplicação`
[Ubuntu ▸ JVM 1090 ▸ Tomcat 10.0.11: source.jar] —JDBC (dependência tracejada)→ `<<external>> srvMySQL : Dell Edge 3600`
[Windows 10 ▸ MySQL: Banco de dados principal] —replicação (1:1..\*)— `<<external>> srvOracle : Dell Edge 3600`
[Ubuntu ▸ Oracle: Banco de dados espelho].

## Atividades (atual)
- **AtividadesNaBiblioteca**: raias Acadêmico, Estudante, RH, Atendente, Biblioteca, Financeiro, Banco — um único fluxo
  contínuo reserva → empréstimo → renovação → devolução → reparo/incineração → multa → boleto → pagamento → quitação.
- **Cadastros**: raias Administrador, RH, Acadêmico, Biblioteca — fork em 4 ramos (Pessoa, Livro, Estudante, Atendente)
  com verificação de existência da Pessoa antes de Estudante/Atendente; join final.
