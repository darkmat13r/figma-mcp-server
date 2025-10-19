import React, { useState } from 'react';
import { Section } from './composed/Section';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { ConnectionState } from '../types';

interface TestPanelProps {
  connectionState: ConnectionState;
  onSendMessage: (message: any) => void;
}

export const TestPanel: React.FC<TestPanelProps> = ({ connectionState, onSendMessage }) => {
  const [requestId, setRequestId] = useState('test-1');

  const sendTestMessage = (method: string, params?: any) => {
    const message = {
      id: requestId,
      method,
      params,
    };
    onSendMessage(message);

    // Auto-increment request ID
    const match = requestId.match(/^(.*?)(\d+)$/);
    if (match) {
      const prefix = match[1];
      const num = parseInt(match[2]) + 1;
      setRequestId(`${prefix}${num}`);
    }
  };

  const isConnected = connectionState === ConnectionState.CONNECTED;

  return (
    <Section
      title="Test Commands"
      description="Send test WebSocket messages"
    >
      <div className="space-y-3">
        <div className="space-y-2">
          <label className="text-xs font-medium text-muted-foreground">
            Request ID
          </label>
          <Input
            value={requestId}
            onChange={(e) => setRequestId(e.target.value)}
            placeholder="test-1"
            className="h-8"
          />
        </div>

        <div className="grid grid-cols-2 gap-2">
          <Button
            size="sm"
            onClick={() => sendTestMessage('ping')}
            disabled={!isConnected}
            variant="outline"
          >
            Ping
          </Button>

          <Button
            size="sm"
            onClick={() => sendTestMessage('echo', { message: 'Hello!' })}
            disabled={!isConnected}
            variant="outline"
          >
            Echo
          </Button>

          <Button
            size="sm"
            onClick={() => sendTestMessage('initialize', {
              protocolVersion: '2024-11-05',
              capabilities: { roots: { listChanged: false } },
              clientInfo: { name: 'Figma Plugin', version: '1.0' }
            })}
            disabled={!isConnected}
            variant="outline"
          >
            Initialize
          </Button>

          <Button
            size="sm"
            onClick={() => sendTestMessage('tools/list')}
            disabled={!isConnected}
            variant="outline"
          >
            List Tools
          </Button>

          <Button
            size="sm"
            onClick={() => sendTestMessage('tools/call', {
              name: 'figma_get_selection'
            })}
            disabled={!isConnected}
            variant="outline"
            className="col-span-2"
          >
            Get Selection
          </Button>
        </div>

        {!isConnected && (
          <p className="text-xs text-yellow-500/80">
            Connect to WebSocket server to send messages
          </p>
        )}
      </div>
    </Section>
  );
};
