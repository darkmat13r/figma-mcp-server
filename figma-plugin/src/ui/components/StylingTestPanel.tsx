import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Card } from './ui/card';
import { Section } from './composed';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';

interface SelectionInfo {
  id: string;
  name: string;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

interface StylingTestPanelProps {
  onSendCommand: (command: any) => void;
  isConnected: boolean;
}

export const StylingTestPanel: React.FC<StylingTestPanelProps> = ({ onSendCommand, isConnected }) => {
  // State for test results
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // State for each styling tool
  const [nodeId, setNodeId] = useState('');

  // Fills
  const [fillColor, setFillColor] = useState('#FF0000');
  const [fillOpacity, setFillOpacity] = useState('1');

  // Strokes
  const [strokeColor, setStrokeColor] = useState('#000000');
  const [strokeWeight, setStrokeWeight] = useState('1');
  const [strokeAlign, setStrokeAlign] = useState('CENTER');

  // Effects
  const [shadowX, setShadowX] = useState('0');
  const [shadowY, setShadowY] = useState('4');
  const [shadowBlur, setShadowBlur] = useState('8');
  const [shadowColor, setShadowColor] = useState('#000000');
  const [shadowOpacity, setShadowOpacity] = useState('0.25');

  // Opacity
  const [opacity, setOpacity] = useState('1');
  const [blendMode, setBlendMode] = useState('NORMAL');

  // Corner Radius
  const [cornerRadius, setCornerRadius] = useState('8');
  const [topLeftRadius, setTopLeftRadius] = useState('');
  const [topRightRadius, setTopRightRadius] = useState('');
  const [bottomLeftRadius, setBottomLeftRadius] = useState('');
  const [bottomRightRadius, setBottomRightRadius] = useState('');

  // Style IDs
  const [styleType, setStyleType] = useState('FILL');
  const [styleId, setStyleId] = useState('');

  const hexToRgb = (hex: string): { r: number; g: number; b: number } => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16) / 255,
      g: parseInt(result[2], 16) / 255,
      b: parseInt(result[3], 16) / 255
    } : { r: 1, g: 0, b: 0 };
  };

  const sendCommand = (method: string, params: any) => {
    const command = {
      id: `test_${Date.now()}`,
      method,
      params
    };

    // Send directly to plugin (not through WebSocket)
    parent.postMessage({ pluginMessage: { type: 'ws-command', command } }, '*');

    setTestResult({ success: true, message: `Sent ${method} command to plugin` });

    // Clear result after 3 seconds
    setTimeout(() => setTestResult(null), 3000);
  };

  const testSetFills = () => {
    const rgb = hexToRgb(fillColor);
    const opacity = parseFloat(fillOpacity);

    // Create proper Figma Paint object
    const fill: any = {
      type: 'SOLID',
      color: {
        r: rgb.r,
        g: rgb.g,
        b: rgb.b
      },
      visible: true
    };

    // Only add opacity if it's less than 1
    if (opacity < 1) {
      fill.opacity = opacity;
    }

    console.log('[StylingTestPanel] Sending fill:', fill);

    sendCommand('setStyle', {
      nodeId,
      fills: [fill]
    });
  };

  const testSetStrokes = () => {
    const rgb = hexToRgb(strokeColor);
    sendCommand('setStyle', {
      nodeId,
      strokes: [{
        type: 'SOLID',
        color: rgb
      }],
      strokeWeight: parseFloat(strokeWeight),
      strokeAlign: strokeAlign
    });
  };

  const testSetEffects = () => {
    const rgb = hexToRgb(shadowColor);
    sendCommand('setStyle', {
      nodeId,
      effects: [{
        type: 'DROP_SHADOW',
        color: {
          ...rgb,
          a: parseFloat(shadowOpacity)
        },
        offset: {
          x: parseFloat(shadowX),
          y: parseFloat(shadowY)
        },
        radius: parseFloat(shadowBlur),
        visible: true
      }]
    });
  };

  const testSetOpacity = () => {
    sendCommand('setStyle', {
      nodeId,
      opacity: parseFloat(opacity),
      blendMode: blendMode
    });
  };

  const testSetCornerRadius = () => {
    const params: any = { nodeId };

    if (topLeftRadius || topRightRadius || bottomLeftRadius || bottomRightRadius) {
      if (topLeftRadius) params.topLeftRadius = parseFloat(topLeftRadius);
      if (topRightRadius) params.topRightRadius = parseFloat(topRightRadius);
      if (bottomLeftRadius) params.bottomLeftRadius = parseFloat(bottomLeftRadius);
      if (bottomRightRadius) params.bottomRightRadius = parseFloat(bottomRightRadius);
    } else {
      params.cornerRadius = parseFloat(cornerRadius);
    }

    sendCommand('setStyle', params);
  };

  const testApplyStyle = () => {
    sendCommand('applyStyle', {
      nodeId,
      styleType,
      styleId
    });
  };

  // Listen for selection changes from plugin
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const message = event.data.pluginMessage;
      if (message && message.type === 'selection-changed') {
        const selection = message.selection as SelectionInfo[];
        if (selection && selection.length > 0) {
          // Auto-fill with the first selected node
          setNodeId(selection[0].id);
          setTestResult({
            success: true,
            message: `Selected: ${selection[0].name} (${selection[0].type})`
          });
          // Clear success message after 3 seconds
          setTimeout(() => setTestResult(null), 3000);
        } else {
          setTestResult({
            success: false,
            message: 'No node selected in Figma'
          });
          setTimeout(() => setTestResult(null), 3000);
        }
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const getSelectedNode = () => {
    parent.postMessage({ pluginMessage: { type: 'get-selection' } }, '*');
  };

  return (
    <div className="space-y-4 p-4">
      {testResult && (
        <Alert variant={testResult.success ? 'default' : 'destructive'}>
          <AlertDescription>{testResult.message}</AlertDescription>
        </Alert>
      )}

      <Section title="Node Selection">
        <div className="space-y-2">
          <Label>Node ID <span className="text-destructive">*</span></Label>
          <div className="flex gap-2">
            <Input
              value={nodeId}
              onChange={(e) => setNodeId(e.target.value)}
              placeholder="Enter node ID or use selection"
            />
            <Button onClick={getSelectedNode} size="sm">
              Get Selected
            </Button>
          </div>
        </div>
      </Section>

      <Separator />

      <Section title="1. Set Fills" description="Apply solid colors to nodes">
        <div className="space-y-3">
          <div className="space-y-2"><Label>Fill Color</Label>
            <div className="flex gap-2">
              <Input
                type="color"
                value={fillColor}
                onChange={(e) => setFillColor(e.target.value)}
                className="w-20"
              />
              <Input
                value={fillColor}
                onChange={(e) => setFillColor(e.target.value)}
                placeholder="#FF0000"
              />
            </div>
          </div>
          <div className="space-y-2"><Label>Opacity</Label>
            <Input
              type="number"
              min="0"
              max="1"
              step="0.1"
              value={fillOpacity}
              onChange={(e) => setFillOpacity(e.target.value)}
            />
          </div>
          <Button onClick={testSetFills} disabled={!nodeId} className="w-full">
            Apply Fills
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="2. Set Strokes" description="Apply borders to nodes">
        <div className="space-y-3">
          <div className="space-y-2"><Label>Stroke Color</Label>
            <div className="flex gap-2">
              <Input
                type="color"
                value={strokeColor}
                onChange={(e) => setStrokeColor(e.target.value)}
                className="w-20"
              />
              <Input
                value={strokeColor}
                onChange={(e) => setStrokeColor(e.target.value)}
                placeholder="#000000"
              />
            </div>
          </div>
          <div className="space-y-2"><Label>Stroke Weight</Label>
            <Input
              type="number"
              min="0"
              step="0.5"
              value={strokeWeight}
              onChange={(e) => setStrokeWeight(e.target.value)}
            />
          </div>
          <div className="space-y-2"><Label>Stroke Align</Label>
            <Select value={strokeAlign} onValueChange={setStrokeAlign}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="INSIDE">Inside</SelectItem>
                <SelectItem value="CENTER">Center</SelectItem>
                <SelectItem value="OUTSIDE">Outside</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={testSetStrokes} disabled={!nodeId} className="w-full">
            Apply Strokes
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="3. Set Effects" description="Apply drop shadows">
        <div className="space-y-3">
          <div className="space-y-2"><Label>Shadow Color</Label>
            <div className="flex gap-2">
              <Input
                type="color"
                value={shadowColor}
                onChange={(e) => setShadowColor(e.target.value)}
                className="w-20"
              />
              <Input
                value={shadowColor}
                onChange={(e) => setShadowColor(e.target.value)}
                placeholder="#000000"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2"><Label>X Offset</Label>
              <Input
                type="number"
                value={shadowX}
                onChange={(e) => setShadowX(e.target.value)}
              />
            </div>
            <div className="space-y-2"><Label>Y Offset</Label>
              <Input
                type="number"
                value={shadowY}
                onChange={(e) => setShadowY(e.target.value)}
              />
            </div>
          </div>
          <div className="space-y-2"><Label>Blur Radius</Label>
            <Input
              type="number"
              min="0"
              value={shadowBlur}
              onChange={(e) => setShadowBlur(e.target.value)}
            />
          </div>
          <div className="space-y-2"><Label>Opacity</Label>
            <Input
              type="number"
              min="0"
              max="1"
              step="0.05"
              value={shadowOpacity}
              onChange={(e) => setShadowOpacity(e.target.value)}
            />
          </div>
          <Button onClick={testSetEffects} disabled={!nodeId} className="w-full">
            Apply Effects
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="4. Set Opacity" description="Control transparency">
        <div className="space-y-3">
          <div className="space-y-2"><Label>Opacity</Label>
            <Input
              type="number"
              min="0"
              max="1"
              step="0.1"
              value={opacity}
              onChange={(e) => setOpacity(e.target.value)}
            />
          </div>
          <div className="space-y-2"><Label>Blend Mode</Label>
            <Select value={blendMode} onValueChange={setBlendMode}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="NORMAL">Normal</SelectItem>
                <SelectItem value="MULTIPLY">Multiply</SelectItem>
                <SelectItem value="SCREEN">Screen</SelectItem>
                <SelectItem value="OVERLAY">Overlay</SelectItem>
                <SelectItem value="DARKEN">Darken</SelectItem>
                <SelectItem value="LIGHTEN">Lighten</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={testSetOpacity} disabled={!nodeId} className="w-full">
            Apply Opacity
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="5. Set Corner Radius" description="Round corners">
        <div className="space-y-3">
          <div className="space-y-2"><Label>All Corners (Uniform)</Label>
            <Input
              type="number"
              min="0"
              value={cornerRadius}
              onChange={(e) => setCornerRadius(e.target.value)}
              placeholder="8"
            />
          </div>
          <div className="text-sm text-muted-foreground">Or set individual corners:</div>
          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2"><Label>Top Left</Label>
              <Input
                type="number"
                min="0"
                value={topLeftRadius}
                onChange={(e) => setTopLeftRadius(e.target.value)}
                placeholder="Auto"
              />
            </div>
            <div className="space-y-2"><Label>Top Right</Label>
              <Input
                type="number"
                min="0"
                value={topRightRadius}
                onChange={(e) => setTopRightRadius(e.target.value)}
                placeholder="Auto"
              />
            </div>
            <div className="space-y-2"><Label>Bottom Left</Label>
              <Input
                type="number"
                min="0"
                value={bottomLeftRadius}
                onChange={(e) => setBottomLeftRadius(e.target.value)}
                placeholder="Auto"
              />
            </div>
            <div className="space-y-2"><Label>Bottom Right</Label>
              <Input
                type="number"
                min="0"
                value={bottomRightRadius}
                onChange={(e) => setBottomRightRadius(e.target.value)}
                placeholder="Auto"
              />
            </div>
          </div>
          <Button onClick={testSetCornerRadius} disabled={!nodeId} className="w-full">
            Apply Corner Radius
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="6-8. Apply Styles" description="Apply existing style references">
        <div className="space-y-3">
          <div className="space-y-2"><Label>Style Type</Label>
            <Select value={styleType} onValueChange={setStyleType}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="FILL">Fill Style</SelectItem>
                <SelectItem value="STROKE">Stroke Style</SelectItem>
                <SelectItem value="EFFECT">Effect Style</SelectItem>
                <SelectItem value="TEXT">Text Style</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2"><Label>Style ID</Label>
            <Input
              value={styleId}
              onChange={(e) => setStyleId(e.target.value)}
              placeholder="Enter style ID from Figma"
            />
          </div>
          <Button onClick={testApplyStyle} disabled={!nodeId || !styleId} className="w-full">
            Apply Style
          </Button>
        </div>
      </Section>

      <div className="text-xs text-muted-foreground mt-4">
        <p>💡 Tip: Create or select a node in Figma, then click "Get Selected" to auto-fill the Node ID.</p>
      </div>
    </div>
  );
};
