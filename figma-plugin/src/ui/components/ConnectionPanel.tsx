/**
 * ConnectionPanel Component
 *
 * Displays MCP server connection status and controls.
 * Uses design system components for consistent styling.
 */

import React, { useState } from 'react';
import { ConnectionState } from '../types';
import { Button } from '@/ui/components/ui/button';
import { Input } from '@/ui/components/ui/input';
import { Alert, AlertDescription } from '@/ui/components/ui/alert';
import { Section } from '@/ui/components/composed/Section';
import { StatusBadge, StatusType } from '@/ui/components/composed/StatusBadge';

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
  const [serverUrl, setServerUrl] = useState('ws://localhost:8081');

  const handleConnect = () => {
    if (serverUrl.trim()) {
      onConnect(serverUrl.trim());
    }
  };

  const getStatusType = (): StatusType => {
    switch (connectionState) {
      case ConnectionState.CONNECTED:
        return 'success';
      case ConnectionState.CONNECTING:
        return 'loading';
      case ConnectionState.ERROR:
        return 'error';
      default:
        return 'neutral';
    }
  };

  const getStatusLabel = (): string => {
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
    <div className="border-b border-border bg-card p-4">
      <Section
        title="MCP Server"
        action={
          <StatusBadge
            status={getStatusType()}
            label={getStatusLabel()}
          />
        }
        className="space-y-3"
      >
        <div className="space-y-3">
          {/* Server URL Input */}
          <div className="flex gap-2 items-end">
            <div className="flex-1">
              <Input
                type="text"
                value={serverUrl}
                onChange={(e) => setServerUrl(e.target.value)}
                placeholder="ws://localhost:8081/mcp"
                disabled={connectionState === ConnectionState.CONNECTED}
              />
            </div>
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
      </Section>
    </div>
  );
};
