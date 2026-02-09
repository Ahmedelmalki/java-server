package com.example.parser;

import java.nio.ByteBuffer;
import java.nio.file.Path;

public interface BodyReader {
    boolean process(ByteBuffer buffer);
    byte[] getBody();
    Path getBodyFile();
    void cleanup();
}