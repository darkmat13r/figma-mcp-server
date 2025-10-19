/**
 * SelectionPanel Component
 *
 * Displays currently selected Figma nodes with their properties.
 * Uses design system components for consistent styling.
 */

import React from 'react';
import { SelectionInfo } from '../types';
import { Card, CardContent } from '@/ui/components/ui/card';
import { Badge } from '@/ui/components/ui/badge';
import { ScrollArea } from '@/ui/components/ui/scroll-area';
import { Section } from '@/ui/components/composed/Section';
import { EmptyState } from '@/ui/components/composed/EmptyState';

interface SelectionPanelProps {
  selection: SelectionInfo[];
}

export const SelectionPanel: React.FC<SelectionPanelProps> = ({ selection }) => {
  return (
    <div className="border-t border-figma-border bg-figma-bg-secondary">
      <ScrollArea className="max-h-64">
        <div className="p-4">
          <Section
            title={`Selection (${selection.length})`}
            className="space-y-3"
          >
            {selection.length === 0 ? (
              <EmptyState
                title="No nodes selected"
                description="Select nodes in the canvas to see their properties here"
                className="py-8"
              />
            ) : (
              <div className="space-y-2">
                {selection.map((node) => (
                  <Card
                    key={node.id}
                    className="bg-figma-bg border-figma-border hover:border-primary/50 transition-colors"
                  >
                    <CardContent className="p-3">
                      <div className="flex items-start justify-between gap-3">
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <h4 className="text-sm font-medium text-figma-text truncate">
                              {node.name}
                            </h4>
                            <Badge variant="outline" className="text-xs shrink-0">
                              {node.type}
                            </Badge>
                          </div>
                          <div className="flex items-center gap-3 text-xs text-figma-text-secondary">
                            <span className="flex items-center gap-1">
                              <span className="text-figma-text-tertiary">Size:</span>
                              {Math.round(node.width)} × {Math.round(node.height)}
                            </span>
                            <span className="flex items-center gap-1">
                              <span className="text-figma-text-tertiary">Pos:</span>
                              ({Math.round(node.x)}, {Math.round(node.y)})
                            </span>
                          </div>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </Section>
        </div>
      </ScrollArea>
    </div>
  );
};
