package ru.oleynikova.finance.autotests.data;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class TestData {

    private TestData() {
    }

    private static String suffix() {
        return Long.toString(System.nanoTime(), 36) + ThreadLocalRandom.current().nextInt(1000);
    }

    public static String uniqueName(String prefix) {
        String name = prefix + " " + suffix();
        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    public static String uniqueName() {
        return uniqueName("Счёт");
    }

    public static String uniqueEmail() {
        return ("user." + suffix() + "@test.local").toLowerCase(Locale.ROOT);
    }
}
