/**
 * ConnectionPanel Component with File-Specific Routing
 *
 * ## Purpose
 * Displays MCP server connection status and provides the MCP URL for Claude Code configuration.
 *
 * ## File-Specific Routing
 * - Shows the WebSocket URL with the current Figma file's ID
 * - Displays the MCP URL that Claude Code should use
 * - Provides copy-to-clipboard functionality for easy setup
 *
 * ## SOLID Principles
 * - Single Responsibility: Only displays connection information and controls
 * - Dependency Inversion: Receives props, doesn't know about internal state management
 */

import React, { useState, useEffect } from 'react';
import { ConnectionState } from '../types';
import { Button } from '@/ui/components/ui/button';
import { Input } from '@/ui/components/ui/input';
import { Alert, AlertDescription } from '@/ui/components/ui/alert';
import { Section } from '@/ui/components/composed/Section';
import { StatusBadge, StatusType } from '@/ui/components/composed/StatusBadge';
import { Label } from '@/ui/components/ui/label';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/ui/components/ui/collapsible';
import { ChevronDown, ChevronUp } from 'lucide-react';

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
  const [serverUrl, setServerUrl] = useState('ws://localhost:8081/figma');
  const [fileId, setFileId] = useState<string>('');
  const [mcpUrl, setMcpUrl] = useState<string>('');
  const [copySuccess, setCopySuccess] = useState<string>('');
  const [isOpen, setIsOpen] = useState(false);

  // Get file ID from Figma on mount and when file changes
  useEffect(() => {
    // Request file info from plugin
    parent.postMessage(
      {
        pluginMessage: {
          type: 'get-file-info',
        },
      },
      '*'
    );

    // Listen for file info response
    const handleMessage = (event: MessageEvent) => {
      const message = event.data.pluginMessage;
      if (message?.type === 'file-info') {
        const fid = message.fileKey || 'unknown';
        setFileId(fid);

        // Build MCP URL with fileId
        const sseUrl = `http://localhost:1234/sse?fileId=${encodeURIComponent(fid)}`;
        setMcpUrl(sseUrl);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const handleConnect = () => {
    if (serverUrl.trim()) {
      // Build URL with fileId
      const separator = serverUrl.includes('?') ? '&' : '?';
      const urlWithFileId = `${serverUrl}${separator}fileId=${encodeURIComponent(fileId)}`;
      onConnect(urlWithFileId);
    }
  };

  const copyToClipboard = async (text: string, label: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopySuccess(label);
      setTimeout(() => setCopySuccess(''), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
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
    <Collapsible open={isOpen} onOpenChange={setIsOpen} className="border-b border-border bg-card">
      <div className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h3 className="text-sm font-medium">Connection</h3>
            <StatusBadge
              status={getStatusType()}
              label={getStatusLabel()}
            />
          </div>
          <CollapsibleTrigger asChild>
            <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
              {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </Button>
          </CollapsibleTrigger>
        </div>
      </div>

      <CollapsibleContent>
        <div className="px-4 pb-4 space-y-3">
          {/* File ID Display */}
          <div className="space-y-1">
            <Label className="text-xs font-medium text-muted-foreground">
              File ID
            </Label>
            <div className="flex gap-2">
              <Input
                type="text"
                value={fileId || 'Loading...'}
                readOnly
                className="text-xs font-mono"
              />
              <Button
                onClick={() => copyToClipboard(fileId, 'fileId')}
                variant="outline"
                size="sm"
                disabled={!fileId}
              >
                {copySuccess === 'fileId' ? 'Copied!' : 'Copy'}
              </Button>
            </div>
          </div>

          {/* MCP URL for Claude Code */}
          <div className="space-y-1">
            <Label className="text-xs font-medium text-muted-foreground">
              MCP URL (for Claude Code Config)
            </Label>
            <div className="flex gap-2">
              <Input
                type="text"
                value={mcpUrl || 'Waiting for file ID...'}
                readOnly
                className="text-xs font-mono"
              />
              <Button
                onClick={() => copyToClipboard(mcpUrl, 'mcpUrl')}
                variant="outline"
                size="sm"
                disabled={!mcpUrl}
              >
                {copySuccess === 'mcpUrl' ? 'Copied!' : 'Copy'}
              </Button>
            </div>
            <p className="text-xs text-muted-foreground">
              Use this URL in your Claude Desktop MCP configuration
            </p>
          </div>

          {/* WebSocket Server URL */}
          <div className="space-y-1">
            <Label className="text-xs font-medium text-muted-foreground">
              WebSocket Server
            </Label>
            <div className="flex gap-2 items-end">
              <div className="flex-1">
                <Input
                  type="text"
                  value={serverUrl}
                  onChange={(e) => setServerUrl(e.target.value)}
                  placeholder="ws://localhost:8081/figma"
                  disabled={connectionState === ConnectionState.CONNECTED}
                  className="text-xs"
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
                  disabled={connectionState === ConnectionState.CONNECTING || !fileId}
                  size="sm"
                >
                  Connect
                </Button>
              )}
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <Alert variant="destructive">
              <AlertDescription className="text-xs">
                {error}
              </AlertDescription>
            </Alert>
          )}

          {/* Connection Info */}
          {connectionState === ConnectionState.CONNECTED && (
            <Alert>
              <AlertDescription className="text-xs">
                Connected to file: {fileId}
              </AlertDescription>
            </Alert>
          )}
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
};
