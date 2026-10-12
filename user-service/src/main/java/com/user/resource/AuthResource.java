package com.user.resource;

import com.user.entity.UserEntity;
import com.user.entity.UserRole;
import com.user.repository.UserRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.Locale;

@Path("/api/v1/auth")
@Blocking
public class AuthResource {

    @ConfigProperty(name = "app.login-success-uri", defaultValue = "http://localhost:4200/login-success")
    URI loginSuccessUri;

    @Inject
    UserRepository userRepository;

    @Inject
    AppTokenExchangeStore tokenExchangeStore;

    @Inject
    AppTokenIssuer appTokenIssuer;

    @Inject
    AppTokenValidator appTokenValidator;

    @Inject
    Instance<UserInfo> userInfo;

    @GET
    @Path("/login")
    @Authenticated
    @Produces("text/plain")
    @Transactional
    public Response login() {
        return completeLogin();
    }

    @GET
    @Path("/google-login")
    @Authenticated
    @Produces("text/plain")
    @Transactional
    public Response googleLogin() {
        return completeLogin();
    }

    private Response completeLogin() {
        String email = userInfo.isResolvable() ? userInfo.get().getString("email") : null;
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account did not provide an email address");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> createUser(normalizedEmail));

        String token = appTokenIssuer.issue(user);
        URI redirectUri = UriBuilder.fromUri(loginSuccessUri)
                .fragment("token=" + token)
                .build();
        return Response.seeOther(redirectUri).build();
    }

    @GET
    @Path("/validate")
    @PermitAll
    public Response validateToken(@HeaderParam("Authorization") String authorization) {
        if (appTokenValidator.validateAuthorizationHeader(authorization) == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/exchange")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response exchangeToken(TokenExchangeRequest request) {
        if (request == null || request.code() == null || request.code().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return tokenExchangeStore.consume(request.code())
                .map(token -> Response.ok(new TokenExchangeResponse(token))
                        .header("Cache-Control", "no-store")
                        .header("Pragma", "no-cache")
                        .build())
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED).build());
    }

    private UserEntity createUser(String email) {
        UserEntity user = new UserEntity();
        user.email = email;
        user.username = email.substring(0, email.indexOf('@'));
        user.role = UserRole.USER;
        userRepository.persist(user);
        return user;
    }
}
