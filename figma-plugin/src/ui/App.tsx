import React from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { useSelection } from './hooks/useSelection';
import { ConnectionPanel } from './components/ConnectionPanel';
import { DesignSystemPanel } from './components/DesignSystemPanel';
import { SelectionPanel } from './components/SelectionPanel';
import { ConnectionState } from './types';

export const App: React.FC = () => {
  console.log('App component rendering...');

  let content;
  try {
    const { connectionState, error, connect, disconnect } = useWebSocket();
    const selection = useSelection();

    console.log('Hooks initialized successfully');

    content = (
      <div className="dark flex h-screen flex-col bg-background text-foreground">
        {/* Header */}
        <div className="border-b border-border bg-background p-4">
          <h1 className="text-xl font-bold">
            Figma MCP Bridge
          </h1>
          <p className="text-xs text-muted-foreground">
            Connect to MCP server and control Figma
          </p>
        </div>

        {/* Connection Panel */}
        <ConnectionPanel
          connectionState={connectionState}
          error={error}
          onConnect={connect}
          onDisconnect={disconnect}
        />

        {/* Main Content */}
        <DesignSystemPanel
          connected={connectionState === ConnectionState.CONNECTED}
        />

        {/* Selection Info */}
        <SelectionPanel selection={selection} />
      </div>
    );
  } catch (err) {
    console.error('Error in App component:', err);
    content = (
      <div style={{ padding: '20px', color: 'white', background: '#2C2C2C' }}>
        <h1 style={{ color: '#ff6b6b' }}>Error Loading App</h1>
        <pre style={{ color: '#ffa500', marginTop: '10px' }}>{String(err)}</pre>
      </div>
    );
  }

  return content;
};
