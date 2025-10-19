import React, { useState } from 'react';
import { ConnectionState } from '../types';
import { Button } from '@/ui/components/ui/button';
import { Input } from '@/ui/components/ui/input';
import { Alert, AlertDescription } from '@/ui/components/ui/alert';
import { Badge } from '@/ui/components/ui/badge';

interface ConnectionPanelProps {
  connectionState: ConnectionState;
  error: string | null;
  onConnect: (url: string) => void;
  onDisconnect: () => void;
}

export const ConnectionPanel: React.FC<ConnectionPanelProps> = ({
  connectionState,
  error,
  onConnect,
  onDisconnect,
}) => {
  const [serverUrl, setServerUrl] = useState('ws://localhost:8080');

  const handleConnect = () => {
    if (serverUrl.trim()) {
      onConnect(serverUrl.trim());
    }
  };

  const getStatusBadgeVariant = () => {
    switch (connectionState) {
      case ConnectionState.CONNECTED:
        return 'default';
      case ConnectionState.ERROR:
        return 'destructive';
      default:
        return 'secondary';
    }
  };

  const getStatusText = () => {
    switch (connectionState) {
      case ConnectionState.CONNECTED:
        return 'Connected';
      case ConnectionState.CONNECTING:
        return 'Connecting...';
      case ConnectionState.ERROR:
        return 'Error';
      default:
        return 'Disconnected';
    }
  };

  return (
    <div className="border-b border-gray-700 bg-figma-bg-secondary p-4">
      <div className="space-y-3">
        {/* Status Indicator */}
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-figma-text">MCP Server</h2>
          <Badge variant={getStatusBadgeVariant() as any}>
            {getStatusText()}
          </Badge>
        </div>

        {/* Server URL Input */}
        <div className="flex gap-2">
          <Input
            type="text"
            value={serverUrl}
            onChange={(e) => setServerUrl(e.target.value)}
            placeholder="ws://localhost:8080/mcp"
            disabled={connectionState === ConnectionState.CONNECTED}
            className="flex-1"
          />
          {connectionState === ConnectionState.CONNECTED ? (
            <Button
              onClick={onDisconnect}
              variant="destructive"
              size="sm"
            >
              Disconnect
            </Button>
          ) : (
            <Button
              onClick={handleConnect}
              disabled={connectionState === ConnectionState.CONNECTING}
              size="sm"
            >
              Connect
            </Button>
          )}
        </div>

        {/* Error Message */}
        {error && (
          <Alert variant="destructive">
            <AlertDescription className="text-xs">
              {error}
            </AlertDescription>
          </Alert>
        )}
      </div>
    </div>
  );
};
