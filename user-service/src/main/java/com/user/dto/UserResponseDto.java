package com.user.dto;

import com.user.entity.UserEntity;
import com.user.entity.UserRole;

import java.time.Instant;

public record UserResponseDto(
        Long id,
        String email,
        String username,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponseDto from(UserEntity user) {
        return new UserResponseDto(
                user.id,
                user.email,
                user.username,
                user.role,
                user.createdAt,
                user.updatedAt
        );
    }
}
