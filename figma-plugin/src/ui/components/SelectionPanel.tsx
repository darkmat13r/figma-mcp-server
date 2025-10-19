import React from 'react';
import { SelectionInfo } from '../types';

interface SelectionPanelProps {
  selection: SelectionInfo[];
}

export const SelectionPanel: React.FC<SelectionPanelProps> = ({ selection }) => {
  return (
    <div className="border-t border-gray-700 bg-figma-bg-secondary p-4">
      <h3 className="mb-2 text-sm font-semibold text-figma-text">
        Selection ({selection.length})
      </h3>
      {selection.length === 0 ? (
        <p className="text-xs text-figma-text-secondary">
          No nodes selected
        </p>
      ) : (
        <div className="space-y-2">
          {selection.map((node) => (
            <div
              key={node.id}
              className="rounded border border-gray-700 bg-figma-bg p-2"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="text-sm font-medium text-figma-text">
                    {node.name}
                  </div>
                  <div className="text-xs text-figma-text-secondary">
                    {node.type}
                  </div>
                </div>
                <div className="text-right text-xs text-figma-text-secondary">
                  <div>{Math.round(node.width)} × {Math.round(node.height)}</div>
                  <div>({Math.round(node.x)}, {Math.round(node.y)})</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
