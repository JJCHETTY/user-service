package com.user.resource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.build.Jwt;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@QuarkusTestResource(TestJwtKeysResource.class)
class UserResourceTest {
    @Test
    void testListUsersRequiresAuthentication() {
        given()
          .redirects().follow(false)
          .when().get("/api/v1/users")
          .then()
             .statusCode(401);
    }

    @Test
    void oidcCallbackPathIsProtectedAndNotAnUnmatchedRoute() {
        given()
                .redirects().follow(false)
                .when().get("/api/v1/auth/callback")
                .then()
                .statusCode(401);
    }

    @Test
    void currentUserReadsIdFromTheSignedApplicationToken() {
        String token = Jwt.issuer("https://recipe-app.com/issuer")
                .subject("test@example.com")
                .upn("test@example.com")
                .groups("USER")
                .claim("userId", 999999L)
                .claim("role", "USER")
                .sign();

        given()
                .header("Authorization", "Bearer " + token)
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(404);
    }

    @Test
    void currentUserRejectsMissingAndOpaqueProviderTokens() {
        given()
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(401);

        given()
                .header("Authorization", "Bearer opaque-provider-token")
                .when().get("/api/v1/users/me")
                .then()
                .statusCode(401);
    }

}