package com.example.hello;

/** Output payload for the hello Lambda example. */
public record HelloResponse(String message, String requestId) {}
