import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { Section } from './composed';
import { Card } from './ui/card';

interface SelectionInfo {
  id: string;
  name: string;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

interface VariablesTestPanelProps {
  onSendCommand: (command: any) => void;
  isConnected: boolean;
}

export const VariablesTestPanel: React.FC<VariablesTestPanelProps> = ({ onSendCommand, isConnected }) => {
  // State for test results
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [resultsData, setResultsData] = useState<string | null>(null);

  // State for node selection
  const [nodeId, setNodeId] = useState('');

  // Section 1: Collection Management
  // Tool 1: Create Variable Collection
  const [collectionName, setCollectionName] = useState('');
  const [collectionModes, setCollectionModes] = useState('');

  // Tool 2: Get Variables
  const [getVarsCollectionId, setGetVarsCollectionId] = useState('');
  const [getVarsType, setGetVarsType] = useState('');

  // Section 2: Variable Creation
  // Tool 3: Create Variable
  const [variableName, setVariableName] = useState('');
  const [variableCollectionId, setVariableCollectionId] = useState('');
  const [variableType, setVariableType] = useState('STRING');
  const [variableValue, setVariableValue] = useState('');
  // For COLOR type
  const [colorR, setColorR] = useState('1');
  const [colorG, setColorG] = useState('0');
  const [colorB, setColorB] = useState('0');
  const [colorA, setColorA] = useState('1');
  const [colorHex, setColorHex] = useState('#FF0000');

  // Section 3: Variable Binding
  // Tool 4: Bind Variable
  const [bindNodeId, setBindNodeId] = useState('');
  const [bindField, setBindField] = useState('fills');
  const [bindVariableId, setBindVariableId] = useState('');

  // Tool 5: Unbind Variable
  const [unbindNodeId, setUnbindNodeId] = useState('');
  const [unbindField, setUnbindField] = useState('fills');

  // Section 4: Variable Values
  // Tool 6: Set Variable Value
  const [setValueVariableId, setSetValueVariableId] = useState('');
  const [setValueValue, setSetValueValue] = useState('');
  const [setValueModeId, setSetValueModeId] = useState('');
  const [setValueType, setSetValueType] = useState('STRING');
  // For COLOR type in setValue
  const [setValueColorR, setSetValueColorR] = useState('1');
  const [setValueColorG, setSetValueColorG] = useState('0');
  const [setValueColorB, setSetValueColorB] = useState('0');
  const [setValueColorA, setSetValueColorA] = useState('1');
  const [setValueColorHex, setSetValueColorHex] = useState('#FF0000');

  const hexToRgb = (hex: string): { r: number; g: number; b: number } => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16) / 255,
      g: parseInt(result[2], 16) / 255,
      b: parseInt(result[3], 16) / 255
    } : { r: 1, g: 0, b: 0 };
  };

  // Update RGB when hex changes
  useEffect(() => {
    const rgb = hexToRgb(colorHex);
    setColorR(rgb.r.toFixed(3));
    setColorG(rgb.g.toFixed(3));
    setColorB(rgb.b.toFixed(3));
  }, [colorHex]);

  useEffect(() => {
    const rgb = hexToRgb(setValueColorHex);
    setSetValueColorR(rgb.r.toFixed(3));
    setSetValueColorG(rgb.g.toFixed(3));
    setSetValueColorB(rgb.b.toFixed(3));
  }, [setValueColorHex]);

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

  // Listen for responses from plugin
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const message = event.data.pluginMessage;

      if (message && message.type === 'selection-changed') {
        const selection = message.selection as SelectionInfo[];
        if (selection && selection.length > 0) {
          setNodeId(selection[0].id);
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

      if (message && message.type === 'ws-response') {
        const response = message.response;
        if (response && response.result) {
          setResultsData(JSON.stringify(response.result, null, 2));
        }
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const getSelectedNode = () => {
    parent.postMessage({ pluginMessage: { type: 'get-selection' } }, '*');
  };

  const clearResults = () => {
    setResultsData(null);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setTestResult({ success: true, message: 'Copied to clipboard!' });
    setTimeout(() => setTestResult(null), 2000);
  };

  // Tool 1: Create Variable Collection
  const testCreateVariableCollection = () => {
    const params: any = {
      name: collectionName
    };

    if (collectionModes) {
      const modes = collectionModes.split(',').map(m => m.trim()).filter(m => m);
      if (modes.length > 0) {
        params.modes = modes;
      }
    }

    sendCommand('createVariableCollection', params);
  };

  // Tool 2: Get Variables
  const testGetVariables = () => {
    const params: any = {};

    if (getVarsCollectionId) params.collectionId = getVarsCollectionId;
    if (getVarsType && getVarsType !== 'ALL') params.type = getVarsType;

    sendCommand('getVariables', params);
  };

  // Tool 3: Create Variable
  const testCreateVariable = () => {
    const params: any = {
      name: variableName,
      collectionId: variableCollectionId,
      type: variableType
    };

    // Build values object if provided
    if (variableValue) {
      let value: any;

      switch (variableType) {
        case 'BOOLEAN':
          value = variableValue.toLowerCase() === 'true';
          break;
        case 'FLOAT':
          value = parseFloat(variableValue);
          break;
        case 'STRING':
          value = variableValue;
          break;
        case 'COLOR':
          value = {
            r: parseFloat(colorR),
            g: parseFloat(colorG),
            b: parseFloat(colorB),
            a: parseFloat(colorA)
          };
          break;
      }

      // For simplicity, set value for default mode
      // In reality, you'd need to know the mode ID
      params.values = { default: value };
    }

    sendCommand('createVariable', params);
  };

  // Tool 4: Bind Variable
  const testBindVariable = () => {
    sendCommand('bindVariable', {
      nodeId: bindNodeId,
      field: bindField,
      variableId: bindVariableId
    });
  };

  // Tool 5: Unbind Variable
  const testUnbindVariable = () => {
    sendCommand('unbindVariable', {
      nodeId: unbindNodeId,
      field: unbindField
    });
  };

  // Tool 6: Set Variable Value
  const testSetVariableValue = () => {
    const params: any = {
      variableId: setValueVariableId
    };

    // Parse value based on type
    let value: any;
    switch (setValueType) {
      case 'BOOLEAN':
        value = setValueValue.toLowerCase() === 'true';
        break;
      case 'FLOAT':
        value = parseFloat(setValueValue);
        break;
      case 'STRING':
        value = setValueValue;
        break;
      case 'COLOR':
        value = {
          r: parseFloat(setValueColorR),
          g: parseFloat(setValueColorG),
          b: parseFloat(setValueColorB),
          a: parseFloat(setValueColorA)
        };
        break;
    }

    params.value = value;

    if (setValueModeId) params.modeId = setValueModeId;

    sendCommand('setVariableValue', params);
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
          <Label>Auto-fill Node ID</Label>
          <div className="flex gap-2">
            <Input
              value={nodeId}
              onChange={(e) => setNodeId(e.target.value)}
              placeholder="Select a node in Figma"
              readOnly
            />
            <Button onClick={getSelectedNode} size="sm">
              Get Selected
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Select a node in Figma to auto-fill IDs for variable binding
          </p>
        </div>
      </Section>

      <Separator />

      {/* SECTION 1: COLLECTION MANAGEMENT */}
      <Section title="Section 1: Collection Management" description="Create and query variable collections">
        <div className="space-y-4">
          {/* Tool 1: Create Variable Collection */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">1. Create Variable Collection</h4>
            <div className="space-y-2">
              <Label>Collection Name <span className="text-destructive">*</span></Label>
              <Input
                value={collectionName}
                onChange={(e) => setCollectionName(e.target.value)}
                placeholder="Colors, Typography, etc."
              />
            </div>
            <div className="space-y-2">
              <Label>Modes (comma-separated, optional)</Label>
              <Input
                value={collectionModes}
                onChange={(e) => setCollectionModes(e.target.value)}
                placeholder="Light, Dark"
              />
              <p className="text-xs text-muted-foreground">
                Leave empty for default mode, or specify modes like "Light, Dark"
              </p>
            </div>
            <Button onClick={testCreateVariableCollection} disabled={!collectionName} className="w-full">
              Create Collection
            </Button>
          </Card>

          {/* Tool 2: Get Variables */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">2. Get Variables</h4>
            <div className="space-y-2">
              <Label>Collection ID (optional)</Label>
              <Input
                value={getVarsCollectionId}
                onChange={(e) => setGetVarsCollectionId(e.target.value)}
                placeholder="Filter by collection"
              />
            </div>
            <div className="space-y-2">
              <Label>Variable Type (optional)</Label>
              <Select value={getVarsType} onValueChange={setGetVarsType}>
                <SelectTrigger>
                  <SelectValue placeholder="All types" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Types</SelectItem>
                  <SelectItem value="BOOLEAN">Boolean</SelectItem>
                  <SelectItem value="FLOAT">Float</SelectItem>
                  <SelectItem value="STRING">String</SelectItem>
                  <SelectItem value="COLOR">Color</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Button onClick={testGetVariables} className="w-full">
              Get Variables
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 2: VARIABLE CREATION */}
      <Section title="Section 2: Variable Creation" description="Create design token variables">
        <div className="space-y-4">
          {/* Tool 3: Create Variable */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">3. Create Variable</h4>
            <div className="space-y-2">
              <Label>Variable Name <span className="text-destructive">*</span></Label>
              <Input
                value={variableName}
                onChange={(e) => setVariableName(e.target.value)}
                placeholder="primary-color, font-size-base, etc."
              />
            </div>
            <div className="space-y-2">
              <Label>Collection ID <span className="text-destructive">*</span></Label>
              <Input
                value={variableCollectionId}
                onChange={(e) => setVariableCollectionId(e.target.value)}
                placeholder="Collection to add variable to"
              />
              <p className="text-xs text-muted-foreground">
                Use "Get Variables" above to find collection IDs
              </p>
            </div>
            <div className="space-y-2">
              <Label>Variable Type <span className="text-destructive">*</span></Label>
              <Select value={variableType} onValueChange={setVariableType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BOOLEAN">Boolean</SelectItem>
                  <SelectItem value="FLOAT">Float (Number)</SelectItem>
                  <SelectItem value="STRING">String (Text)</SelectItem>
                  <SelectItem value="COLOR">Color</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Separator />

            <div className="space-y-2">
              <Label>Initial Value (optional)</Label>

              {variableType === 'BOOLEAN' && (
                <Select value={variableValue} onValueChange={setVariableValue}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select boolean value" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">True</SelectItem>
                    <SelectItem value="false">False</SelectItem>
                  </SelectContent>
                </Select>
              )}

              {variableType === 'FLOAT' && (
                <Input
                  type="number"
                  step="0.1"
                  value={variableValue}
                  onChange={(e) => setVariableValue(e.target.value)}
                  placeholder="16, 1.5, 24, etc."
                />
              )}

              {variableType === 'STRING' && (
                <Input
                  value={variableValue}
                  onChange={(e) => setVariableValue(e.target.value)}
                  placeholder="Text value"
                />
              )}

              {variableType === 'COLOR' && (
                <div className="space-y-3">
                  <div className="flex gap-2 items-center">
                    <Input
                      type="color"
                      value={colorHex}
                      onChange={(e) => setColorHex(e.target.value)}
                      className="w-20"
                    />
                    <Input
                      value={colorHex}
                      onChange={(e) => setColorHex(e.target.value)}
                      placeholder="#FF0000"
                    />
                  </div>
                  <div className="grid grid-cols-4 gap-2">
                    <div className="space-y-1">
                      <Label className="text-xs">R (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={colorR}
                        onChange={(e) => setColorR(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">G (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={colorG}
                        onChange={(e) => setColorG(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">B (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={colorB}
                        onChange={(e) => setColorB(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">A (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={colorA}
                        onChange={(e) => setColorA(e.target.value)}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            <Button
              onClick={testCreateVariable}
              disabled={!variableName || !variableCollectionId}
              className="w-full"
            >
              Create Variable
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 3: VARIABLE BINDING */}
      <Section title="Section 3: Variable Binding" description="Bind variables to node properties">
        <div className="space-y-4">
          {/* Tool 4: Bind Variable */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">4. Bind Variable</h4>
            <div className="space-y-2">
              <Label>Node ID <span className="text-destructive">*</span></Label>
              <div className="flex gap-2">
                <Input
                  value={bindNodeId}
                  onChange={(e) => setBindNodeId(e.target.value)}
                  placeholder="Enter node ID"
                />
                <Button onClick={() => setBindNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Field (Property) <span className="text-destructive">*</span></Label>
              <Select value={bindField} onValueChange={setBindField}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="fills">Fills</SelectItem>
                  <SelectItem value="strokes">Strokes</SelectItem>
                  <SelectItem value="opacity">Opacity</SelectItem>
                  <SelectItem value="width">Width</SelectItem>
                  <SelectItem value="height">Height</SelectItem>
                  <SelectItem value="cornerRadius">Corner Radius</SelectItem>
                  <SelectItem value="topLeftRadius">Top Left Radius</SelectItem>
                  <SelectItem value="topRightRadius">Top Right Radius</SelectItem>
                  <SelectItem value="bottomLeftRadius">Bottom Left Radius</SelectItem>
                  <SelectItem value="bottomRightRadius">Bottom Right Radius</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Property to bind the variable to
              </p>
            </div>
            <div className="space-y-2">
              <Label>Variable ID <span className="text-destructive">*</span></Label>
              <Input
                value={bindVariableId}
                onChange={(e) => setBindVariableId(e.target.value)}
                placeholder="Variable to bind"
              />
              <p className="text-xs text-muted-foreground">
                Use "Get Variables" to find variable IDs
              </p>
            </div>
            <Button
              onClick={testBindVariable}
              disabled={!bindNodeId || !bindField || !bindVariableId}
              className="w-full"
            >
              Bind Variable
            </Button>
          </Card>

          {/* Tool 5: Unbind Variable */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">5. Unbind Variable</h4>
            <div className="space-y-2">
              <Label>Node ID <span className="text-destructive">*</span></Label>
              <div className="flex gap-2">
                <Input
                  value={unbindNodeId}
                  onChange={(e) => setUnbindNodeId(e.target.value)}
                  placeholder="Enter node ID"
                />
                <Button onClick={() => setUnbindNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Field (Property) <span className="text-destructive">*</span></Label>
              <Select value={unbindField} onValueChange={setUnbindField}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="fills">Fills</SelectItem>
                  <SelectItem value="strokes">Strokes</SelectItem>
                  <SelectItem value="opacity">Opacity</SelectItem>
                  <SelectItem value="width">Width</SelectItem>
                  <SelectItem value="height">Height</SelectItem>
                  <SelectItem value="cornerRadius">Corner Radius</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Button
              onClick={testUnbindVariable}
              disabled={!unbindNodeId || !unbindField}
              className="w-full"
            >
              Unbind Variable
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 4: VARIABLE VALUES */}
      <Section title="Section 4: Variable Values" description="Update variable values">
        <div className="space-y-4">
          {/* Tool 6: Set Variable Value */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">6. Set Variable Value</h4>
            <div className="space-y-2">
              <Label>Variable ID <span className="text-destructive">*</span></Label>
              <Input
                value={setValueVariableId}
                onChange={(e) => setSetValueVariableId(e.target.value)}
                placeholder="Variable to update"
              />
            </div>
            <div className="space-y-2">
              <Label>Variable Type <span className="text-destructive">*</span></Label>
              <Select value={setValueType} onValueChange={setSetValueType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BOOLEAN">Boolean</SelectItem>
                  <SelectItem value="FLOAT">Float (Number)</SelectItem>
                  <SelectItem value="STRING">String (Text)</SelectItem>
                  <SelectItem value="COLOR">Color</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Select the type that matches your variable
              </p>
            </div>

            <Separator />

            <div className="space-y-2">
              <Label>New Value <span className="text-destructive">*</span></Label>

              {setValueType === 'BOOLEAN' && (
                <Select value={setValueValue} onValueChange={setSetValueValue}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select boolean value" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">True</SelectItem>
                    <SelectItem value="false">False</SelectItem>
                  </SelectContent>
                </Select>
              )}

              {setValueType === 'FLOAT' && (
                <Input
                  type="number"
                  step="0.1"
                  value={setValueValue}
                  onChange={(e) => setSetValueValue(e.target.value)}
                  placeholder="16, 1.5, 24, etc."
                />
              )}

              {setValueType === 'STRING' && (
                <Input
                  value={setValueValue}
                  onChange={(e) => setSetValueValue(e.target.value)}
                  placeholder="Text value"
                />
              )}

              {setValueType === 'COLOR' && (
                <div className="space-y-3">
                  <div className="flex gap-2 items-center">
                    <Input
                      type="color"
                      value={setValueColorHex}
                      onChange={(e) => setSetValueColorHex(e.target.value)}
                      className="w-20"
                    />
                    <Input
                      value={setValueColorHex}
                      onChange={(e) => setSetValueColorHex(e.target.value)}
                      placeholder="#FF0000"
                    />
                  </div>
                  <div className="grid grid-cols-4 gap-2">
                    <div className="space-y-1">
                      <Label className="text-xs">R (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={setValueColorR}
                        onChange={(e) => setSetValueColorR(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">G (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={setValueColorG}
                        onChange={(e) => setSetValueColorG(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">B (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={setValueColorB}
                        onChange={(e) => setSetValueColorB(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs">A (0-1)</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        max="1"
                        value={setValueColorA}
                        onChange={(e) => setSetValueColorA(e.target.value)}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="space-y-2">
              <Label>Mode ID (optional)</Label>
              <Input
                value={setValueModeId}
                onChange={(e) => setSetValueModeId(e.target.value)}
                placeholder="Leave empty for default mode"
              />
              <p className="text-xs text-muted-foreground">
                Specify mode ID to set value for a specific theme/mode
              </p>
            </div>

            <Button
              onClick={testSetVariableValue}
              disabled={!setValueVariableId || (!setValueValue && setValueType !== 'COLOR')}
              className="w-full"
            >
              Set Variable Value
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* RESULTS DISPLAY */}
      {resultsData && (
        <Section
          title="Results"
          action={
            <div className="flex gap-2">
              <Button onClick={() => copyToClipboard(resultsData)} size="sm" variant="outline">
                Copy
              </Button>
              <Button onClick={clearResults} size="sm" variant="outline">
                Clear
              </Button>
            </div>
          }
        >
          <Card className="p-4 bg-muted">
            <pre className="text-xs overflow-auto max-h-96">
              {resultsData}
            </pre>
          </Card>
        </Section>
      )}

      <div className="text-xs text-muted-foreground mt-4 space-y-1">
        <p>Tip: Use "Get Variables" to retrieve collection and variable IDs for use in other tools.</p>
        <p>Note: All 6 variable and token tools are available in this panel.</p>
        <p>Color values use 0-1 range (Figma format), not 0-255.</p>
      </div>
    </div>
  );
};
