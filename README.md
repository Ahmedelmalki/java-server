# Java HTTP/1.1 Server

A lightweight, high-performance HTTP/1.1 server implementation in Java, built from scratch using non-blocking I/O with Java NIO. This server demonstrates robust handling of HTTP protocol features including chunked transfer encoding, multipart form data, session management, and CGI script execution.

## Features

### Core HTTP Functionality
- ✅ **HTTP/1.1 Compliance** - Full protocol implementation with proper request/response handling
- ✅ **Non-blocking I/O** - Event-driven architecture using Java NIO Selector for high concurrency
- ✅ **Keep-Alive Connections** - Connection reuse with configurable timeouts
- ✅ **Chunked Transfer Encoding** - Binary-safe streaming with incremental parsing
- ✅ **Content-Length Bodies** - Fixed-length request body handling with size limits

### HTTP Methods
- `GET` - Static file serving with directory listing support
- `POST` - File uploads and form data processing
- `DELETE` - File deletion with security checks

### Advanced Features
- 📁 **Static File Serving** - Efficient file delivery with MIME type detection
- 📤 **File Uploads** - Binary and multipart/form-data support
- 🔄 **HTTP Redirects** - Configurable 301/302 redirects
- 🍪 **Session Management** - Cookie-based sessions with expiration
- 🔧 **CGI Support** - Execute Python and PHP scripts via CGI/1.1
- 📂 **Directory Listing** - Auto-generated HTML directory indexes
- 🚫 **Custom Error Pages** - Configurable error pages for all HTTP status codes
- 📊 **Request Size Limits** - Configurable body size enforcement (413 responses)

## Architecture

### Project Structure
```
java-server/
├── src/main/java/com/example/
│   ├── config/          # Configuration data models
│   │   ├── ConfigLoader.java
│   │   ├── ServerConfig.java
│   │   ├── RouteConfig.java
│   │   └── RedirectConfig.java
│   ├── core/            # Server core & connection management
│   │   ├── Server.java
│   │   ├── ConnectionContext.java
│   │   └── ConnState.java
│   ├── handlers/        # Request handlers
│   │   ├── StaticFileHandler.java
│   │   ├── UploadHandler.java
│   │   ├── CGIHandler.java
│   │   ├── DeleteHandler.java
│   │   ├── SessionHandler.java
│   │   └── ErrorHandler.java
│   ├── http/            # HTTP protocol implementation
│   │   ├── HTTPRequest.java
│   │   ├── HTTPResponse.java
│   │   └── MultiPart.java
│   ├── parser/          # Protocol parsers
│   │   ├── BodyReader.java
│   │   ├── ChunkedBodyReader.java
│   │   ├── FixedLengthReader.java
│   │   ├── MultipartParser.java
│   │   ├── ConfigParser.java
│   │   └── SimpleJsonParser.java
│   ├── routing/         # Request routing
│   │   └── Router.java
│   ├── session/         # Session management
│   │   ├── Session.java
│   │   ├── SessionManager.java
│   │   └── Cookie.java
│   └── Main.java
├── config.json          # Server configuration
├── www/                 # Web root directory
├── test/                # Test scripts
└── pom.xml
```

### Design Patterns

**Event-Driven Architecture**: Uses Java NIO Selector for multiplexed I/O operations
- Single-threaded event loop
- Non-blocking socket operations
- Efficient handling of thousands of concurrent connections

**State Machine Pattern**: Connection lifecycle management
- `READING_HEADERS` → `READING_BODY` → `PROCESSING` → `WRITING_RESPONSE` → `CLOSED`
- Clean state transitions with proper resource cleanup

**Strategy Pattern**: Body readers for different transfer encodings
- `FixedLengthReader` for Content-Length bodies
- `ChunkedBodyReader` for chunked transfer encoding
- Pluggable design for easy extension

## Requirements

- **Java**: 17 or higher
- **Maven**: 3.6 or higher
- **Operating System**: Linux, macOS, or Windows

## Installation
```bash
# Clone the repository
git clone <repository-url>
cd java-server

# Build the project
mvn clean compile

# Run the server
mvn exec:java
```

The server will start on the configured ports (default: 8080, 8081).

## Configuration

