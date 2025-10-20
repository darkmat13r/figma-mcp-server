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

interface ImageTestPanelProps {
  onSendCommand: (command: any) => void;
  isConnected: boolean;
}

export const ImageTestPanel: React.FC<ImageTestPanelProps> = ({ onSendCommand, isConnected }) => {
  // State for test results
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState('');

  // Create Image - Section 1
  const [createImageData, setCreateImageData] = useState('https://picsum.photos/200/300');
  const [createWidth, setCreateWidth] = useState('');
  const [createHeight, setCreateHeight] = useState('');
  const [createX, setCreateX] = useState('');
  const [createY, setCreateY] = useState('');
  const [createdImageId, setCreatedImageId] = useState('');

  // Set Image Fill - Section 2
  const [fillNodeId, setFillNodeId] = useState('');
  const [fillImageData, setFillImageData] = useState('https://picsum.photos/400/300');
  const [fillScaleMode, setFillScaleMode] = useState('FILL');
  const [fillRotation, setFillRotation] = useState('');
  const [fillOpacity, setFillOpacity] = useState('');

  // Export Node - Section 3
  const [exportNodeId, setExportNodeId] = useState('');
  const [exportFormat, setExportFormat] = useState('PNG');
  const [exportScale, setExportScale] = useState('1');
  const [exportConstraintType, setExportConstraintType] = useState('');
  const [exportConstraintValue, setExportConstraintValue] = useState('');
  const [exportedImageData, setExportedImageData] = useState('');
  const [exportedFormat, setExportedFormat] = useState('');

  // Get Image Fills - Section 4
  const [getImageNodeId, setGetImageNodeId] = useState('');
  const [imageFillsData, setImageFillsData] = useState<any>(null);

  const sendCommand = (method: string, params: any) => {
    const command = {
      id: `test_${Date.now()}`,
      method,
      params
    };

    parent.postMessage({ pluginMessage: { type: 'ws-command', command } }, '*');
    setTestResult({ success: true, message: `Sent ${method} command to plugin` });
    setTimeout(() => setTestResult(null), 3000);
  };

  // Listen for selection changes and command responses
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const message = event.data.pluginMessage;

      if (message && message.type === 'selection-changed') {
        const selection = message.selection as SelectionInfo[];
        if (selection && selection.length > 0) {
          setSelectedNodeId(selection[0].id);
          setTestResult({
            success: true,
            message: `Selected: ${selection[0].name} (${selection[0].type})`
          });
          setTimeout(() => setTestResult(null), 3000);
        } else {
          setTestResult({
            success: false,
            message: 'No node selected in Figma'
          });
          setTimeout(() => setTestResult(null), 3000);
        }
      }

      // Handle command responses
      if (message && message.type === 'ws-response') {
        const response = message.response;

        // Check if this is a createImage response
        if (response && response.result && response.result.imageNodeId) {
          setCreatedImageId(response.result.imageNodeId);
          setTestResult({
            success: true,
            message: `Image created with ID: ${response.result.imageNodeId}`
          });
          setTimeout(() => setTestResult(null), 3000);
        }

        // Check if this is an exportNode response
        if (response && response.result && response.result.imageData) {
          setExportedImageData(response.result.imageData);
          setExportedFormat(response.result.format || exportFormat);
          setTestResult({
            success: true,
            message: `Node exported as ${response.result.format || exportFormat}`
          });
          setTimeout(() => setTestResult(null), 3000);
        }

        // Check if this is a getImageFills response
        if (response && response.result && response.result.images !== undefined) {
          setImageFillsData(response.result);
          setTestResult({
            success: true,
            message: `Retrieved ${response.result.images.length} image fill(s)`
          });
          setTimeout(() => setTestResult(null), 3000);
        }
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [exportFormat]);

  const getSelectedNode = () => {
    parent.postMessage({ pluginMessage: { type: 'get-selection' } }, '*');
  };

  const useSelectedForFill = () => {
    if (selectedNodeId) {
      setFillNodeId(selectedNodeId);
      setTestResult({ success: true, message: 'Node ID copied to Set Image Fill' });
      setTimeout(() => setTestResult(null), 2000);
    }
  };

  const useSelectedForExport = () => {
    if (selectedNodeId) {
      setExportNodeId(selectedNodeId);
      setTestResult({ success: true, message: 'Node ID copied to Export Node' });
      setTimeout(() => setTestResult(null), 2000);
    }
  };

  const useSelectedForGetFills = () => {
    if (selectedNodeId) {
      setGetImageNodeId(selectedNodeId);
      setTestResult({ success: true, message: 'Node ID copied to Get Image Fills' });
      setTimeout(() => setTestResult(null), 2000);
    }
  };

  // Section 1: Create Image
  const testCreateImage = () => {
    const params: any = {
      imageData: createImageData
    };

    if (createWidth) params.width = parseFloat(createWidth);
    if (createHeight) params.height = parseFloat(createHeight);
    if (createX) params.x = parseFloat(createX);
    if (createY) params.y = parseFloat(createY);

    sendCommand('figma_create_image', params);
  };

  // Section 2: Set Image Fill
  const testSetImageFill = () => {
    const params: any = {
      nodeId: fillNodeId,
      imageData: fillImageData,
      scaleMode: fillScaleMode
    };

    if (fillRotation) params.rotation = parseFloat(fillRotation);
    if (fillOpacity) params.opacity = parseFloat(fillOpacity);

    sendCommand('figma_set_image_fill', params);
  };

  // Section 3: Export Node
  const testExportNode = () => {
    const params: any = {
      nodeId: exportNodeId,
      format: exportFormat
    };

    if (exportScale) params.scale = parseFloat(exportScale);

    if (exportConstraintType && exportConstraintType !== 'NONE' && exportConstraintValue) {
      params.constraint = {
        type: exportConstraintType,
        value: parseFloat(exportConstraintValue)
      };
    }

    sendCommand('figma_export_node', params);
  };

  // Section 4: Get Image Fills
  const testGetImageFills = () => {
    sendCommand('figma_get_image_fills', {
      nodeId: getImageNodeId
    });
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setTestResult({ success: true, message: `${label} copied to clipboard` });
      setTimeout(() => setTestResult(null), 2000);
    });
  };

  const downloadImage = () => {
    if (!exportedImageData) return;

    const mimeTypes: { [key: string]: string } = {
      PNG: 'image/png',
      JPG: 'image/jpeg',
      SVG: 'image/svg+xml',
      PDF: 'application/pdf'
    };

    const mimeType = mimeTypes[exportedFormat] || 'image/png';
    const extension = exportedFormat.toLowerCase();

    const link = document.createElement('a');
    link.href = `data:${mimeType};base64,${exportedImageData}`;
    link.download = `figma-export-${Date.now()}.${extension}`;
    link.click();

    setTestResult({ success: true, message: 'Image downloaded' });
    setTimeout(() => setTestResult(null), 2000);
  };

  return (
    <div className="space-y-4 p-4">
      {testResult && (
        <Alert variant={testResult.success ? 'default' : 'destructive'}>
          <AlertDescription>{testResult.message}</AlertDescription>
        </Alert>
      )}

      <Section title="Node Selection Helper">
        <div className="space-y-2">
          <Label>Selected Node ID</Label>
          <div className="flex gap-2">
            <Input
              value={selectedNodeId}
              onChange={(e) => setSelectedNodeId(e.target.value)}
              placeholder="Click 'Get Selected' to fetch"
              readOnly
            />
            <Button onClick={getSelectedNode} size="sm">
              Get Selected
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Select a node in Figma, then click "Get Selected" to auto-fill node IDs below
          </p>
        </div>
      </Section>

      <Separator />

      <Section title="1. Create Image" description="Create an image node from URL or base64 data">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Image Data <span className="text-destructive">*</span></Label>
            <Input
              value={createImageData}
              onChange={(e) => setCreateImageData(e.target.value)}
              placeholder="https://example.com/image.png or data:image/png;base64,..."
            />
            <p className="text-xs text-muted-foreground">
              URL or base64 encoded image data
            </p>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>Width (optional)</Label>
              <Input
                type="number"
                min="1"
                value={createWidth}
                onChange={(e) => setCreateWidth(e.target.value)}
                placeholder="Auto"
              />
            </div>
            <div className="space-y-2">
              <Label>Height (optional)</Label>
              <Input
                type="number"
                min="1"
                value={createHeight}
                onChange={(e) => setCreateHeight(e.target.value)}
                placeholder="Auto"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>X Position</Label>
              <Input
                type="number"
                value={createX}
                onChange={(e) => setCreateX(e.target.value)}
                placeholder="0"
              />
            </div>
            <div className="space-y-2">
              <Label>Y Position</Label>
              <Input
                type="number"
                value={createY}
                onChange={(e) => setCreateY(e.target.value)}
                placeholder="0"
              />
            </div>
          </div>

          <Button onClick={testCreateImage} disabled={!createImageData} className="w-full">
            Create Image
          </Button>

          {createdImageId && (
            <Card className="p-3 bg-muted">
              <p className="text-sm font-medium mb-1">Created Image ID:</p>
              <div className="flex gap-2">
                <code className="text-xs bg-background px-2 py-1 rounded flex-1 overflow-x-auto">
                  {createdImageId}
                </code>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => copyToClipboard(createdImageId, 'Image ID')}
                >
                  Copy
                </Button>
              </div>
            </Card>
          )}
        </div>
      </Section>

      <Separator />

      <Section title="2. Set Image Fill" description="Apply an image as a fill to an existing node">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Node ID <span className="text-destructive">*</span></Label>
            <div className="flex gap-2">
              <Input
                value={fillNodeId}
                onChange={(e) => setFillNodeId(e.target.value)}
                placeholder="Enter node ID"
              />
              <Button onClick={useSelectedForFill} size="sm" disabled={!selectedNodeId}>
                Use Selected
              </Button>
            </div>
          </div>

          <div className="space-y-2">
            <Label>Image Data <span className="text-destructive">*</span></Label>
            <Input
              value={fillImageData}
              onChange={(e) => setFillImageData(e.target.value)}
              placeholder="https://example.com/image.png or base64"
            />
          </div>

          <div className="space-y-2">
            <Label>Scale Mode</Label>
            <Select value={fillScaleMode} onValueChange={setFillScaleMode}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="FILL">Fill (stretch to fill)</SelectItem>
                <SelectItem value="FIT">Fit (contain, maintain aspect)</SelectItem>
                <SelectItem value="CROP">Crop (cover, maintain aspect)</SelectItem>
                <SelectItem value="TILE">Tile (repeat pattern)</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>Rotation (degrees)</Label>
              <Input
                type="number"
                min="0"
                max="360"
                value={fillRotation}
                onChange={(e) => setFillRotation(e.target.value)}
                placeholder="0"
              />
            </div>
            <div className="space-y-2">
              <Label>Opacity</Label>
              <Input
                type="number"
                min="0"
                max="1"
                step="0.1"
                value={fillOpacity}
                onChange={(e) => setFillOpacity(e.target.value)}
                placeholder="1"
              />
            </div>
          </div>

          <Button onClick={testSetImageFill} disabled={!fillNodeId || !fillImageData} className="w-full">
            Apply Image Fill
          </Button>
        </div>
      </Section>

      <Separator />

      <Section title="3. Export Node" description="Export a node as an image file">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Node ID <span className="text-destructive">*</span></Label>
            <div className="flex gap-2">
              <Input
                value={exportNodeId}
                onChange={(e) => setExportNodeId(e.target.value)}
                placeholder="Enter node ID"
              />
              <Button onClick={useSelectedForExport} size="sm" disabled={!selectedNodeId}>
                Use Selected
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="space-y-2">
              <Label>Format <span className="text-destructive">*</span></Label>
              <Select value={exportFormat} onValueChange={setExportFormat}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PNG">PNG</SelectItem>
                  <SelectItem value="JPG">JPG</SelectItem>
                  <SelectItem value="SVG">SVG</SelectItem>
                  <SelectItem value="PDF">PDF</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Scale</Label>
              <Input
                type="number"
                min="1"
                max="4"
                step="0.5"
                value={exportScale}
                onChange={(e) => setExportScale(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Constraint (optional)</Label>
            <div className="grid grid-cols-2 gap-2">
              <Select value={exportConstraintType} onValueChange={setExportConstraintType}>
                <SelectTrigger>
                  <SelectValue placeholder="Constraint Type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">None</SelectItem>
                  <SelectItem value="SCALE">Scale</SelectItem>
                  <SelectItem value="WIDTH">Width</SelectItem>
                  <SelectItem value="HEIGHT">Height</SelectItem>
                </SelectContent>
              </Select>
              <Input
                type="number"
                value={exportConstraintValue}
                onChange={(e) => setExportConstraintValue(e.target.value)}
                placeholder="Value"
                disabled={!exportConstraintType || exportConstraintType === 'NONE'}
              />
            </div>
          </div>

          <Button onClick={testExportNode} disabled={!exportNodeId} className="w-full">
            Export Node
          </Button>

          {exportedImageData && (
            <Card className="p-3 bg-muted space-y-3">
              <div>
                <p className="text-sm font-medium mb-2">Export Result ({exportedFormat}):</p>

                {(exportedFormat === 'PNG' || exportedFormat === 'JPG') && (
                  <div className="mb-3 border rounded overflow-hidden bg-background">
                    <img
                      src={`data:image/${exportedFormat.toLowerCase()};base64,${exportedImageData}`}
                      alt="Exported preview"
                      className="max-w-full h-auto max-h-48 mx-auto"
                    />
                  </div>
                )}

                <div className="flex gap-2 mb-2">
                  <Button size="sm" onClick={downloadImage} className="flex-1">
                    Download Image
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => copyToClipboard(exportedImageData, 'Base64 data')}
                    className="flex-1"
                  >
                    Copy Base64
                  </Button>
                </div>

                <details className="text-xs">
                  <summary className="cursor-pointer text-muted-foreground hover:text-foreground">
                    View base64 data ({exportedImageData.length} chars)
                  </summary>
                  <pre className="mt-2 p-2 bg-background rounded text-xs overflow-x-auto max-h-32">
                    {exportedImageData.substring(0, 500)}...
                  </pre>
                </details>
              </div>
            </Card>
          )}
        </div>
      </Section>

      <Separator />

      <Section title="4. Get Image Fills" description="Retrieve image fill information from a node">
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Node ID <span className="text-destructive">*</span></Label>
            <div className="flex gap-2">
              <Input
                value={getImageNodeId}
                onChange={(e) => setGetImageNodeId(e.target.value)}
                placeholder="Enter node ID"
              />
              <Button onClick={useSelectedForGetFills} size="sm" disabled={!selectedNodeId}>
                Use Selected
              </Button>
            </div>
          </div>

          <Button onClick={testGetImageFills} disabled={!getImageNodeId} className="w-full">
            Get Image Fills
          </Button>

          {imageFillsData && (
            <Card className="p-3 bg-muted">
              <p className="text-sm font-medium mb-2">
                Image Fills ({imageFillsData.images?.length || 0}):
              </p>
              {imageFillsData.images && imageFillsData.images.length > 0 ? (
                <div className="space-y-2">
                  {imageFillsData.images.map((fill: any, index: number) => (
                    <div key={index} className="p-2 bg-background rounded text-xs space-y-1">
                      <div className="flex justify-between">
                        <span className="font-medium">Fill #{index + 1}</span>
                        <span className="text-muted-foreground">{fill.type}</span>
                      </div>
                      {fill.scaleMode && (
                        <div>
                          <span className="text-muted-foreground">Scale Mode:</span> {fill.scaleMode}
                        </div>
                      )}
                      {fill.imageRef && (
                        <div>
                          <span className="text-muted-foreground">Image Ref:</span>
                          <code className="ml-1 text-xs">{fill.imageRef}</code>
                        </div>
                      )}
                      {fill.opacity !== undefined && (
                        <div>
                          <span className="text-muted-foreground">Opacity:</span> {fill.opacity}
                        </div>
                      )}
                      {fill.blendMode && (
                        <div>
                          <span className="text-muted-foreground">Blend Mode:</span> {fill.blendMode}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-muted-foreground">No image fills found on this node</p>
              )}

              <details className="mt-3">
                <summary className="cursor-pointer text-xs text-muted-foreground hover:text-foreground">
                  View raw JSON
                </summary>
                <pre className="mt-2 p-2 bg-background rounded text-xs overflow-x-auto max-h-48">
                  {JSON.stringify(imageFillsData, null, 2)}
                </pre>
              </details>
            </Card>
          )}
        </div>
      </Section>

      <div className="text-xs text-muted-foreground mt-4 space-y-1">
        <p>Examples:</p>
        <p>- Image URL: https://picsum.photos/200/300</p>
        <p>- Base64: data:image/png;base64,iVBORw0KG...</p>
        <p>
          Tip: Use the Node Selection Helper to easily populate node IDs from your Figma selection
        </p>
      </div>
    </div>
  );
};
