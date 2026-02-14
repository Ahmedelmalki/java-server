package com.example.parser;

import com.example.http.MultiPart;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MultipartParser {

    private final String boundary;

    public MultipartParser(String boundary) {
        this.boundary = boundary;
    }

    public List<MultiPart> parse(byte[] body) {
        List<MultiPart> parts = new ArrayList<>();
        String boundaryMarker = "--" + boundary;
        byte[] boundaryBytes = boundaryMarker.getBytes(StandardCharsets.US_ASCII);

        List<Integer> boundaryPositions = new ArrayList<>();

        // Find all boundary positions
        for (int i = 0; i < body.length - boundaryBytes.length + 1; i++) {
            boolean match = true;
            for (int j = 0; j < boundaryBytes.length; j++) {
                if (body[i + j] != boundaryBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                boundaryPositions.add(i);
                i += boundaryBytes.length - 1;
            }
        }

        if (boundaryPositions.size() < 2) {
            return parts; // No valid multipart data
        }

        // Process each part between boundaries
        for (int i = 0; i < boundaryPositions.size() - 1; i++) {
            int start = boundaryPositions.get(i) + boundaryBytes.length;
            int end = boundaryPositions.get(i + 1);

            // Check for CRLF after boundary
            if (start + 2 <= body.length && body[start] == '\r' && body[start + 1] == '\n') {
                start += 2;
            } else if (start + 2 <= body.length && body[start] == '-' && body[start + 1] == '-') {
                continue; // End boundary marker
            }

            // Remove CRLF before next boundary
            if (end - 2 >= start && body[end - 2] == '\r' && body[end - 1] == '\n') {
                end -= 2;
            }

            if (end <= start)
                continue;

            // Find headers/body split (\r\n\r\n)
            int split = -1;
            for (int k = start; k < end - 3; k++) {
                if (body[k] == '\r' && body[k + 1] == '\n' &&
                        body[k + 2] == '\r' && body[k + 3] == '\n') {
                    split = k;
                    break;
                }
            }

            if (split != -1) {
                // Parse headers
                String headerStr = new String(body, start, split - start, StandardCharsets.US_ASCII);
                Map<String, String> headers = parseHeaders(headerStr);

                // Extract body
                int bodyStart = split + 4;
                int bodyLen = end - bodyStart;
                byte[] partBody = new byte[bodyLen];
                System.arraycopy(body, bodyStart, partBody, 0, bodyLen);

                // Extract name and filename from Content-Disposition
                String contentDisposition = headers.get("content-disposition");
                String name = null;
                String filename = null;

                if (contentDisposition != null) {
                    for (String param : contentDisposition.split(";")) {
                        param = param.trim();
                        if (param.startsWith("name=")) {
                            name = unquote(param.substring(5));
                        } else if (param.startsWith("filename=")) {
                            filename = unquote(param.substring(9));
                        }
                    }
                }

                parts.add(new MultiPart(name, filename, headers, partBody));
            }
        }

        return parts;
    }

    // ========= HELPER METHODS =========
    private Map<String, String> parseHeaders(String headerStr) {
        Map<String, String> headers = new HashMap<>();
        for (String line : headerStr.split("\r\n")) {
            int idx = line.indexOf(':');
            if (idx != -1) {
                String key = line.substring(0, idx).trim().toLowerCase();
                String value = line.substring(idx + 1).trim();
                headers.put(key, value);
            }
        }
        System.out.println("headers : " + headers.toString());
        return headers;
    }

    private String unquote(String val) {
        if (val.startsWith("\"") && val.endsWith("\"")) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }
}