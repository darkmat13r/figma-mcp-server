// MCP Protocol Types
export interface MCPRequest {
  id: string;
  method: string;
  params?: Record<string, any>;
}

export interface MCPResponse {
  id: string;
  result?: any;
  error?: {
    code: number;
    message: string;
    data?: any;
  };
}

// Plugin Message Types
export type PluginMessage =
  | { type: 'ws-connected' }
  | { type: 'ws-disconnected'; reason?: string }
  | { type: 'ws-error'; error: string }
  | { type: 'mcp-response'; response: MCPResponse }
  | { type: 'selection-changed'; selection: SelectionInfo[] }
  | { type: 'operation-complete'; message: string }
  | { type: 'operation-error'; error: string };

export type UIMessage =
  | { type: 'connect-ws'; url: string }
  | { type: 'disconnect-ws' }
  | { type: 'mcp-request'; request: MCPRequest }
  | { type: 'create-color-styles'; colors: ColorToken[] }
  | { type: 'create-text-styles'; styles: TextStyleToken[] }
  | { type: 'create-node'; nodeType: string; properties: Record<string, any> }
  | { type: 'get-selection' };

// Design System Types
export interface ColorToken {
  name: string;
  hex: string;
  description?: string;
}

export interface TextStyleToken {
  name: string;
  fontSize: number;
  fontWeight: number;
  lineHeight: number;
  letterSpacing: number;
  fontFamily: string;
  textCase?: 'UPPER' | 'LOWER' | 'TITLE';
}

export interface SelectionInfo {
  id: string;
  name: string;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

// WebSocket Connection State
export enum ConnectionState {
  DISCONNECTED = 'disconnected',
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  ERROR = 'error',
}

// MCP Tool Definitions
export interface MCPTool {
  name: string;
  description: string;
  inputSchema: {
    type: string;
    properties: Record<string, any>;
    required?: string[];
  };
}
