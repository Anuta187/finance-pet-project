package ru.oleynikova.finance.autotests.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Настройки берутся из -D свойств, потом из переменных TEST_*, потом из config.properties. */
public final class TestConfig {

    private static final Properties FILE = load();

    private TestConfig() {
    }

    public static String apiUrl() {
        return get("apiUrl");
    }

    public static String baseUrl() {
        return get("baseUrl");
    }

    public static String browser() {
        return get("browser");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public static String browserSize() {
        return get("browserSize");
    }

    public static long timeoutMs() {
        return Long.parseLong(get("timeoutMs"));
    }

    public static boolean resetDb() {
        return Boolean.parseBoolean(get("resetDb"));
    }

    static String get(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String env = System.getenv("TEST_" + key.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());
        if (env != null && !env.isBlank()) {
            return env;
        }
        String value = FILE.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Не задан параметр конфигурации: " + key);
        }
        return value.trim();
    }

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = TestConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }
}
