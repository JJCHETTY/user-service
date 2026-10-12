package com.user.resource;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import com.user.entity.UserEntity;

@ApplicationScoped
public class AppTokenIssuer {
    public static final String ISSUER = "https://recipe-app.com/issuer";

    public String issue(UserEntity user) {
        if (user.id == null || user.id <= 0 || user.email == null || user.email.isBlank() || user.role == null) {
            throw new IllegalArgumentException("A persisted user with an email and role is required");
        }

        String role = user.role.name();
        return Jwt.issuer(ISSUER)
                .subject(user.email)
                .upn(user.email)
                .groups(role)
                .claim("userId", user.id)
                .claim("role", role)
                .sign();
    }
}
