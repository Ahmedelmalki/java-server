#!/usr/bin/python3
import os

print("Content-Type: text/html\r\n\r\n")
print("<h1>CGI Environment Test</h1>")
print(f"<p>Method: {os.environ.get('REQUEST_METHOD')}</p>")
print(f"<p>Query String: {os.environ.get('QUERY_STRING')}</p>")
print(f"<p>Content Length: {os.environ.get('CONTENT_LENGTH')}</p>")
print(f"<p>User Agent: {os.environ.get('HTTP_USER_AGENT')}</p>")