package fr.carrefour.bff.infrastructure.adapter.io.web;

public record CurrentUserResponse(
        String username,
        String email,
        String name,
        boolean isAdmin
) {
}
