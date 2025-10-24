import React, { useState } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './ui/card';
import { Alert, AlertDescription } from './ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

interface StyleTestPanelProps {
  onSendMessage: (message: any) => void;
}

export const StyleTestPanel: React.FC<StyleTestPanelProps> = ({ onSendMessage }) => {
  // Fill Style Test
  const [fillStyleName, setFillStyleName] = useState('');
  const [fillColor, setFillColor] = useState('#FF0000');

  // Stroke Style Test
  const [strokeStyleName, setStrokeStyleName] = useState('');
  const [strokeColor, setStrokeColor] = useState('#0000FF');
  const [strokeWidth, setStrokeWidth] = useState('2');

  // Text Style Test
  const [textStyleName, setTextStyleName] = useState('');
  const [fontSize, setFontSize] = useState('16');
  const [fontFamily, setFontFamily] = useState('Inter');
  const [fontWeight, setFontWeight] = useState('400');

  // Apply Style Test
  const [styleType, setStyleType] = useState<'fill' | 'stroke' | 'text'>('fill');
  const [styleIdToApply, setStyleIdToApply] = useState('');

  // Effect Style Test
  const [effectStyleName, setEffectStyleName] = useState('');
  const [effectType, setEffectType] = useState<'DROP_SHADOW' | 'INNER_SHADOW' | 'LAYER_BLUR'>('DROP_SHADOW');
  const [shadowColor, setShadowColor] = useState('#000000');
  const [shadowOpacity, setShadowOpacity] = useState('0.25');
  const [shadowX, setShadowX] = useState('0');
  const [shadowY, setShadowY] = useState('4');
  const [shadowBlur, setShadowBlur] = useState('8');

  const [testResult, setTestResult] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  const hexToRgb = (hex: string): { r: number; g: number; b: number } => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16) / 255,
      g: parseInt(result[2], 16) / 255,
      b: parseInt(result[3], 16) / 255,
    } : { r: 1, g: 0, b: 0 };
  };

  const handleCreateFillStyle = async () => {
    setIsLoading(true);
    setTestResult('Creating fill style...');

    const rgb = hexToRgb(fillColor);

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-fill-${Date.now()}`,
        method: 'figma_create_paint_style',
        params: {
          name: fillStyleName || `Test Fill ${Date.now()}`,
          paints: [{
            type: 'SOLID',
            color: rgb,
            opacity: 1
          }]
        }
      }
    });

    setTimeout(() => {
      setTestResult('Fill style creation sent! Check Figma for results.');
      setIsLoading(false);
    }, 500);
  };

  const handleCreateStrokeStyle = async () => {
    setIsLoading(true);
    setTestResult('Creating stroke style...');

    const rgb = hexToRgb(strokeColor);

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-stroke-${Date.now()}`,
        method: 'figma_create_paint_style',
        params: {
          name: strokeStyleName || `Test Stroke ${Date.now()}`,
          paints: [{
            type: 'SOLID',
            color: rgb,
            opacity: 1
          }]
        }
      }
    });

    setTimeout(() => {
      setTestResult('Stroke style creation sent! Check Figma for results.');
      setIsLoading(false);
    }, 500);
  };

  const handleCreateTextStyle = async () => {
    setIsLoading(true);
    setTestResult('Creating text style...');

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-text-${Date.now()}`,
        method: 'figma_create_text_style',
        params: {
          name: textStyleName || `Test Text ${Date.now()}`,
          fontSize: parseFloat(fontSize),
          fontFamily: fontFamily,
          fontWeight: parseInt(fontWeight),
          lineHeight: { value: 1.5, unit: 'AUTO' },
          letterSpacing: { value: 0, unit: 'PIXELS' }
        }
      }
    });

    setTimeout(() => {
      setTestResult('Text style creation sent! Check Figma for results.');
      setIsLoading(false);
    }, 500);
  };

  const handleCreateEffectStyle = async () => {
    setIsLoading(true);
    setTestResult('Creating effect style...');

    const rgb = hexToRgb(shadowColor);

    const effect: any = {
      type: effectType,
      color: { ...rgb, a: parseFloat(shadowOpacity) },
      offset: { x: parseFloat(shadowX), y: parseFloat(shadowY) },
      radius: parseFloat(shadowBlur),
      visible: true,
      blendMode: 'NORMAL'
    };

    if (effectType === 'LAYER_BLUR') {
      delete effect.offset;
      delete effect.color;
    }

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-effect-${Date.now()}`,
        method: 'figma_create_effect_style',
        params: {
          name: effectStyleName || `Test Effect ${Date.now()}`,
          effects: [effect]
        }
      }
    });

    setTimeout(() => {
      setTestResult('Effect style creation sent! Check Figma for results.');
      setIsLoading(false);
    }, 500);
  };

  const handleApplyStyle = async () => {
    setIsLoading(true);
    setTestResult('Applying style to selection...');

    const methodMap = {
      fill: 'figma_set_fill_style_id',
      stroke: 'figma_set_stroke_style_id',
      text: 'figma_set_text_style_id'
    };

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-apply-${Date.now()}`,
        method: methodMap[styleType],
        params: {
          styleId: styleIdToApply
        }
      }
    });

    setTimeout(() => {
      setTestResult(`${styleType} style application sent! Check Figma for results.`);
      setIsLoading(false);
    }, 500);
  };

  const handleGetStyles = async () => {
    setIsLoading(true);
    setTestResult('Fetching styles...');

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-get-styles-${Date.now()}`,
        method: 'figma_get_local_paint_styles',
        params: {}
      }
    });

    setTimeout(() => {
      setTestResult('Style fetch request sent! Check console for results.');
      setIsLoading(false);
    }, 500);
  };

  const handleCreateTestNode = () => {
    setIsLoading(true);
    setTestResult('Creating test rectangle...');

    onSendMessage({
      type: 'ws-command',
      command: {
        id: `test-rect-${Date.now()}`,
        method: 'figma_create_rectangle',
        params: {
          name: 'Test Rectangle for Styles',
          x: 100,
          y: 100,
          width: 200,
          height: 100,
          fills: [{
            type: 'SOLID',
            color: { r: 0.8, g: 0.8, b: 0.8 }
          }]
        }
      }
    });

    setTimeout(() => {
      setTestResult('Test rectangle created! Select it to apply styles.');
      setIsLoading(false);
    }, 500);
  };

  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle className="text-sm">Style Testing Panel</CardTitle>
        <CardDescription className="text-xs">
          Test style creation and application
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <Tabs defaultValue="create-fill" className="w-full">
          <TabsList className="grid w-full grid-cols-3 h-auto">
            <TabsTrigger value="create-fill" className="text-xs py-1">Create Fill</TabsTrigger>
            <TabsTrigger value="create-stroke" className="text-xs py-1">Create Stroke</TabsTrigger>
            <TabsTrigger value="create-text" className="text-xs py-1">Create Text</TabsTrigger>
          </TabsList>

          <TabsContent value="create-fill" className="space-y-3 mt-3">
            <div className="space-y-1">
              <Label className="text-xs font-medium">Style Name</Label>
              <Input
                type="text"
                placeholder="Primary/Red/500"
                value={fillStyleName}
                onChange={(e) => setFillStyleName(e.target.value)}
                className="text-xs h-9"
              />
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Fill Color</Label>
              <div className="flex gap-2">
                <Input
                  type="color"
                  value={fillColor}
                  onChange={(e) => setFillColor(e.target.value)}
                  className="w-20 h-9 p-1"
                />
                <Input
                  type="text"
                  value={fillColor}
                  onChange={(e) => setFillColor(e.target.value)}
                  className="flex-1 text-xs h-9"
                  placeholder="#FF0000"
                />
              </div>
            </div>

            <Button
              onClick={handleCreateFillStyle}
              disabled={isLoading}
              className="w-full text-xs"
            >
              Create Fill Style
            </Button>
          </TabsContent>

          <TabsContent value="create-stroke" className="space-y-3 mt-3">
            <div className="space-y-1">
              <Label className="text-xs font-medium">Style Name</Label>
              <Input
                type="text"
                placeholder="Stroke/Primary/2px"
                value={strokeStyleName}
                onChange={(e) => setStrokeStyleName(e.target.value)}
                className="text-xs h-9"
              />
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Stroke Color</Label>
              <div className="flex gap-2">
                <Input
                  type="color"
                  value={strokeColor}
                  onChange={(e) => setStrokeColor(e.target.value)}
                  className="w-20 h-9 p-1"
                />
                <Input
                  type="text"
                  value={strokeColor}
                  onChange={(e) => setStrokeColor(e.target.value)}
                  className="flex-1 text-xs h-9"
                  placeholder="#0000FF"
                />
              </div>
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Stroke Width</Label>
              <Input
                type="number"
                value={strokeWidth}
                onChange={(e) => setStrokeWidth(e.target.value)}
                className="text-xs h-9"
                min="0"
                step="0.5"
              />
            </div>

            <Button
              onClick={handleCreateStrokeStyle}
              disabled={isLoading}
              className="w-full text-xs"
            >
              Create Stroke Style
            </Button>
          </TabsContent>

          <TabsContent value="create-text" className="space-y-3 mt-3">
            <div className="space-y-1">
              <Label className="text-xs font-medium">Style Name</Label>
              <Input
                type="text"
                placeholder="Text/Heading/H1"
                value={textStyleName}
                onChange={(e) => setTextStyleName(e.target.value)}
                className="text-xs h-9"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div className="space-y-1">
                <Label className="text-xs font-medium">Font Size</Label>
                <Input
                  type="number"
                  value={fontSize}
                  onChange={(e) => setFontSize(e.target.value)}
                  className="text-xs h-9"
                  min="1"
                />
              </div>

              <div className="space-y-1">
                <Label className="text-xs font-medium">Font Weight</Label>
                <Select value={fontWeight} onValueChange={setFontWeight}>
                  <SelectTrigger className="text-xs h-9">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="100">Thin (100)</SelectItem>
                    <SelectItem value="200">Extra Light (200)</SelectItem>
                    <SelectItem value="300">Light (300)</SelectItem>
                    <SelectItem value="400">Regular (400)</SelectItem>
                    <SelectItem value="500">Medium (500)</SelectItem>
                    <SelectItem value="600">Semi Bold (600)</SelectItem>
                    <SelectItem value="700">Bold (700)</SelectItem>
                    <SelectItem value="800">Extra Bold (800)</SelectItem>
                    <SelectItem value="900">Black (900)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Font Family</Label>
              <Input
                type="text"
                value={fontFamily}
                onChange={(e) => setFontFamily(e.target.value)}
                className="text-xs h-9"
                placeholder="Inter"
              />
            </div>

            <Button
              onClick={handleCreateTextStyle}
              disabled={isLoading}
              className="w-full text-xs"
            >
              Create Text Style
            </Button>
          </TabsContent>
        </Tabs>

        <Tabs defaultValue="apply" className="w-full">
          <TabsList className="grid w-full grid-cols-2 h-auto">
            <TabsTrigger value="apply" className="text-xs py-1">Apply Style</TabsTrigger>
            <TabsTrigger value="effects" className="text-xs py-1">Create Effect</TabsTrigger>
          </TabsList>

          <TabsContent value="apply" className="space-y-3 mt-3">
            <Alert>
              <AlertDescription className="text-xs">
                Select a node in Figma first, then apply a style by ID
              </AlertDescription>
            </Alert>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Style Type</Label>
              <Select value={styleType} onValueChange={(value: any) => setStyleType(value)}>
                <SelectTrigger className="text-xs h-9">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="fill">Fill Style</SelectItem>
                  <SelectItem value="stroke">Stroke Style</SelectItem>
                  <SelectItem value="text">Text Style</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Style ID</Label>
              <Input
                type="text"
                placeholder="S:abc123..."
                value={styleIdToApply}
                onChange={(e) => setStyleIdToApply(e.target.value)}
                className="text-xs h-9"
              />
            </div>

            <div className="flex gap-2">
              <Button
                onClick={handleApplyStyle}
                disabled={isLoading || !styleIdToApply}
                className="flex-1 text-xs"
              >
                Apply to Selection
              </Button>
              <Button
                onClick={handleGetStyles}
                disabled={isLoading}
                variant="outline"
                className="flex-1 text-xs"
              >
                Get All Styles
              </Button>
            </div>
          </TabsContent>

          <TabsContent value="effects" className="space-y-3 mt-3">
            <div className="space-y-1">
              <Label className="text-xs font-medium">Effect Style Name</Label>
              <Input
                type="text"
                placeholder="Shadow/Elevation/1"
                value={effectStyleName}
                onChange={(e) => setEffectStyleName(e.target.value)}
                className="text-xs h-9"
              />
            </div>

            <div className="space-y-1">
              <Label className="text-xs font-medium">Effect Type</Label>
              <Select value={effectType} onValueChange={(value: any) => setEffectType(value)}>
                <SelectTrigger className="text-xs h-9">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DROP_SHADOW">Drop Shadow</SelectItem>
                  <SelectItem value="INNER_SHADOW">Inner Shadow</SelectItem>
                  <SelectItem value="LAYER_BLUR">Layer Blur</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {effectType !== 'LAYER_BLUR' && (
              <>
                <div className="space-y-1">
                  <Label className="text-xs font-medium">Shadow Color & Opacity</Label>
                  <div className="flex gap-2">
                    <Input
                      type="color"
                      value={shadowColor}
                      onChange={(e) => setShadowColor(e.target.value)}
                      className="w-20 h-9 p-1"
                    />
                    <Input
                      type="number"
                      value={shadowOpacity}
                      onChange={(e) => setShadowOpacity(e.target.value)}
                      className="flex-1 text-xs h-9"
                      placeholder="Opacity"
                      min="0"
                      max="1"
                      step="0.05"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-2">
                  <div className="space-y-1">
                    <Label className="text-xs font-medium">X Offset</Label>
                    <Input
                      type="number"
                      value={shadowX}
                      onChange={(e) => setShadowX(e.target.value)}
                      className="text-xs h-9"
                      step="1"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label className="text-xs font-medium">Y Offset</Label>
                    <Input
                      type="number"
                      value={shadowY}
                      onChange={(e) => setShadowY(e.target.value)}
                      className="text-xs h-9"
                      step="1"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label className="text-xs font-medium">Blur</Label>
                    <Input
                      type="number"
                      value={shadowBlur}
                      onChange={(e) => setShadowBlur(e.target.value)}
                      className="text-xs h-9"
                      min="0"
                      step="1"
                    />
                  </div>
                </div>
              </>
            )}

            {effectType === 'LAYER_BLUR' && (
              <div className="space-y-1">
                <Label className="text-xs font-medium">Blur Radius</Label>
                <Input
                  type="number"
                  value={shadowBlur}
                  onChange={(e) => setShadowBlur(e.target.value)}
                  className="text-xs h-9"
                  min="0"
                  step="1"
                />
              </div>
            )}

            <Button
              onClick={handleCreateEffectStyle}
              disabled={isLoading}
              className="w-full text-xs"
            >
              Create Effect Style
            </Button>
          </TabsContent>
        </Tabs>

        <div className="pt-2 border-t">
          <Button
            onClick={handleCreateTestNode}
            disabled={isLoading}
            variant="outline"
            className="w-full text-xs"
          >
            Create Test Rectangle
          </Button>
        </div>

        {testResult && (
          <Alert>
            <AlertDescription className="text-xs">
              {testResult}
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  );
};
