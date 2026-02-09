package com.example.parser;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ChunkedBodyReader implements BodyReader {
    
    private enum State {
        READING_SIZE,
        READING_DATA,
        READING_CRLF_AFTER_DATA,
        FINISHED
    }

    private State state = State.READING_SIZE;
    private int currentChunkSize = 0;
    private int bytesReadInChunk = 0;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
    
    private final long maxBodySize;
    private long totalBytesRead = 0;

    public ChunkedBodyReader(long maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    @Override
    public boolean process(ByteBuffer buffer) {
        if (state == State.FINISHED) return true;

        while (buffer.hasRemaining() || state == State.FINISHED) {
            switch (state) {
                case READING_SIZE:
                    if (readLine(buffer)) {
                        String line = new String(lineBuffer.toByteArray(), StandardCharsets.US_ASCII);
                        lineBuffer.reset();
                        try {
                            String hexSize = line.split(";")[0].trim();
                            if (hexSize.isEmpty()) continue;
                            currentChunkSize = Integer.parseInt(hexSize, 16);
                            bytesReadInChunk = 0;
                            
                            if (currentChunkSize == 0) {
                                state = State.READING_CRLF_AFTER_DATA;
                                return process(buffer);
                            } else {
                                state = State.READING_DATA;
                            }
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Invalid chunk size: " + line);
                        }
                    } else {
                        return false;
                    }
                    break;

                case READING_DATA:
                    int needed = currentChunkSize - bytesReadInChunk;
                    int available = buffer.remaining();
                    int toRead = Math.min(available, needed);

                    if (totalBytesRead + toRead > maxBodySize) {
                        throw new RuntimeException("Chunked body too large: > " + maxBodySize);
                    }

                    byte[] data = new byte[toRead];
                    buffer.get(data);
                    outputStream.write(data, 0, toRead);
                    
                    bytesReadInChunk += toRead;
                    totalBytesRead += toRead;

                    if (bytesReadInChunk == currentChunkSize) {
                        state = State.READING_CRLF_AFTER_DATA;
                    } else {
                        return false;
                    }
                    break;

                case READING_CRLF_AFTER_DATA:
                    if (readLine(buffer)) {
                        String line = new String(lineBuffer.toByteArray(), StandardCharsets.US_ASCII);
                        lineBuffer.reset();
                        
                        if (line.trim().isEmpty()) {
                            if (currentChunkSize == 0) {
                                state = State.FINISHED;
                                return true;
                            }
                            state = State.READING_SIZE;
                        } else {
                            if (currentChunkSize == 0) {
                                // Trailer headers - ignore and continue
                            } else {
                                throw new RuntimeException("Expected CRLF after chunk data");
                            }
                        }
                    } else {
                        return false;
                    }
                    break;

                case FINISHED:
                    return true;
            }

            if (state == State.FINISHED) return true;
        }

        return false;
    }
    
    private boolean readLine(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == '\n') {
                return true;
            }
            lineBuffer.write(b);
        }
        return false;
    }

    @Override
    public byte[] getBody() {
        return outputStream.toByteArray();
    }

    @Override
    public java.nio.file.Path getBodyFile() {
        return null;
    }

    @Override
    public void cleanup() {
        // Nothing to clean up for in-memory
    }
}