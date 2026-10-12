package com.user.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.user.entity.UserEntity;
import com.user.entity.UserRole;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(TestJwtKeysResource.class)
class AuthResourceTest {
    private static final String APPLICATION_ISSUER = AppTokenIssuer.ISSUER;

    @Inject
    AppTokenExchangeStore tokenExchangeStore;

    @Inject
    AppTokenIssuer appTokenIssuer;

    @Inject
    JWTParser jwtParser;

    @Test
    void validatesOnlySignedApplicationTokensWithUserAndRoleClaims() throws Exception {
        UserEntity user = new UserEntity();
        user.id = 1L;
        user.email = "test@example.com";
        user.role = UserRole.USER;
        String validToken = appTokenIssuer.issue(user);
        var parsed = jwtParser.parse(validToken);
        assertEquals(APPLICATION_ISSUER, parsed.getIssuer());
        assertEquals(1L, ((JsonNumber) parsed.getClaim("userId")).longValueExact());
        assertEquals("USER", parsed.getClaim("role"));
        assertTrue(parsed.getGroups().contains("USER"));

        given()
                .header("Authorization", "Bearer " + validToken)
                .when().get("/api/v1/auth/validate")
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + appToken(APPLICATION_ISSUER, false, true))
                .when().get("/api/v1/auth/validate")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", "Bearer " + appToken(APPLICATION_ISSUER, true, false))
                .when().get("/api/v1/auth/validate")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", "Bearer " + appToken("https://accounts.google.com", true, true))
                .when().get("/api/v1/auth/validate")
                .then()
                .statusCode(401);
    }

    @Test
    void exchangesShortLivedCodeOnlyOnce() {
        String code = tokenExchangeStore.issue("opaque-test-app-token");

        given()
                .contentType("application/json")
                .body(new TokenExchangeRequest(code))
                .when().post("/api/v1/auth/exchange")
                .then()
                .statusCode(200)
                .body("appToken", equalTo("opaque-test-app-token"));

        given()
                .contentType("application/json")
                .body(new TokenExchangeRequest(code))
                .when().post("/api/v1/auth/exchange")
                .then()
                .statusCode(401);
    }

    @Test
    void rejectsOpaqueProviderAccessTokens() {
        given()
                .header("Authorization", "Bearer opaque-provider-access-token")
                .when().get("/api/v1/auth/validate")
                .then()
                .statusCode(401);
    }

    private String appToken(String issuer, boolean includeUserId, boolean includeRole) {
        var builder = Jwt.issuer(issuer).subject("test@example.com").groups("USER");
        if (includeUserId) {
            builder.claim("userId", 1L);
        }
        if (includeRole) {
            builder.claim("role", "USER");
        }
        return builder.sign();
    }
}
