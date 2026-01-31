#!/usr/bin/env python3
import os
import sys
import json
from datetime import datetime

def main():
    # Get cookies from environment
    cookie_header = os.environ.get('HTTP_COOKIE', '')
    
    # Parse session ID from cookies
    session_id = None
    if cookie_header:
        cookies = {}
        for cookie in cookie_header.split(';'):
            if '=' in cookie:
                name, value = cookie.strip().split('=', 1)
                cookies[name] = value
        session_id = cookies.get('JSESSIONID')
    
    # Simulate session data
    response_data = {
        'session_id': session_id,
        'timestamp': datetime.now().isoformat(),
        'cookies_received': cookie_header,
        'message': 'Session demo page'
    }
    
    # Generate response
    response_body = json.dumps(response_data, indent=2)
    
    # HTTP headers
    print("Content-Type: application/json")
    print(f"Content-Length: {len(response_body)}")
    
    # If no session, set a new one (this would be handled by SessionManager in Java)
    if not session_id:
        print("Set-Cookie: JSESSIONID=demo-session-123; Path=/; HttpOnly; SameSite=Lax")
    
    print()  # Empty line to separate headers from body
    print(response_body)

if __name__ == '__main__':
    main()