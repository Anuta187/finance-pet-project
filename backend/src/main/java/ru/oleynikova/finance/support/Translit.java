package ru.oleynikova.finance.support;

import java.util.Locale;
import java.util.Map;

/** Имя держателя карты латиницей. */
public final class Translit {

    private static final Map<Character, String> RU = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"), Map.entry('д', "d"),
            Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"), Map.entry('з', "z"), Map.entry('и', "i"),
            Map.entry('й', "i"), Map.entry('к', "k"), Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"),
            Map.entry('о', "o"), Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "kh"), Map.entry('ц', "ts"), Map.entry('ч', "ch"),
            Map.entry('ш', "sh"), Map.entry('щ', "shch"), Map.entry('ъ', "ie"), Map.entry('ы', "y"), Map.entry('ь', ""),
            Map.entry('э', "e"), Map.entry('ю', "iu"), Map.entry('я', "ia"));

    private Translit() {
    }

    public static String cardHolder(String fullName) {
        StringBuilder sb = new StringBuilder();
        for (char c : fullName.toLowerCase(Locale.ROOT).toCharArray()) {
            String t = RU.get(c);
            if (t != null) {
                sb.append(t);
            } else if ((c >= 'a' && c <= 'z') || c == ' ' || c == '-') {
                sb.append(c);
            }
        }
        String result = sb.toString().trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        return result.isEmpty() ? "CARD HOLDER" : result;
    }
}
