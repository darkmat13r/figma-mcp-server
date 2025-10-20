import React from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { ConnectionPanel } from './components/ConnectionPanel';
import { ConsolePanel } from './components/ConsolePanel';
import { TestPanel } from './components/TestPanel';
import { StylingTestPanel } from './components/StylingTestPanel';
import { TypographyTestPanel } from './components/TypographyTestPanel';
import { HierarchyTestPanel } from './components/HierarchyTestPanel';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './components/ui/tabs';

export const App: React.FC = () => {
  console.log('App component rendering...');

  let content;
  try {
    const { connectionState, error, logs, connect, disconnect, clearLogs, sendRequest } = useWebSocket();

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

        {/* Main Content with Tabs */}
        <div className="flex-1 overflow-hidden">
          <Tabs defaultValue="node-creation" className="h-full flex flex-col">
            <TabsList className="w-full justify-start rounded-none border-b">
              <TabsTrigger value="node-creation">Node Creation</TabsTrigger>
              <TabsTrigger value="styling">Styling</TabsTrigger>
              <TabsTrigger value="typography">Typography</TabsTrigger>
              <TabsTrigger value="hierarchy">Hierarchy</TabsTrigger>
              <TabsTrigger value="console">Console</TabsTrigger>
            </TabsList>

            <TabsContent value="node-creation" className="flex-1 overflow-auto m-0">
              <TestPanel
                connectionState={connectionState}
                onSendMessage={sendRequest}
              />
            </TabsContent>

            <TabsContent value="styling" className="flex-1 overflow-auto m-0">
              <StylingTestPanel
                onSendCommand={sendRequest}
                isConnected={connectionState === 'connected'}
              />
            </TabsContent>

            <TabsContent value="typography" className="flex-1 overflow-auto m-0">
              <TypographyTestPanel
                onSendCommand={sendRequest}
                isConnected={connectionState === 'connected'}
              />
            </TabsContent>

            <TabsContent value="hierarchy" className="flex-1 overflow-auto m-0">
              <HierarchyTestPanel
                onSendCommand={sendRequest}
                isConnected={connectionState === 'connected'}
              />
            </TabsContent>

            <TabsContent value="console" className="flex-1 overflow-auto m-0">
              <ConsolePanel logs={logs} onClear={clearLogs} />
            </TabsContent>
          </Tabs>
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
