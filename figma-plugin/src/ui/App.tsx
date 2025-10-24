import React from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { ConnectionPanel } from './components/ConnectionPanel';
import { ConsolePanel } from './components/ConsolePanel';

export const App: React.FC = () => {
  console.log('App component rendering...');

  let content;
  try {
    const { connectionState, error, logs, connect, disconnect, clearLogs } = useWebSocket();

    console.log('Hooks initialized successfully');

    content = (
      <div className="dark flex h-screen flex-col bg-background text-foreground">
        {/* Header */}
        <div className="border-b border-border bg-background p-4">
          <h1 className="text-xl font-bold">
            Figma MCP Bridge
          </h1>
          <p className="text-xs text-muted-foreground">
            Connect to MCP server and control Figma with Claude Code
          </p>
        </div>

        {/* Collapsible Panels Container */}
        <div className="flex-1 overflow-y-auto">
          {/* Connection Panel - Open by default */}
          <ConnectionPanel
            connectionState={connectionState}
            error={error}
            onConnect={connect}
            onDisconnect={disconnect}
          />

          {/* Console Panel - Collapsed by default */}
          <ConsolePanel logs={logs} onClear={clearLogs} />
        </div>
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
