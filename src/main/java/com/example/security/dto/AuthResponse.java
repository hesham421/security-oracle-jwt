package com.example.security.dto;

public record AuthResponse(String accessToken, long expiresIn) {}
