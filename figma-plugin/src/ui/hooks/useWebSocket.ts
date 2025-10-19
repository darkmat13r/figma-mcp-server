import { useState, useEffect, useCallback, useRef } from 'react';
import { ConnectionState, MCPRequest, MCPResponse } from '../types';

export function useWebSocket() {
  const [connectionState, setConnectionState] = useState<ConnectionState>(
    ConnectionState.DISCONNECTED
  );
  const [lastResponse, setLastResponse] = useState<MCPResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  // Clean up WebSocket on unmount
  useEffect(() => {
    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
    };
  }, []);

  // Handle messages from plugin
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const message = event.data.pluginMessage;
      if (!message) return;

      switch (message.type) {
        case 'mcp-request':
          // Plugin wants to send a request via WebSocket
          if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify(message.request));
          } else {
            parent.postMessage(
              {
                pluginMessage: {
                  type: 'ws-error',
                  error: 'WebSocket not connected',
                },
              },
              '*'
            );
          }
          break;

        case 'operation-complete':
          console.log('Operation complete:', message.message);
          break;

        case 'operation-error':
          setError(message.error);
          break;
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const connect = useCallback((url: string) => {
    setConnectionState(ConnectionState.CONNECTING);
    setError(null);

    try {
      // Create WebSocket connection
      const ws = new WebSocket(url);
      wsRef.current = ws;

      ws.onopen = () => {
        setConnectionState(ConnectionState.CONNECTED);
        setError(null);

        // Notify plugin of successful connection
        parent.postMessage(
          {
            pluginMessage: {
              type: 'ws-connected',
            },
          },
          '*'
        );

        console.log('WebSocket connected to', url);
      };

      ws.onmessage = (event) => {
        try {
          const response = JSON.parse(event.data) as MCPResponse;
          setLastResponse(response);

          // Forward response to plugin
          parent.postMessage(
            {
              pluginMessage: {
                type: 'mcp-response',
                response,
              },
            },
            '*'
          );
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
          setError('Failed to parse server response');
        }
      };

      ws.onerror = (event) => {
        console.error('WebSocket error:', event);
        setConnectionState(ConnectionState.ERROR);
        setError('WebSocket connection error');

        parent.postMessage(
          {
            pluginMessage: {
              type: 'ws-error',
              error: 'WebSocket connection error',
            },
          },
          '*'
        );
      };

      ws.onclose = (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        setConnectionState(ConnectionState.DISCONNECTED);
        wsRef.current = null;

        parent.postMessage(
          {
            pluginMessage: {
              type: 'ws-disconnected',
              reason: event.reason || 'Connection closed',
            },
          },
          '*'
        );

        // Auto-reconnect after 3 seconds if it wasn't a clean close
        if (!event.wasClean && connectionState === ConnectionState.CONNECTED) {
          reconnectTimeoutRef.current = setTimeout(() => {
            console.log('Attempting to reconnect...');
            connect(url);
          }, 3000);
        }
      };
    } catch (error) {
      setConnectionState(ConnectionState.ERROR);
      const errorMessage = error instanceof Error ? error.message : 'Failed to connect';
      setError(errorMessage);

      parent.postMessage(
        {
          pluginMessage: {
            type: 'ws-error',
            error: errorMessage,
          },
        },
        '*'
      );
    }
  }, [connectionState]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    if (wsRef.current) {
      wsRef.current.close(1000, 'User disconnected');
      wsRef.current = null;
    }

    setConnectionState(ConnectionState.DISCONNECTED);
    parent.postMessage(
      {
        pluginMessage: {
          type: 'disconnect-ws',
        },
      },
      '*'
    );
  }, []);

  const sendRequest = useCallback((request: MCPRequest) => {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) {
      setError('Not connected to WebSocket server');
      return;
    }

    try {
      wsRef.current.send(JSON.stringify(request));
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to send request';
      setError(errorMessage);
      console.error('Failed to send WebSocket message:', error);
    }
  }, []);

  return {
    connectionState,
    lastResponse,
    error,
    connect,
    disconnect,
    sendRequest,
  };
}