The server is configured via `config.json`:
```json
{
  "servers": [
    {
      "host": "localhost",
      "ports": [8080, 8081],
      "serverName": "primary-server",
      "isDefault": true,
      "clientMaxBodySize": 10485760,
      "timeout": 30000,
      "errorPages": {
        "404": "src/main/java/com/example/error_pages/404.html"
      },
      "routes": [
        {
          "path": "/",
          "methods": ["GET"],
          "root": "www",
          "autoindex": true
        },
        {
          "path": "/uploads",
          "methods": ["GET", "POST", "DELETE"],
          "root": "www/uploads",
          "uploadEnabled": true
        },
        {
          "path": "/api",
          "methods": ["GET", "POST"],
          "root": "www/api/cgi-bin",
          "cgi": {
            ".py": "/usr/bin/python3",
            ".php": "/usr/bin/php"
          }
        },
        {
          "path": "/redirect",
          "redirect": {
            "code": 301,
            "url": "http://localhost:8080/"
          }
        }
      ]
    }
  ]
}
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `host` | Server hostname | `localhost` |
| `ports` | Array of ports to listen on | `[8080]` |
| `serverName` | Virtual host name | - |
| `isDefault` | Default server for unknown hosts | `false` |
| `clientMaxBodySize` | Max request body size in bytes | `5242880` (5MB) |
| `timeout` | Connection timeout in milliseconds | `30000` (30s) |
| `errorPages` | Custom error page paths | `{}` |
| `routes` | Route configurations | `[]` |

### Route Configuration

| Option | Description | Required |
|--------|-------------|----------|
| `path` | URL path prefix | ✓ |
| `methods` | Allowed HTTP methods | ✓ |
| `root` | File system root directory | - |
| `index` | Default file for directories | - |
| `autoindex` | Enable directory listing | `false` |
| `uploadEnabled` | Enable file uploads | `false` |
| `cgi` | CGI handler mappings | - |
| `redirect` | Redirect configuration | - |

## Usage Examples

### Static File Serving
```bash
# Serve a file
curl http://localhost:8080/static/test.html

# List directory contents
curl http://localhost:8080/static/
```

### File Upload
```bash
# Binary upload
curl -X POST --data-binary @file.bin http://localhost:8080/uploads

# Multipart form upload
curl -X POST -F "file=@document.pdf" http://localhost:8080/uploads
```

### File Deletion
```bash
curl -X DELETE http://localhost:8080/uploads/file.bin
```

### CGI Execution
```bash
# Execute Python CGI script
curl http://localhost:8080/api/hello.py

# POST data to CGI
curl -X POST -d "name=value" http://localhost:8080/api/process.py
```

### Session Management
```bash
# Sessions are automatically managed via cookies
curl -c cookies.txt http://localhost:8080/session-demo
curl -b cookies.txt http://localhost:8080/session-demo
```

## Testing

The project includes a comprehensive test suite:
```bash
# Run all tests
cd test
./run_tests.sh

# Run individual tests
./test_static_files.sh
./test_redirect.sh
./test_virtual_hosts.sh
./test_methods_and_error_pages.sh
./test_delete.sh
./test_multipart_upload.sh
./test_binary_chunked.sh
./test_chunked_fragmented.sh
./test_body_size_limit.sh
./test_session_cookies.sh
./test_cgi.sh
./test_cgi_post_body.sh
./test_keepalive.sh
./test_bad_request_recovery.sh
```

### Stress Testing
```bash
# Requires siege (install via: apt install siege or brew install siege)
./test/stress_test.sh
```

**Target**: 99.5% availability under load

## Performance

- **Single-threaded**: One event loop, no thread overhead
- **Non-blocking I/O**: Handles thousands of concurrent connections
- **Zero-copy**: Efficient file serving with NIO channels
- **Memory efficient**: Streaming body processing with configurable limits

## Security Features

- ✅ Directory traversal protection
- ✅ Request body size limits (413 enforcement)
- ✅ Connection timeouts
- ✅ HTTP-only session cookies
- ✅ Path sanitization for uploads and deletions
- ✅ CGI environment isolation

## Limitations

- Single-threaded (suitable for I/O-bound workloads)
- No HTTPS support (HTTP only)
- No HTTP/2 support
- No gzip/compression support
- CGI scripts run synchronously

## Technical Highlights

### Chunked Transfer Encoding
Implements a robust state machine for parsing chunked requests:
- Binary-safe chunk processing
- Handles fragmented chunk size lines
- Supports trailer headers
- Incremental parsing with proper state transitions

### Multipart Form Data
Full RFC 2388 implementation:
- Boundary detection and parsing
- Binary-safe file uploads
- Header parsing for each part
- Content-Disposition attribute extraction

### Session Management
Thread-safe session handling:
- UUID-based session IDs
- Configurable expiration
- Automatic cleanup of expired sessions
- Cookie-based session tracking

## Contributing

This is an educational project demonstrating HTTP server implementation fundamentals. Contributions are welcome for:
- Bug fixes
- Documentation improvements
- Test coverage expansion
- Performance optimizations

## License

This project is provided as-is for educational purposes.

## Acknowledgments

Built as part of a learning exercise to understand:
- HTTP/1.1 protocol internals
- Non-blocking I/O with Java NIO
- Event-driven server architecture
- State machine design patterns
- Binary protocol parsing

---

**Author**: Developed as a comprehensive HTTP server implementation exercise  
**Version**: 1.0.0  
**Last Updated**: February 2026
