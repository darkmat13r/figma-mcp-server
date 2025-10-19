#!/bin/bash

# Test MCP Server - Send initialize message
echo "Testing MCP Server..."
echo ""
echo "Sending initialize message..."
echo '{"jsonrpc":"2.0","id":"1","method":"initialize","params":{"protocolVersion":"2024-11-05","clientInfo":{"name":"test-client","version":"1.0.0"},"capabilities":{}}}' | timeout 5 java -jar server/build/libs/server-all.jar 2>&1 | head -20