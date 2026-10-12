package com.user.resource;

import com.user.dto.UserResponseDto;
import com.user.repository.UserRepository;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class UserResource {
    @Inject
    UserRepository userRepository;

    @Inject
    AppTokenValidator appTokenValidator;

    @GET
    @RolesAllowed("ADMIN")
    public List<UserResponseDto> listUsers() {
        return userRepository.listAll().stream()
                .map(UserResponseDto::from)
                .toList();
    }

    @GET
    @Path("/me")
    @PermitAll
    public Response getCurrentUser(@HeaderParam("Authorization") String authorization) {
        JsonWebToken token = appTokenValidator.validateAuthorizationHeader(authorization);
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Long userId = appTokenValidator.userId(token);
        return userRepository.findByIdOptional(userId)
                .map(UserResponseDto::from)
                .map(dto -> Response.ok(dto).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

}
