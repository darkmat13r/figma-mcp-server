/// <reference types="@figma/plugin-typings" />

import { PluginMessage, UIMessage, ColorToken, TextStyleToken } from './types';

// WebSocket connection (Figma plugins can't use WebSocket directly, must use iframe bridge)
// The UI will establish the WebSocket connection and relay messages
let isConnected = false;

// Show plugin UI
figma.showUI(__html__, {
  width: 480,
  height: 640,
  themeColors: true,
});

// Helper: Convert hex to RGB
function hexToRgb(hex: string): RGB {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!result) {
    throw new Error(`Invalid hex color: ${hex}`);
  }
  return {
    r: parseInt(result[1], 16) / 255,
    g: parseInt(result[2], 16) / 255,
    b: parseInt(result[3], 16) / 255,
  };
}

// Helper: Get font weight name
function getFontWeightName(weight: number): string {
  const weights: Record<number, string> = {
    100: 'Thin',
    200: 'ExtraLight',
    300: 'Light',
    400: 'Regular',
    500: 'Medium',
    600: 'SemiBold',
    700: 'Bold',
    800: 'ExtraBold',
    900: 'Black',
  };
  return weights[weight] || 'Regular';
}

// Create color styles
async function createColorStyles(colors: ColorToken[]): Promise<void> {
  try {
    let createdCount = 0;

    for (const color of colors) {
      const rgb = hexToRgb(color.hex);
      const paintStyle = figma.createPaintStyle();
      paintStyle.name = color.name;
      paintStyle.paints = [{ type: 'SOLID', color: rgb }];

      if (color.description) {
        paintStyle.description = color.description;
      }

      createdCount++;
    }

    sendMessage({
      type: 'operation-complete',
      message: `Successfully created ${createdCount} color styles`,
    });
  } catch (error) {
    sendMessage({
      type: 'operation-error',
      error: error instanceof Error ? error.message : String(error),
    });
  }
}

// Create text styles
async function createTextStyles(styles: TextStyleToken[]): Promise<void> {
  try {
    let createdCount = 0;

    for (const style of styles) {
      const textStyle = figma.createTextStyle();
      textStyle.name = style.name;
      textStyle.fontSize = style.fontSize;

      // Load font before setting it
      const fontName = {
        family: style.fontFamily,
        style: getFontWeightName(style.fontWeight),
      };

      try {
        await figma.loadFontAsync(fontName);
        textStyle.fontName = fontName;
      } catch (fontError) {
        console.warn(`Failed to load font ${fontName.family} ${fontName.style}, using default`);
        await figma.loadFontAsync({ family: 'Inter', style: 'Regular' });
        textStyle.fontName = { family: 'Inter', style: 'Regular' };
      }

      textStyle.lineHeight = {
        value: style.fontSize * style.lineHeight,
        unit: 'PIXELS',
      };

      textStyle.letterSpacing = {
        value: style.fontSize * style.letterSpacing,
        unit: 'PIXELS',
      };

      if (style.textCase) {
        textStyle.textCase = style.textCase;
      }

      createdCount++;
    }

    sendMessage({
      type: 'operation-complete',
      message: `Successfully created ${createdCount} text styles`,
    });
  } catch (error) {
    sendMessage({
      type: 'operation-error',
      error: error instanceof Error ? error.message : String(error),
    });
  }
}

// Create nodes based on type
function createNode(nodeType: string, properties: Record<string, any>): void {
  try {
    let node: SceneNode | null = null;

    switch (nodeType.toUpperCase()) {
      case 'RECTANGLE':
        node = figma.createRectangle();
        break;
      case 'ELLIPSE':
        node = figma.createEllipse();
        break;
      case 'FRAME':
        node = figma.createFrame();
        break;
      case 'TEXT':
        node = figma.createText();
        break;
      case 'LINE':
        node = figma.createLine();
        break;
      default:
        throw new Error(`Unknown node type: ${nodeType}`);
    }

    if (node) {
      // Apply properties
      Object.entries(properties).forEach(([key, value]) => {
        if (key === 'fills' && Array.isArray(value)) {
          (node as GeometryMixin).fills = value.map((fill: any) => {
            if (fill.type === 'SOLID' && fill.color) {
              return {
                type: 'SOLID',
                color: hexToRgb(fill.color),
              };
            }
            return fill;
          });
        } else if (key in node) {
          (node as any)[key] = value;
        }
      });

      // Add to current page
      figma.currentPage.appendChild(node);
      figma.currentPage.selection = [node];
      figma.viewport.scrollAndZoomIntoView([node]);

      sendMessage({
        type: 'operation-complete',
        message: `Created ${nodeType} node`,
      });
    }
  } catch (error) {
    sendMessage({
      type: 'operation-error',
      error: error instanceof Error ? error.message : String(error),
    });
  }
}

// Get current selection info
function getSelectionInfo(): void {
  const selection = figma.currentPage.selection;
  const selectionInfo = selection.map((node) => ({
    id: node.id,
    name: node.name,
    type: node.type,
    x: 'x' in node ? node.x : 0,
    y: 'y' in node ? node.y : 0,
    width: 'width' in node ? node.width : 0,
    height: 'height' in node ? node.height : 0,
  }));

  sendMessage({
    type: 'selection-changed',
    selection: selectionInfo,
  });
}

// Send message to UI
function sendMessage(message: PluginMessage): void {
  figma.ui.postMessage(message);
}

// Handle connection (actual WebSocket is in UI thread)
function handleConnect(): void {
  isConnected = true;
  // Connection is established in UI thread via WebSocket
  // Plugin just tracks the state
}

// Handle disconnect
function handleDisconnect(): void {
  isConnected = false;
}

// Send MCP request (relay through UI which has WebSocket)
function sendMCPRequest(request: any): void {
  if (!isConnected) {
    sendMessage({
      type: 'ws-error',
      error: 'Not connected to MCP server',
    });
    return;
  }

  // Forward request to UI which will send via WebSocket
  sendMessage({
    type: 'mcp-request',
    request: request,
  });
}

// Handle messages from UI
figma.ui.onmessage = async (msg: UIMessage) => {
  switch (msg.type) {
    case 'connect-ws':
      // UI will handle WebSocket connection, plugin just tracks state
      handleConnect();
      break;

    case 'disconnect-ws':
      handleDisconnect();
      break;

    case 'mcp-request':
      sendMCPRequest(msg.request);
      break;

    case 'mcp-response':
      // Handle responses from server (via UI)
      // Can process server responses here if needed
      break;

    case 'create-color-styles':
      await createColorStyles(msg.colors);
      break;

    case 'create-text-styles':
      await createTextStyles(msg.styles);
      break;

    case 'create-node':
      createNode(msg.nodeType, msg.properties);
      break;

    case 'get-selection':
      getSelectionInfo();
      break;

    default:
      console.warn('Unknown message type:', (msg as any).type);
  }
};

// Listen for selection changes
figma.on('selectionchange', () => {
  getSelectionInfo();
});

// Send initial selection
getSelectionInfo();
