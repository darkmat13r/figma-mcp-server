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

interface ComponentsTestPanelProps {
  onSendCommand: (command: any) => void;
  isConnected: boolean;
}

export const ComponentsTestPanel: React.FC<ComponentsTestPanelProps> = ({ onSendCommand, isConnected }) => {
  // State for test results
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [resultsData, setResultsData] = useState<string | null>(null);

  // State for node selection
  const [nodeId, setNodeId] = useState('');

  // Section 1: Component Creation
  // Tool 1: Create Component from Node
  const [componentNodeId, setComponentNodeId] = useState('');
  const [componentName, setComponentName] = useState('');
  const [componentDescription, setComponentDescription] = useState('');

  // Tool 2: Create Component Set
  const [componentSetIds, setComponentSetIds] = useState('');
  const [componentSetName, setComponentSetName] = useState('');

  // Section 2: Component Properties
  // Tool 3: Set Component Properties
  const [propertiesComponentId, setPropertiesComponentId] = useState('');
  const [propertyName, setPropertyName] = useState('');
  const [propertyType, setPropertyType] = useState('TEXT');
  const [propertyDefaultValue, setPropertyDefaultValue] = useState('');
  const [propertyVariantOptions, setPropertyVariantOptions] = useState('');
  const [properties, setProperties] = useState<any[]>([]);

  // Section 3: Instance Management
  // Tool 4: Set Instance Properties
  const [instancePropsId, setInstancePropsId] = useState('');
  const [instancePropKey, setInstancePropKey] = useState('');
  const [instancePropValue, setInstancePropValue] = useState('');
  const [instanceProperties, setInstanceProperties] = useState<Record<string, any>>({});

  // Tool 5: Detach Instance
  const [detachInstanceId, setDetachInstanceId] = useState('');

  // Tool 6: Swap Instance
  const [swapInstanceId, setSwapInstanceId] = useState('');
  const [newComponentId, setNewComponentId] = useState('');

  // Section 4: Variants
  // Tool 7: Add Variant
  const [variantComponentSetId, setVariantComponentSetId] = useState('');
  const [variantPropKey, setVariantPropKey] = useState('');
  const [variantPropValue, setVariantPropValue] = useState('');
  const [variantProperties, setVariantProperties] = useState<Record<string, string>>({});

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

  // Tool 1: Create Component from Node
  const testCreateComponentFromNode = () => {
    const params: any = {
      nodeId: componentNodeId
    };
    if (componentName) params.name = componentName;
    if (componentDescription) params.description = componentDescription;

    sendCommand('createComponentFromNode', params);
  };

  // Tool 2: Create Component Set
  const testCreateComponentSet = () => {
    const componentIds = componentSetIds.split(',').map(id => id.trim()).filter(id => id);
    const params: any = {
      componentIds
    };
    if (componentSetName) params.name = componentSetName;

    sendCommand('createComponentSet', params);
  };

  // Tool 3: Set Component Properties - Add property to list
  const addProperty = () => {
    if (!propertyName || !propertyType) {
      setTestResult({ success: false, message: 'Property name and type are required' });
      setTimeout(() => setTestResult(null), 3000);
      return;
    }

    const newProperty: any = {
      name: propertyName,
      type: propertyType
    };

    if (propertyDefaultValue) {
      // Parse default value based on type
      if (propertyType === 'BOOLEAN') {
        newProperty.defaultValue = propertyDefaultValue.toLowerCase() === 'true';
      } else {
        newProperty.defaultValue = propertyDefaultValue;
      }
    }

    if (propertyType === 'VARIANT' && propertyVariantOptions) {
      newProperty.variantOptions = propertyVariantOptions.split(',').map(v => v.trim());
    }

    setProperties([...properties, newProperty]);

    // Clear inputs
    setPropertyName('');
    setPropertyDefaultValue('');
    setPropertyVariantOptions('');

    setTestResult({ success: true, message: 'Property added to list' });
    setTimeout(() => setTestResult(null), 2000);
  };

  const removeProperty = (index: number) => {
    setProperties(properties.filter((_, i) => i !== index));
  };

  const testSetComponentProperties = () => {
    sendCommand('setComponentProperties', {
      componentId: propertiesComponentId,
      properties
    });
  };

  // Tool 4: Set Instance Properties - Add property to object
  const addInstanceProperty = () => {
    if (!instancePropKey) {
      setTestResult({ success: false, message: 'Property key is required' });
      setTimeout(() => setTestResult(null), 3000);
      return;
    }

    setInstanceProperties({
      ...instanceProperties,
      [instancePropKey]: instancePropValue
    });

    setInstancePropKey('');
    setInstancePropValue('');

    setTestResult({ success: true, message: 'Property added' });
    setTimeout(() => setTestResult(null), 2000);
  };

  const removeInstanceProperty = (key: string) => {
    const newProps = { ...instanceProperties };
    delete newProps[key];
    setInstanceProperties(newProps);
  };

  const testSetInstanceProperties = () => {
    sendCommand('setInstanceProperties', {
      instanceId: instancePropsId,
      properties: instanceProperties
    });
  };

  // Tool 5: Detach Instance
  const testDetachInstance = () => {
    sendCommand('detachInstance', {
      instanceId: detachInstanceId
    });
  };

  // Tool 6: Swap Instance
  const testSwapInstance = () => {
    sendCommand('swapInstance', {
      instanceId: swapInstanceId,
      newComponentId
    });
  };

  // Tool 7: Add Variant - Add property to object
  const addVariantProperty = () => {
    if (!variantPropKey || !variantPropValue) {
      setTestResult({ success: false, message: 'Property key and value are required' });
      setTimeout(() => setTestResult(null), 3000);
      return;
    }

    setVariantProperties({
      ...variantProperties,
      [variantPropKey]: variantPropValue
    });

    setVariantPropKey('');
    setVariantPropValue('');

    setTestResult({ success: true, message: 'Variant property added' });
    setTimeout(() => setTestResult(null), 2000);
  };

  const removeVariantProperty = (key: string) => {
    const newProps = { ...variantProperties };
    delete newProps[key];
    setVariantProperties(newProps);
  };

  const testAddVariant = () => {
    sendCommand('addVariant', {
      componentSetId: variantComponentSetId,
      properties: variantProperties
    });
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
            Select a node in Figma to auto-fill IDs below
          </p>
        </div>
      </Section>

      <Separator />

      {/* SECTION 1: COMPONENT CREATION */}
      <Section title="Section 1: Component Creation" description="Create components and component sets">
        <div className="space-y-4">
          {/* Tool 1: Create Component from Node */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">1. Create Component from Node</h4>
            <div className="space-y-2">
              <Label>Node ID <span className="text-destructive">*</span></Label>
              <div className="flex gap-2">
                <Input
                  value={componentNodeId}
                  onChange={(e) => setComponentNodeId(e.target.value)}
                  placeholder="Enter node ID to convert"
                />
                <Button onClick={() => setComponentNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Component Name (optional)</Label>
              <Input
                value={componentName}
                onChange={(e) => setComponentName(e.target.value)}
                placeholder="My Component"
              />
            </div>
            <div className="space-y-2">
              <Label>Description (optional)</Label>
              <Input
                value={componentDescription}
                onChange={(e) => setComponentDescription(e.target.value)}
                placeholder="Component description"
              />
            </div>
            <Button onClick={testCreateComponentFromNode} disabled={!componentNodeId} className="w-full">
              Create Component
            </Button>
          </Card>

          {/* Tool 2: Create Component Set */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">2. Create Component Set</h4>
            <div className="space-y-2">
              <Label>Component IDs (comma-separated) <span className="text-destructive">*</span></Label>
              <Input
                value={componentSetIds}
                onChange={(e) => setComponentSetIds(e.target.value)}
                placeholder="comp1, comp2, comp3"
              />
              <p className="text-xs text-muted-foreground">
                Requires at least 2 component IDs
              </p>
            </div>
            <div className="space-y-2">
              <Label>Component Set Name (optional)</Label>
              <Input
                value={componentSetName}
                onChange={(e) => setComponentSetName(e.target.value)}
                placeholder="Button Variants"
              />
            </div>
            <Button onClick={testCreateComponentSet} disabled={!componentSetIds} className="w-full">
              Create Component Set
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 2: COMPONENT PROPERTIES */}
      <Section title="Section 2: Component Properties" description="Define properties for components">
        <div className="space-y-4">
          {/* Tool 3: Set Component Properties */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">3. Set Component Properties</h4>
            <div className="space-y-2">
              <Label>Component ID <span className="text-destructive">*</span></Label>
              <Input
                value={propertiesComponentId}
                onChange={(e) => setPropertiesComponentId(e.target.value)}
                placeholder="Enter component ID"
              />
            </div>

            <Separator />

            <div className="space-y-3">
              <Label className="text-sm font-semibold">Property Builder</Label>

              <div className="space-y-2">
                <Label>Property Name <span className="text-destructive">*</span></Label>
                <Input
                  value={propertyName}
                  onChange={(e) => setPropertyName(e.target.value)}
                  placeholder="label, visible, etc."
                />
              </div>

              <div className="space-y-2">
                <Label>Property Type <span className="text-destructive">*</span></Label>
                <Select value={propertyType} onValueChange={setPropertyType}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="BOOLEAN">Boolean</SelectItem>
                    <SelectItem value="TEXT">Text</SelectItem>
                    <SelectItem value="INSTANCE_SWAP">Instance Swap</SelectItem>
                    <SelectItem value="VARIANT">Variant</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Default Value (optional)</Label>
                <Input
                  value={propertyDefaultValue}
                  onChange={(e) => setPropertyDefaultValue(e.target.value)}
                  placeholder={propertyType === 'BOOLEAN' ? 'true/false' : 'Default value'}
                />
              </div>

              {propertyType === 'VARIANT' && (
                <div className="space-y-2">
                  <Label>Variant Options (comma-separated)</Label>
                  <Input
                    value={propertyVariantOptions}
                    onChange={(e) => setPropertyVariantOptions(e.target.value)}
                    placeholder="Small, Medium, Large"
                  />
                </div>
              )}

              <Button onClick={addProperty} size="sm" variant="outline" className="w-full">
                Add Property to List
              </Button>
            </div>

            {properties.length > 0 && (
              <div className="space-y-2">
                <Label>Properties to Set ({properties.length})</Label>
                <Card className="p-3 bg-muted space-y-2">
                  {properties.map((prop, index) => (
                    <div key={index} className="flex items-center justify-between text-sm">
                      <span>
                        <strong>{prop.name}</strong> ({prop.type})
                        {prop.defaultValue && ` = ${prop.defaultValue}`}
                      </span>
                      <Button
                        onClick={() => removeProperty(index)}
                        size="sm"
                        variant="ghost"
                      >
                        Remove
                      </Button>
                    </div>
                  ))}
                </Card>
              </div>
            )}

            <Button
              onClick={testSetComponentProperties}
              disabled={!propertiesComponentId || properties.length === 0}
              className="w-full"
            >
              Set Component Properties
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 3: INSTANCE MANAGEMENT */}
      <Section title="Section 3: Instance Management" description="Manage component instances">
        <div className="space-y-4">
          {/* Tool 4: Set Instance Properties */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">4. Set Instance Properties</h4>
            <div className="space-y-2">
              <Label>Instance ID <span className="text-destructive">*</span></Label>
              <div className="flex gap-2">
                <Input
                  value={instancePropsId}
                  onChange={(e) => setInstancePropsId(e.target.value)}
                  placeholder="Enter instance ID"
                />
                <Button onClick={() => setInstancePropsId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>

            <Separator />

            <div className="space-y-3">
              <Label className="text-sm font-semibold">Property Overrides</Label>

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-2">
                  <Label>Property Key</Label>
                  <Input
                    value={instancePropKey}
                    onChange={(e) => setInstancePropKey(e.target.value)}
                    placeholder="label"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Property Value</Label>
                  <Input
                    value={instancePropValue}
                    onChange={(e) => setInstancePropValue(e.target.value)}
                    placeholder="Button Text"
                  />
                </div>
              </div>

              <Button onClick={addInstanceProperty} size="sm" variant="outline" className="w-full">
                Add Property Override
              </Button>
            </div>

            {Object.keys(instanceProperties).length > 0 && (
              <div className="space-y-2">
                <Label>Property Overrides ({Object.keys(instanceProperties).length})</Label>
                <Card className="p-3 bg-muted space-y-2">
                  {Object.entries(instanceProperties).map(([key, value]) => (
                    <div key={key} className="flex items-center justify-between text-sm">
                      <span>
                        <strong>{key}:</strong> {String(value)}
                      </span>
                      <Button
                        onClick={() => removeInstanceProperty(key)}
                        size="sm"
                        variant="ghost"
                      >
                        Remove
                      </Button>
                    </div>
                  ))}
                </Card>
              </div>
            )}

            <Button
              onClick={testSetInstanceProperties}
              disabled={!instancePropsId || Object.keys(instanceProperties).length === 0}
              className="w-full"
            >
              Set Instance Properties
            </Button>
          </Card>

          {/* Tool 5: Detach Instance */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">5. Detach Instance</h4>
            <div className="space-y-2">
              <Label>Instance ID <span className="text-destructive">*</span></Label>
              <div className="flex gap-2">
                <Input
                  value={detachInstanceId}
                  onChange={(e) => setDetachInstanceId(e.target.value)}
                  placeholder="Enter instance ID"
                />
                <Button onClick={() => setDetachInstanceId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                Breaks the link between instance and master component
              </p>
            </div>
            <Button onClick={testDetachInstance} disabled={!detachInstanceId} className="w-full">
              Detach Instance
            </Button>
          </Card>

          {/* Tool 6: Swap Instance */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">6. Swap Instance</h4>
            <div className="space-y-2">
              <Label>Instance ID <span className="text-destructive">*</span></Label>
              <div className="flex gap-2">
                <Input
                  value={swapInstanceId}
                  onChange={(e) => setSwapInstanceId(e.target.value)}
                  placeholder="Enter instance ID"
                />
                <Button onClick={() => setSwapInstanceId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>New Component ID <span className="text-destructive">*</span></Label>
              <Input
                value={newComponentId}
                onChange={(e) => setNewComponentId(e.target.value)}
                placeholder="Component to swap to"
              />
            </div>
            <Button onClick={testSwapInstance} disabled={!swapInstanceId || !newComponentId} className="w-full">
              Swap Instance
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 4: VARIANTS */}
      <Section title="Section 4: Variants" description="Add variants to component sets">
        <div className="space-y-4">
          {/* Tool 7: Add Variant */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">7. Add Variant</h4>
            <div className="space-y-2">
              <Label>Component Set ID <span className="text-destructive">*</span></Label>
              <Input
                value={variantComponentSetId}
                onChange={(e) => setVariantComponentSetId(e.target.value)}
                placeholder="Enter component set ID"
              />
            </div>

            <Separator />

            <div className="space-y-3">
              <Label className="text-sm font-semibold">Variant Properties</Label>

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-2">
                  <Label>Property Key</Label>
                  <Input
                    value={variantPropKey}
                    onChange={(e) => setVariantPropKey(e.target.value)}
                    placeholder="Size, State, etc."
                  />
                </div>
                <div className="space-y-2">
                  <Label>Property Value</Label>
                  <Input
                    value={variantPropValue}
                    onChange={(e) => setVariantPropValue(e.target.value)}
                    placeholder="Large, Hover, etc."
                  />
                </div>
              </div>

              <Button onClick={addVariantProperty} size="sm" variant="outline" className="w-full">
                Add Variant Property
              </Button>
            </div>

            {Object.keys(variantProperties).length > 0 && (
              <div className="space-y-2">
                <Label>Variant Properties ({Object.keys(variantProperties).length})</Label>
                <Card className="p-3 bg-muted space-y-2">
                  {Object.entries(variantProperties).map(([key, value]) => (
                    <div key={key} className="flex items-center justify-between text-sm">
                      <span>
                        <strong>{key}:</strong> {value}
                      </span>
                      <Button
                        onClick={() => removeVariantProperty(key)}
                        size="sm"
                        variant="ghost"
                      >
                        Remove
                      </Button>
                    </div>
                  ))}
                </Card>
              </div>
            )}

            <Button
              onClick={testAddVariant}
              disabled={!variantComponentSetId || Object.keys(variantProperties).length === 0}
              className="w-full"
            >
              Add Variant
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
        <p>Tip: Use "Get Selected" to quickly fill node/component IDs from your current Figma selection.</p>
        <p>Note: All 7 component and variant tools are available in this panel.</p>
      </div>
    </div>
  );
};
