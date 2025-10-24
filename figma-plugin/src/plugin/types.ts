// Shared types for plugin
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

export type PluginMessage =
  | { type: 'ws-connected' }
  | { type: 'ws-disconnected'; reason?: string }
  | { type: 'ws-error'; error: string }
  | { type: 'mcp-request'; request: MCPRequest }
  | { type: 'mcp-response'; response: MCPResponse }
  | { type: 'ws-response'; requestId: string; result: any }
  | { type: 'selection-changed'; selection: SelectionInfo[] }
  | { type: 'operation-complete'; message: string }
  | { type: 'operation-error'; error: string }
  | { type: 'file-info'; fileKey: string; fileName: string };

export type UIMessage =
  | { type: 'connect-ws'; url: string }
  | { type: 'disconnect-ws' }
  | { type: 'mcp-request'; request: MCPRequest }
  | { type: 'mcp-response'; response: MCPResponse }
  | { type: 'ws-command'; command: any }
  | { type: 'create-color-styles'; colors: ColorToken[] }
  | { type: 'create-text-styles'; styles: TextStyleToken[] }
  | { type: 'create-node'; nodeType: string; properties: Record<string, any> }
  | { type: 'get-selection' }
  | { type: 'get-file-info' };
