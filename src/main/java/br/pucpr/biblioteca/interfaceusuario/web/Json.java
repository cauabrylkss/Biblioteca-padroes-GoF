package br.pucpr.biblioteca.interfaceusuario.web;

import br.pucpr.biblioteca.interfaceusuario.Mensagens;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Converte em JSON o que os tratadores devolvem (records, listas, {@code Optional}, datas, {@code BigDecimal},
 * enums), sem biblioteca externa. Os campos de um record viram as chaves do objeto, na ordem em que foram declarados.
 *
 * <p>Datas saem como {@code "dd/MM/yyyy"} e dinheiro como número exato ({@code BigDecimal.toPlainString()}, nunca
 * {@code double}): esta API existe só para a página da biblioteca.
 */
final class Json {

    private Json() {
    }

    static String escrever(Object valor) {
        StringBuilder json = new StringBuilder();
        escrever(valor, json);
        return json.toString();
    }

    private static void escrever(Object valor, StringBuilder json) {
        switch (valor) {
            case null -> json.append("null");
            case Optional<?> opcional -> escrever(opcional.orElse(null), json);
            case Boolean b -> json.append(b);
            case BigDecimal dinheiro -> json.append(dinheiro.toPlainString());
            case Number numero -> json.append(numero);
            case LocalDate data -> texto(Mensagens.data(data), json);
            case Enum<?> constante -> texto(constante.name(), json);
            case CharSequence texto -> texto(texto.toString(), json);
            case Record registro -> objeto(registro, json);
            case Map<?, ?> mapa -> mapa(mapa, json);
            case Iterable<?> itens -> lista(itens, json);
            default -> texto(valor.toString(), json);
        }
    }

    private static void objeto(Record registro, StringBuilder json) {
        json.append('{');
        RecordComponent[] campos = registro.getClass().getRecordComponents();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                json.append(',');
            }
            texto(campos[i].getName(), json);
            json.append(':');
            escrever(ler(campos[i], registro), json);
        }
        json.append('}');
    }

    private static Object ler(RecordComponent campo, Record registro) {
        try {
            campo.getAccessor().trySetAccessible();       // records aninhados em interfaces/classes de outros pacotes
            return campo.getAccessor().invoke(registro);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Não foi possível ler " + campo.getName(), e);
        }
    }

    private static void mapa(Map<?, ?> mapa, StringBuilder json) {
        json.append('{');
        boolean primeiro = true;
        for (Map.Entry<?, ?> par : mapa.entrySet()) {
            if (!primeiro) {
                json.append(',');
            }
            primeiro = false;
            texto(String.valueOf(par.getKey()), json);
            json.append(':');
            escrever(par.getValue(), json);
        }
        json.append('}');
    }

    private static void lista(Iterable<?> itens, StringBuilder json) {
        json.append('[');
        boolean primeiro = true;
        for (Object item : itens) {
            if (!primeiro) {
                json.append(',');
            }
            primeiro = false;
            escrever(item, json);
        }
        json.append(']');
    }

    /** Texto entre aspas, com os caracteres especiais escapados (acentos seguem em UTF-8). */
    private static void texto(String texto, StringBuilder json) {
        json.append('"');
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (c < 0x20) {
                        json.append(String.format("\\u%04x", (int) c));
                    } else {
                        json.append(c);
                    }
                }
            }
        }
        json.append('"');
    }
}
