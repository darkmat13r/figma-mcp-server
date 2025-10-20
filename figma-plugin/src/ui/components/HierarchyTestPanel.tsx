import React, { useState, useEffect } from 'react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
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

interface HierarchyTestPanelProps {
  onSendCommand: (command: any) => void;
  isConnected: boolean;
}

export const HierarchyTestPanel: React.FC<HierarchyTestPanelProps> = ({ onSendCommand, isConnected }) => {
  // State for test results
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null);
  const [resultsData, setResultsData] = useState<string | null>(null);

  // State for node IDs
  const [nodeId, setNodeId] = useState('');
  const [parentNodeId, setParentNodeId] = useState('');
  const [newParentId, setNewParentId] = useState('');

  // Get Node Info (Tool 1)
  const [getInfoNodeId, setGetInfoNodeId] = useState('');

  // Set Selection (Tool 3)
  const [selectionNodeIds, setSelectionNodeIds] = useState('');

  // Find Nodes (Tool 4)
  const [findName, setFindName] = useState('');
  const [findType, setFindType] = useState('');
  const [findParentId, setFindParentId] = useState('');
  const [findRecursive, setFindRecursive] = useState(true);

  // Search Nodes (Tool 5)
  const [searchText, setSearchText] = useState('');
  const [searchNodeId, setSearchNodeId] = useState('');
  const [searchComponentId, setSearchComponentId] = useState('');
  const [searchCurrentPageOnly, setSearchCurrentPageOnly] = useState(false);

  // Get Children (Tool 6)
  const [childrenNodeId, setChildrenNodeId] = useState('');
  const [childrenRecursive, setChildrenRecursive] = useState(false);

  // Get Parent (Tool 7)
  const [parentLookupNodeId, setParentLookupNodeId] = useState('');

  // Move Node (Tool 8)
  const [moveNodeId, setMoveNodeId] = useState('');
  const [moveNewParentId, setMoveNewParentId] = useState('');
  const [moveIndex, setMoveIndex] = useState('');

  // Clone Node (Tool 9)
  const [cloneNodeId, setCloneNodeId] = useState('');
  const [cloneDeep, setCloneDeep] = useState(true);

  // Switch Page (Tool 11)
  const [switchPageId, setSwitchPageId] = useState('');
  const [switchPageName, setSwitchPageName] = useState('');

  // Create Page (Tool 12)
  const [createPageName, setCreatePageName] = useState('');
  const [createPageSwitch, setCreatePageSwitch] = useState(true);

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

  // Listen for responses from plugin
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

  // Tool 1: Get Node Info
  const testGetNodeInfo = () => {
    sendCommand('getNodeInfo', {
      nodeId: getInfoNodeId
    });
  };

  // Tool 2: Get Selection
  const testGetSelection = () => {
    sendCommand('getSelection', {});
  };

  // Tool 3: Set Selection
  const testSetSelection = () => {
    const nodeIds = selectionNodeIds.split(',').map(id => id.trim()).filter(id => id);
    sendCommand('setSelection', {
      nodeIds: nodeIds
    });
  };

  // Tool 4: Find Nodes
  const testFindNodes = () => {
    const params: any = {};
    if (findName) params.name = findName;
    if (findType) {
      // Support comma-separated types
      const types = findType.split(',').map(t => t.trim()).filter(t => t);
      params.type = types.length === 1 ? types[0] : types;
    }
    if (findParentId) params.parentId = findParentId;
    params.recursive = findRecursive;

    sendCommand('findNodes', params);
  };

  // Tool 5: Search Nodes
  const testSearchNodes = () => {
    const params: any = {};
    if (searchText) params.searchText = searchText;
    if (searchNodeId) params.nodeId = searchNodeId;
    if (searchComponentId) params.componentId = searchComponentId;
    params.searchInCurrentPageOnly = searchCurrentPageOnly;

    sendCommand('searchNodes', params);
  };

  // Tool 6: Get Children
  const testGetChildren = () => {
    sendCommand('getChildren', {
      nodeId: childrenNodeId,
      recursive: childrenRecursive
    });
  };

  // Tool 7: Get Parent
  const testGetParent = () => {
    sendCommand('getParent', {
      nodeId: parentLookupNodeId
    });
  };

  // Tool 8: Move Node
  const testMoveNode = () => {
    const params: any = {
      nodeId: moveNodeId,
      newParentId: moveNewParentId
    };
    if (moveIndex) params.index = parseInt(moveIndex);

    sendCommand('moveNode', params);
  };

  // Tool 9: Clone Node
  const testCloneNode = () => {
    sendCommand('cloneNode', {
      nodeId: cloneNodeId,
      deep: cloneDeep
    });
  };

  // Tool 10: Get Current Page Nodes
  const testGetCurrentPageNodes = () => {
    sendCommand('getCurrentPageNodes', {});
  };

  // Tool 11: Get All Pages
  const testGetAllPages = () => {
    sendCommand('getAllPages', {});
  };

  // Tool 12: Switch Page
  const testSwitchPage = () => {
    const params: any = {};
    if (switchPageId) params.pageId = switchPageId;
    if (switchPageName) params.pageName = switchPageName;

    sendCommand('switchPage', params);
  };

  // Tool 13: Create Page
  const testCreatePage = () => {
    const params: any = {
      switchToPage: createPageSwitch
    };
    if (createPageName) params.name = createPageName;

    sendCommand('createPage', params);
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
            Select a node in Figma to auto-fill node IDs below
          </p>
        </div>
      </Section>

      <Separator />

      {/* SECTION 1: NODE INFORMATION */}
      <Section title="Section 1: Node Information" description="Get information about nodes and selections">
        <div className="space-y-4">
          {/* Tool 1: Get Node Info */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">1. Get Node Info</h4>
            <div className="space-y-2">
              <Label>Node ID</Label>
              <div className="flex gap-2">
                <Input
                  value={getInfoNodeId}
                  onChange={(e) => setGetInfoNodeId(e.target.value)}
                  placeholder="Enter node ID"
                />
                <Button onClick={() => setGetInfoNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <Button onClick={testGetNodeInfo} disabled={!getInfoNodeId} className="w-full">
              Get Node Info
            </Button>
          </Card>

          {/* Tool 2: Get Selection */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">2. Get Selection</h4>
            <p className="text-sm text-muted-foreground">
              Get all currently selected nodes in Figma
            </p>
            <Button onClick={testGetSelection} className="w-full">
              Get Current Selection
            </Button>
          </Card>

          {/* Tool 3: Set Selection */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">3. Set Selection</h4>
            <div className="space-y-2">
              <Label>Node IDs (comma-separated)</Label>
              <Input
                value={selectionNodeIds}
                onChange={(e) => setSelectionNodeIds(e.target.value)}
                placeholder="id1, id2, id3"
              />
            </div>
            <Button onClick={testSetSelection} disabled={!selectionNodeIds} className="w-full">
              Set Selection
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 2: NODE SEARCH & QUERY */}
      <Section title="Section 2: Node Search & Query" description="Find and search for nodes">
        <div className="space-y-4">
          {/* Tool 4: Find Nodes */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">4. Find Nodes</h4>
            <div className="space-y-2">
              <Label>Name Pattern (regex)</Label>
              <Input
                value={findName}
                onChange={(e) => setFindName(e.target.value)}
                placeholder="Button.*|Frame.*"
              />
            </div>
            <div className="space-y-2">
              <Label>Type(s) (comma-separated)</Label>
              <Input
                value={findType}
                onChange={(e) => setFindType(e.target.value)}
                placeholder="FRAME, TEXT, RECTANGLE"
              />
              <p className="text-xs text-muted-foreground">
                Examples: FRAME, TEXT, RECTANGLE, COMPONENT, INSTANCE
              </p>
            </div>
            <div className="space-y-2">
              <Label>Parent ID (optional)</Label>
              <div className="flex gap-2">
                <Input
                  value={findParentId}
                  onChange={(e) => setFindParentId(e.target.value)}
                  placeholder="Search within parent"
                />
                <Button onClick={() => setFindParentId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="findRecursive"
                checked={findRecursive}
                onChange={(e) => setFindRecursive(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="findRecursive" className="cursor-pointer">
                Recursive search (search all descendants)
              </Label>
            </div>
            <Button onClick={testFindNodes} className="w-full">
              Find Nodes
            </Button>
          </Card>

          {/* Tool 5: Search Nodes */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">5. Search Nodes (Advanced)</h4>
            <div className="space-y-2">
              <Label>Search Text</Label>
              <Input
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                placeholder="Search in names and text content"
              />
            </div>
            <div className="space-y-2">
              <Label>Node ID (optional)</Label>
              <Input
                value={searchNodeId}
                onChange={(e) => setSearchNodeId(e.target.value)}
                placeholder="Find specific node by ID"
              />
            </div>
            <div className="space-y-2">
              <Label>Component ID (optional)</Label>
              <Input
                value={searchComponentId}
                onChange={(e) => setSearchComponentId(e.target.value)}
                placeholder="Find all instances of component"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="searchCurrentPageOnly"
                checked={searchCurrentPageOnly}
                onChange={(e) => setSearchCurrentPageOnly(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="searchCurrentPageOnly" className="cursor-pointer">
                Search current page only
              </Label>
            </div>
            <Button onClick={testSearchNodes} className="w-full">
              Search Nodes
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 3: NODE HIERARCHY */}
      <Section title="Section 3: Node Hierarchy" description="Navigate and manipulate node hierarchy">
        <div className="space-y-4">
          {/* Tool 6: Get Children */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">6. Get Children</h4>
            <div className="space-y-2">
              <Label>Parent Node ID</Label>
              <div className="flex gap-2">
                <Input
                  value={childrenNodeId}
                  onChange={(e) => setChildrenNodeId(e.target.value)}
                  placeholder="Enter parent node ID"
                />
                <Button onClick={() => setChildrenNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="childrenRecursive"
                checked={childrenRecursive}
                onChange={(e) => setChildrenRecursive(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="childrenRecursive" className="cursor-pointer">
                Get all descendants recursively
              </Label>
            </div>
            <Button onClick={testGetChildren} disabled={!childrenNodeId} className="w-full">
              Get Children
            </Button>
          </Card>

          {/* Tool 7: Get Parent */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">7. Get Parent</h4>
            <div className="space-y-2">
              <Label>Child Node ID</Label>
              <div className="flex gap-2">
                <Input
                  value={parentLookupNodeId}
                  onChange={(e) => setParentLookupNodeId(e.target.value)}
                  placeholder="Enter child node ID"
                />
                <Button onClick={() => setParentLookupNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <Button onClick={testGetParent} disabled={!parentLookupNodeId} className="w-full">
              Get Parent
            </Button>
          </Card>

          {/* Tool 8: Move Node */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">8. Move Node</h4>
            <div className="space-y-2">
              <Label>Node ID to Move</Label>
              <div className="flex gap-2">
                <Input
                  value={moveNodeId}
                  onChange={(e) => setMoveNodeId(e.target.value)}
                  placeholder="Node to move"
                />
                <Button onClick={() => setMoveNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label>New Parent ID</Label>
              <Input
                value={moveNewParentId}
                onChange={(e) => setMoveNewParentId(e.target.value)}
                placeholder="Destination parent"
              />
            </div>
            <div className="space-y-2">
              <Label>Index (optional)</Label>
              <Input
                type="number"
                value={moveIndex}
                onChange={(e) => setMoveIndex(e.target.value)}
                placeholder="Position in parent (leave empty for end)"
                min="0"
              />
            </div>
            <Button onClick={testMoveNode} disabled={!moveNodeId || !moveNewParentId} className="w-full">
              Move Node
            </Button>
          </Card>

          {/* Tool 9: Clone Node */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">9. Clone Node</h4>
            <div className="space-y-2">
              <Label>Node ID to Clone</Label>
              <div className="flex gap-2">
                <Input
                  value={cloneNodeId}
                  onChange={(e) => setCloneNodeId(e.target.value)}
                  placeholder="Node to duplicate"
                />
                <Button onClick={() => setCloneNodeId(nodeId)} size="sm" variant="outline">
                  Use Selected
                </Button>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="cloneDeep"
                checked={cloneDeep}
                onChange={(e) => setCloneDeep(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="cloneDeep" className="cursor-pointer">
                Deep clone (include all children)
              </Label>
            </div>
            <Button onClick={testCloneNode} disabled={!cloneNodeId} className="w-full">
              Clone Node
            </Button>
          </Card>
        </div>
      </Section>

      <Separator />

      {/* SECTION 4: PAGE MANAGEMENT */}
      <Section title="Section 4: Page Management" description="Manage Figma pages">
        <div className="space-y-4">
          {/* Tool 10: Get Current Page Nodes */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">10. Get Current Page Nodes</h4>
            <p className="text-sm text-muted-foreground">
              Get all top-level nodes on the current page
            </p>
            <Button onClick={testGetCurrentPageNodes} className="w-full">
              Get Current Page Nodes
            </Button>
          </Card>

          {/* Tool 11: Get All Pages */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">11. Get All Pages</h4>
            <p className="text-sm text-muted-foreground">
              List all pages in the current document
            </p>
            <Button onClick={testGetAllPages} className="w-full">
              Get All Pages
            </Button>
          </Card>

          {/* Tool 12: Switch Page */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">12. Switch Page</h4>
            <div className="space-y-2">
              <Label>Page ID (or use name below)</Label>
              <Input
                value={switchPageId}
                onChange={(e) => setSwitchPageId(e.target.value)}
                placeholder="Enter page ID"
              />
            </div>
            <div className="space-y-2">
              <Label>Page Name (or use ID above)</Label>
              <Input
                value={switchPageName}
                onChange={(e) => setSwitchPageName(e.target.value)}
                placeholder="Enter page name"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Provide either Page ID or Page Name (not both)
            </p>
            <Button onClick={testSwitchPage} disabled={!switchPageId && !switchPageName} className="w-full">
              Switch Page
            </Button>
          </Card>

          {/* Tool 13: Create Page */}
          <Card className="p-4 space-y-3">
            <h4 className="font-semibold">13. Create Page</h4>
            <div className="space-y-2">
              <Label>Page Name (optional)</Label>
              <Input
                value={createPageName}
                onChange={(e) => setCreatePageName(e.target.value)}
                placeholder="Defaults to 'Page N'"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="createPageSwitch"
                checked={createPageSwitch}
                onChange={(e) => setCreatePageSwitch(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="createPageSwitch" className="cursor-pointer">
                Switch to new page after creation
              </Label>
            </div>
            <Button onClick={testCreatePage} className="w-full">
              Create Page
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
        <p>Tip: Use "Get Selected" to quickly fill node IDs from your current Figma selection.</p>
        <p>Note: All 13 hierarchy and query tools are available in this panel.</p>
      </div>
    </div>
  );
};
