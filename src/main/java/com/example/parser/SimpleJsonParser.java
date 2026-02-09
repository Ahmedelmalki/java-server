package com.example.parser;

import java.util.*;

public class SimpleJsonParser {
    
    private String json;
    private int pos;
    
    public SimpleJsonParser(String json) {
        this.json = json.trim();
        this.pos = 0;
    }
    
    public Object parse() {
        skipWhitespace();
        return parseValue();
    }
    
    private Object parseValue() {
        skipWhitespace();
        char c = peek();
        
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBoolean();
        if (c == 'n') return parseNull();
        if (c == '-' || Character.isDigit(c)) return parseNumber();
        
        throw new RuntimeException("Unexpected character: " + c);
    }
    
    private Map<String, Object> parseObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        consume('{');
        skipWhitespace();
        
        if (peek() == '}') {
            consume('}');
            return map;
        }
        
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            consume(':');
            skipWhitespace();
            Object value = parseValue();
            map.put(key, value);
            
            skipWhitespace();
            if (peek() == '}') {
                consume('}');
                break;
            }
            consume(',');
        }
        
        return map;
    }
    
    private List<Object> parseArray() {
        List<Object> list = new ArrayList<>();
        consume('[');
        skipWhitespace();
        
        if (peek() == ']') {
            consume(']');
            return list;
        }
        
        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();
            
            if (peek() == ']') {
                consume(']');
                break;
            }
            consume(',');
        }
        
        return list;
    }
    
    private String parseString() {
        consume('"');
        StringBuilder sb = new StringBuilder();
        
        while (peek() != '"') {
            char c = next();
            if (c == '\\') {
                char escaped = next();
                switch (escaped) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(escaped);
                }
            } else {
                sb.append(c);
            }
        }
        
        consume('"');
        return sb.toString();
    }
    
    private Object parseNumber() {
        StringBuilder sb = new StringBuilder();
        
        if (peek() == '-') {
            sb.append(next());
        }
        
        while (Character.isDigit(peek())) {
            sb.append(next());
        }
        
        if (peek() == '.') {
            sb.append(next());
            while (Character.isDigit(peek())) {
                sb.append(next());
            }
        }
        
        if (peek() == 'e' || peek() == 'E') {
            sb.append(next());
            if (peek() == '+' || peek() == '-') {
                sb.append(next());
            }
            while (Character.isDigit(peek())) {
                sb.append(next());
            }
        }
        
        String numStr = sb.toString();
        if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
            return Double.parseDouble(numStr);
        } else {
            return Long.parseLong(numStr);
        }
    }
    
    private Boolean parseBoolean() {
        if (peek() == 't') {
            consume('t');
            consume('r');
            consume('u');
            consume('e');
            return true;
        } else {
            consume('f');
            consume('a');
            consume('l');
            consume('s');
            consume('e');
            return false;
        }
    }
    
    private Object parseNull() {
        consume('n');
        consume('u');
        consume('l');
        consume('l');
        return null;
    }
    
    private void skipWhitespace() {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
    }
    
    private char peek() {
        if (pos >= json.length()) {
            throw new RuntimeException("Unexpected end of JSON");
        }
        return json.charAt(pos);
    }
    
    private char next() {
        char c = peek();
        pos++;
        return c;
    }
    
    private void consume(char expected) {
        char actual = next();
        if (actual != expected) {
            throw new RuntimeException("Expected '" + expected + "' but got '" + actual + "'");
        }
    }
}