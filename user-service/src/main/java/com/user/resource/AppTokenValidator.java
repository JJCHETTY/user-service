package com.user.resource;

import java.util.Set;

import com.user.entity.UserRole;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;

@ApplicationScoped
public class AppTokenValidator {
    private static final Set<String> ALLOWED_ROLES = Set.of(UserRole.USER.name(), UserRole.ADMIN.name());

    @Inject
    JWTParser jwtParser;

    public JsonWebToken validateAuthorizationHeader(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return null;
        }

        try {
            JsonWebToken parsed = jwtParser.parse(token);
            return hasValidApplicationClaims(parsed) ? parsed : null;
        } catch (ParseException exception) {
            return null;
        }
    }

    public Long userId(JsonWebToken token) {
        Object claim = token.getClaim("userId");
        if (claim instanceof JsonNumber number) {
            try {
                long value = number.longValueExact();
                return value > 0 ? value : null;
            } catch (ArithmeticException ignored) {
                return null;
            }
        }
        if (claim instanceof Number number) {
            long value = number.longValue();
            return value > 0 && number.doubleValue() == value ? value : null;
        }
        if (claim instanceof String value) {
            try {
                long userId = Long.parseLong(value);
                return userId > 0 ? userId : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean hasValidApplicationClaims(JsonWebToken token) {
        Object role = token.getClaim("role");
        return AppTokenIssuer.ISSUER.equals(token.getIssuer())
                && userId(token) != null
                && role instanceof String roleName
                && ALLOWED_ROLES.contains(roleName)
                && token.getGroups() != null
                && token.getGroups().contains(roleName);
    }
}
