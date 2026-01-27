package com.example.core;

public enum ConnState {
    READING_HEADERS,
    PROCESSING,
    WRITING_RESPONSE,
    CLOSED
}
