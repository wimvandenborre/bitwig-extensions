package dev.gregross.gig.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class BearerAuth {

    private static final String PREFIX = "Bearer ";

    private BearerAuth() {}

    static boolean matches(String authorizationHeader, String token) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return false;
        }
        byte[] supplied = authorizationHeader.substring(PREFIX.length())
            .getBytes(StandardCharsets.UTF_8);
        byte[] expected = token.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(supplied, expected);
    }
}
