package io.h8.config.yaml;

import java.net.URL;
import java.util.Locale;

final class Util {

    private Util() {}

    static boolean isYaml(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }

    static String urlPath(URL url) {
        String path = url.getPath();
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }
}
