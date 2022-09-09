package org.apache.coyote.http11.response;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Cookie {

    public static final String JSESSIONID = "JSESSIONID";
    private static final String COOKIE_PARAMETER_DELIMITER = "=";
    private static final String COOKIE_CONNECTOR = "; ";

    private final Map<String, String> cookies = new HashMap<>();


    public static Cookie ofJSessionId(String id) {
        Cookie cookie = new Cookie();
        cookie.addCookie(JSESSIONID, id);
        return cookie;
    }

    public void addCookie(String key, String value) {
        cookies.put(key, value);
    }

    public String parseToString() {
        return cookies.entrySet()
                .stream()
                .map(entry -> entry.getKey() + COOKIE_PARAMETER_DELIMITER + entry.getValue())
                .collect(Collectors.joining(COOKIE_CONNECTOR));
    }
}
