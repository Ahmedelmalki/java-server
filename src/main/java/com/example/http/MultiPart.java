package com.example.http;

import java.util.Map;

public class MultiPart {
    private final String name;
    private final String filename;
    private final Map<String, String> headers;
    private final byte[] data;

    public MultiPart(String name, String filename, Map<String, String> headers, byte[] data) {
        this.name = name;
        this.filename = filename;
        this.headers = headers;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isFile() {
        return filename != null && !filename.isEmpty();
    }

    public String getAsString() {
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return String.format("MultiPart{name='%s', filename='%s', size=%d bytes}", 
            name, filename, data.length);
    }
}