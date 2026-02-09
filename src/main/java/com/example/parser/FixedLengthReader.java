package com.example.parser;

import java.nio.ByteBuffer;

public class FixedLengthReader implements BodyReader {
    private final long totalLength;
    private final byte[] body;
    private long bytesRead;

    public FixedLengthReader(long length) {
        this.totalLength = length;
        this.bytesRead = 0;
        this.body = new byte[(int) length];
    }

    @Override
    public boolean process(ByteBuffer buffer) {
        if (totalLength == 0) return true;
        if (!buffer.hasRemaining()) return false;

        long remainingNeeded = totalLength - bytesRead;
        int toRead = (int) Math.min(buffer.remaining(), remainingNeeded);

        buffer.get(body, (int) bytesRead, toRead);
        bytesRead += toRead;

        return bytesRead == totalLength;
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public java.nio.file.Path getBodyFile() {
        return null;
    }

    @Override
    public void cleanup() {
        // Nothing to clean up
    }
}