import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { Section } from './composed';

interface SelectionInfo {
  id: string;
  name: string;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

interface TypographyTestPanelProps {
  onSendCommand: (command: any) => void;
  isConnected: boolean;
}

export const TypographyTestPanel: React.FC<TypographyTestPanelProps> = ({ onSendCommand, isConnected }) => {
  // State for test results
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // State for node selection
  const [nodeId, setNodeId] = useState('');

  // Text Content (4.1)
  const [textContent, setTextContent] = useState('Hello World');

  // Text Style (4.2)
  const [fontFamily, setFontFamily] = useState('Inter');
  const [fontStyle, setFontStyle] = useState('Regular');
  const [fontSize, setFontSize] = useState('16');
  const [fontWeight, setFontWeight] = useState('400');
  const [lineHeightValue, setLineHeightValue] = useState('');
  const [lineHeightUnit, setLineHeightUnit] = useState('AUTO');
  const [letterSpacingValue, setLetterSpacingValue] = useState('0');
  const [letterSpacingUnit, setLetterSpacingUnit] = useState('PERCENT');
  const [textCase, setTextCase] = useState('ORIGINAL');
  const [textDecoration, setTextDecoration] = useState('NONE');

  // Text Alignment (4.3)
  const [textAlignHorizontal, setTextAlignHorizontal] = useState('LEFT');
  const [textAlignVertical, setTextAlignVertical] = useState('TOP');

  // Text Auto Resize (4.4)
  const [textAutoResize, setTextAutoResize] = useState('WIDTH_AND_HEIGHT');

  // Text Truncation (4.5)
  const [textTruncation, setTextTruncation] = useState('DISABLED');
  const [maxLines, setMaxLines] = useState('');

  // Load Font (4.6)
  const [loadFontFamily, setLoadFontFamily] = useState('Inter');
  const [loadFontStyle, setLoadFontStyle] = useState('Bold');

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

  // Tool 4.1: Set Text Content
  const testSetTextContent = () => {
    sendCommand('setTextContent', {
      nodeId,
      characters: textContent
    });
  };

  // Tool 4.2: Set Text Style
  const testSetTextStyle = () => {
    const params: any = { nodeId };

    if (fontFamily) params.fontFamily = fontFamily;
    if (fontStyle) params.fontStyle = fontStyle;
    if (fontSize) params.fontSize = parseFloat(fontSize);
    if (fontWeight) params.fontWeight = parseInt(fontWeight);

    // Line height
    if (lineHeightUnit === 'AUTO') {
      params.lineHeight = { unit: 'AUTO' };
    } else if (lineHeightValue) {
      params.lineHeight = {
        unit: lineHeightUnit,
        value: parseFloat(lineHeightValue)
      };
    }

    // Letter spacing
    if (letterSpacingValue) {
      params.letterSpacing = {
        unit: letterSpacingUnit,
        value: parseFloat(letterSpacingValue)
      };
    }

    if (textCase !== 'ORIGINAL') params.textCase = textCase;
    if (textDecoration !== 'NONE') params.textDecoration = textDecoration;

    sendCommand('setTextStyle', params);
  };

  // Tool 4.3: Set Text Alignment
  const testSetTextAlignment = () => {
    sendCommand('setTextAlignment', {
      nodeId,
      textAlignHorizontal,
      textAlignVertical
    });
  };

  // Tool 4.4: Set Text Auto Resize
  const testSetTextAutoResize = () => {
    sendCommand('setTextAutoResize', {
      nodeId,
      textAutoResize
    });
  };

  // Tool 4.5: Set Text Truncation
  const testSetTextTruncation = () => {
    const params: any = {
      nodeId,
      textTruncation
    };

    if (maxLines && textTruncation === 'ENDING') {
      params.maxLines = parseInt(maxLines);
    }

    sendCommand('setTextTruncation', params);
  };

  // Tool 4.6: Load Font
  const testLoadFont = () => {
    sendCommand('loadFont', {
      fontFamily: loadFontFamily,
      fontStyle: loadFontStyle
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
          <p className="text-xs text-muted-foreground">
            Select a TEXT node in Figma to test typography tools
          </p>
        </div>
      </Section>

      <Separator />

      <Section title="1. Set Text Content" description="Change text content (Tool 4.1)">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Text Content</Label>
            <Input
              value={textContent}
              onChange={(e) => setTextContent(e.target.value)}
              placeholder="Enter text content"
            />
          </div>
          <Button onClick={testSetTextContent} disabled={!nodeId} className="w-full">
            Set Text Content
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="2. Set Text Style" description="Font, size, weight, spacing (Tool 4.2)">
        <div className="space-y-3">
          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>Font Family</Label>
              <Input
                value={fontFamily}
                onChange={(e) => setFontFamily(e.target.value)}
                placeholder="Inter"
              />
            </div>
            <div className="space-y-2">
              <Label>Font Style</Label>
              <Select value={fontStyle} onValueChange={setFontStyle}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Regular">Regular</SelectItem>
                  <SelectItem value="Medium">Medium</SelectItem>
                  <SelectItem value="SemiBold">SemiBold</SelectItem>
                  <SelectItem value="Bold">Bold</SelectItem>
                  <SelectItem value="Italic">Italic</SelectItem>
                  <SelectItem value="Light">Light</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>Font Size (px)</Label>
              <Input
                type="number"
                value={fontSize}
                onChange={(e) => setFontSize(e.target.value)}
                placeholder="16"
              />
            </div>
            <div className="space-y-2">
              <Label>Font Weight</Label>
              <Select value={fontWeight} onValueChange={setFontWeight}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="100">100 - Thin</SelectItem>
                  <SelectItem value="200">200 - ExtraLight</SelectItem>
                  <SelectItem value="300">300 - Light</SelectItem>
                  <SelectItem value="400">400 - Regular</SelectItem>
                  <SelectItem value="500">500 - Medium</SelectItem>
                  <SelectItem value="600">600 - SemiBold</SelectItem>
                  <SelectItem value="700">700 - Bold</SelectItem>
                  <SelectItem value="800">800 - ExtraBold</SelectItem>
                  <SelectItem value="900">900 - Black</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label>Line Height</Label>
            <div className="grid grid-cols-2 gap-2">
              <Select value={lineHeightUnit} onValueChange={setLineHeightUnit}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="AUTO">Auto</SelectItem>
                  <SelectItem value="PIXELS">Pixels</SelectItem>
                  <SelectItem value="PERCENT">Percent</SelectItem>
                </SelectContent>
              </Select>
              <Input
                type="number"
                value={lineHeightValue}
                onChange={(e) => setLineHeightValue(e.target.value)}
                placeholder={lineHeightUnit === 'AUTO' ? 'Auto' : 'Value'}
                disabled={lineHeightUnit === 'AUTO'}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Letter Spacing</Label>
            <div className="grid grid-cols-2 gap-2">
              <Select value={letterSpacingUnit} onValueChange={setLetterSpacingUnit}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PERCENT">Percent</SelectItem>
                  <SelectItem value="PIXELS">Pixels</SelectItem>
                </SelectContent>
              </Select>
              <Input
                type="number"
                value={letterSpacingValue}
                onChange={(e) => setLetterSpacingValue(e.target.value)}
                placeholder="0"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>Text Case</Label>
              <Select value={textCase} onValueChange={setTextCase}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ORIGINAL">Original</SelectItem>
                  <SelectItem value="UPPER">UPPERCASE</SelectItem>
                  <SelectItem value="LOWER">lowercase</SelectItem>
                  <SelectItem value="TITLE">Title Case</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Text Decoration</Label>
              <Select value={textDecoration} onValueChange={setTextDecoration}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">None</SelectItem>
                  <SelectItem value="UNDERLINE">Underline</SelectItem>
                  <SelectItem value="STRIKETHROUGH">Strikethrough</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <Button onClick={testSetTextStyle} disabled={!nodeId} className="w-full">
            Apply Text Style
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="3. Set Text Alignment" description="Horizontal and vertical alignment (Tool 4.3)">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Horizontal Alignment</Label>
            <Select value={textAlignHorizontal} onValueChange={setTextAlignHorizontal}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="LEFT">Left</SelectItem>
                <SelectItem value="CENTER">Center</SelectItem>
                <SelectItem value="RIGHT">Right</SelectItem>
                <SelectItem value="JUSTIFIED">Justified</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-2">
            <Label>Vertical Alignment</Label>
            <Select value={textAlignVertical} onValueChange={setTextAlignVertical}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="TOP">Top</SelectItem>
                <SelectItem value="CENTER">Center</SelectItem>
                <SelectItem value="BOTTOM">Bottom</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={testSetTextAlignment} disabled={!nodeId} className="w-full">
            Apply Alignment
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="4. Set Text Auto Resize" description="Control text box sizing (Tool 4.4)">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Auto Resize Mode</Label>
            <Select value={textAutoResize} onValueChange={setTextAutoResize}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="NONE">None (Fixed size)</SelectItem>
                <SelectItem value="HEIGHT">Auto Height</SelectItem>
                <SelectItem value="WIDTH_AND_HEIGHT">Auto Width & Height</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={testSetTextAutoResize} disabled={!nodeId} className="w-full">
            Apply Auto Resize
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="5. Set Text Truncation" description="Control text overflow (Tool 4.5)">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Truncation Mode</Label>
            <Select value={textTruncation} onValueChange={setTextTruncation}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="DISABLED">Disabled</SelectItem>
                <SelectItem value="ENDING">Ending (with ...)</SelectItem>
              </SelectContent>
            </Select>
          </div>
          {textTruncation === 'ENDING' && (
            <div className="space-y-2">
              <Label>Max Lines</Label>
              <Input
                type="number"
                value={maxLines}
                onChange={(e) => setMaxLines(e.target.value)}
                placeholder="Leave empty for no limit"
                min="1"
              />
            </div>
          )}
          <Button onClick={testSetTextTruncation} disabled={!nodeId} className="w-full">
            Apply Truncation
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="6. Load Font" description="Preload fonts for use (Tool 4.6)">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Font Family</Label>
            <Input
              value={loadFontFamily}
              onChange={(e) => setLoadFontFamily(e.target.value)}
              placeholder="Inter"
            />
          </div>
          <div className="space-y-2">
            <Label>Font Style</Label>
            <Select value={loadFontStyle} onValueChange={setLoadFontStyle}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="Regular">Regular</SelectItem>
                <SelectItem value="Medium">Medium</SelectItem>
                <SelectItem value="SemiBold">SemiBold</SelectItem>
                <SelectItem value="Bold">Bold</SelectItem>
                <SelectItem value="Italic">Italic</SelectItem>
                <SelectItem value="Light">Light</SelectItem>
                <SelectItem value="Thin">Thin</SelectItem>
                <SelectItem value="ExtraLight">ExtraLight</SelectItem>
                <SelectItem value="ExtraBold">ExtraBold</SelectItem>
                <SelectItem value="Black">Black</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={testLoadFont} className="w-full">
            Load Font
          </Button>
          <p className="text-xs text-muted-foreground">
            This checks if a font is available and loads it for use
          </p>
        </div>
      </Section>

      <div className="text-xs text-muted-foreground mt-4">
        <p>💡 Tip: Create or select a TEXT node in Figma, then click "Get Selected" to auto-fill the Node ID.</p>
        <p className="mt-1">⚠️ Note: All tools except "Load Font" require a TEXT node to be selected.</p>
      </div>
    </div>
  );
};
