import { useState, useEffect, useCallback, useRef } from 'react';
import { ConnectionState, MCPRequest, MCPResponse } from '../types';

export interface ConsoleLog {
  id: number;
  timestamp: Date;
  type: 'sent' | 'received' | 'error' | 'info';
  data: any;
}

export function useWebSocket() {
  const [connectionState, setConnectionState] = useState<ConnectionState>(
    ConnectionState.DISCONNECTED
  );
  const [lastResponse, setLastResponse] = useState<MCPResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [logs, setLogs] = useState<ConsoleLog[]>([]);

  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const logIdCounter = useRef(0);

  const addLog = useCallback((type: ConsoleLog['type'], data: any) => {
    setLogs((prev) => [
      ...prev,
      {
        id: logIdCounter.current++,
        timestamp: new Date(),
        type,
        data,
      },
    ]);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
    logIdCounter.current = 0;
  }, []);

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
            addLog('sent', message.request);
          } else {
            const errorMsg = 'WebSocket not connected';
            addLog('error', errorMsg);
            parent.postMessage(
              {
                pluginMessage: {
                  type: 'ws-error',
                  error: errorMsg,
                },
              },
              '*'
            );
          }
          break;

        case 'ws-response':
          // Plugin sending response to a tool command
          if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            const response = {
              id: message.requestId,
              result: message.result,
            };
            wsRef.current.send(JSON.stringify(response));
            addLog('sent', response);
          }
          break;

        case 'operation-complete':
          console.log('Operation complete:', message.message);
          addLog('info', message.message);
          break;

        case 'operation-error':
          setError(message.error);
          addLog('error', message.error);
          break;
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [addLog]);

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

        addLog('info', `Connected to ${url}`);
        console.log('WebSocket connected to', url);
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);

          // Check if this is a tool command (has 'method' field)
          if (data.method) {
            // This is a command from the server to execute in Figma
            addLog('received', data);
            parent.postMessage(
              {
                pluginMessage: {
                  type: 'ws-command',
                  command: data,
                },
              },
              '*'
            );
          } else {
            // This is a response
            const response = data as MCPResponse;
            setLastResponse(response);
            addLog('received', response);

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
          }
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
          const errorMsg = 'Failed to parse server response';
          setError(errorMsg);
          addLog('error', errorMsg);
        }
      };

      ws.onerror = (event) => {
        console.error('WebSocket error:', event);
        setConnectionState(ConnectionState.ERROR);
        const errorMsg = 'WebSocket connection error';
        setError(errorMsg);
        addLog('error', errorMsg);

        parent.postMessage(
          {
            pluginMessage: {
              type: 'ws-error',
              error: errorMsg,
            },
          },
          '*'
        );
      };

      ws.onclose = (event) => {
        console.log('WebSocket closed:', event.code, event.reason);
        setConnectionState(ConnectionState.DISCONNECTED);
        wsRef.current = null;

        const disconnectMsg = event.reason || 'Connection closed';
        addLog('info', `Disconnected: ${disconnectMsg}`);

        parent.postMessage(
          {
            pluginMessage: {
              type: 'ws-disconnected',
              reason: disconnectMsg,
            },
          },
          '*'
        );

        // Auto-reconnect after 3 seconds if it wasn't a clean close
        if (!event.wasClean && connectionState === ConnectionState.CONNECTED) {
          reconnectTimeoutRef.current = setTimeout(() => {
            console.log('Attempting to reconnect...');
            addLog('info', 'Attempting to reconnect...');
            connect(url);
          }, 3000);
        }
      };
    } catch (error) {
      setConnectionState(ConnectionState.ERROR);
      const errorMessage = error instanceof Error ? error.message : 'Failed to connect';
      setError(errorMessage);
      addLog('error', errorMessage);

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
  }, [connectionState, addLog]);

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
      const errorMsg = 'Not connected to WebSocket server';
      setError(errorMsg);
      addLog('error', errorMsg);
      return;
    }

    try {
      wsRef.current.send(JSON.stringify(request));
      addLog('sent', request);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to send request';
      setError(errorMessage);
      addLog('error', errorMessage);
      console.error('Failed to send WebSocket message:', error);
    }
  }, [addLog]);

  return {
    connectionState,
    lastResponse,
    error,
    logs,
    connect,
    disconnect,
    sendRequest,
    clearLogs,
  };
}
