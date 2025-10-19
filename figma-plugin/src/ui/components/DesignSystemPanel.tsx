import React from 'react';
import { ColorToken, TextStyleToken } from '../types';
import { Button } from '@/ui/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/ui/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/ui/components/ui/tabs';

interface DesignSystemPanelProps {
  connected: boolean;
}

export const DesignSystemPanel: React.FC<DesignSystemPanelProps> = ({
  connected,
}) => {

  const handleCreateColorStyles = () => {
    // Sample color tokens
    const colors: ColorToken[] = [
      { name: 'color/primary/500', hex: '#0EA5E9', description: 'Primary brand color' },
      { name: 'color/primary/600', hex: '#0284C7', description: 'Primary hover' },
      { name: 'color/primary/700', hex: '#0369A1', description: 'Primary active' },
      { name: 'color/secondary/500', hex: '#A855F7', description: 'Secondary accent' },
      { name: 'color/neutral/50', hex: '#F9FAFB', description: 'Light background' },
      { name: 'color/neutral/900', hex: '#111827', description: 'Dark text' },
    ];

    parent.postMessage(
      {
        pluginMessage: {
          type: 'create-color-styles',
          colors,
        },
      },
      '*'
    );
  };

  const handleCreateTextStyles = () => {
    // Sample text style tokens
    const styles: TextStyleToken[] = [
      {
        name: 'mobile/h1',
        fontSize: 32,
        fontWeight: 700,
        lineHeight: 1.25,
        letterSpacing: -0.025,
        fontFamily: 'Inter',
      },
      {
        name: 'mobile/h2',
        fontSize: 28,
        fontWeight: 700,
        lineHeight: 1.25,
        letterSpacing: -0.025,
        fontFamily: 'Inter',
      },
      {
        name: 'mobile/body',
        fontSize: 16,
        fontWeight: 400,
        lineHeight: 1.5,
        letterSpacing: 0,
        fontFamily: 'Inter',
      },
      {
        name: 'tablet/h1',
        fontSize: 48,
        fontWeight: 700,
        lineHeight: 1.25,
        letterSpacing: -0.025,
        fontFamily: 'Inter',
      },
    ];

    parent.postMessage(
      {
        pluginMessage: {
          type: 'create-text-styles',
          styles,
        },
      },
      '*'
    );
  };

  return (
    <div className="flex-1 overflow-y-auto p-4">
      <div className="space-y-4">
        <h2 className="text-lg font-semibold text-figma-text">Design System</h2>

        <Tabs defaultValue="colors" className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="colors">Colors</TabsTrigger>
            <TabsTrigger value="typography">Typography</TabsTrigger>
          </TabsList>

          <TabsContent value="colors" className="space-y-4">
            <p className="text-sm text-figma-text-secondary">
              Create color styles for your design system. This will generate a set
              of predefined color tokens.
            </p>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Sample Colors</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="grid grid-cols-3 gap-2">
                  <div className="flex flex-col items-center gap-1">
                    <div className="h-8 w-8 rounded border border-gray-600" style={{ backgroundColor: '#0EA5E9' }} />
                    <span className="text-xs text-figma-text-secondary">Primary</span>
                  </div>
                  <div className="flex flex-col items-center gap-1">
                    <div className="h-8 w-8 rounded border border-gray-600" style={{ backgroundColor: '#A855F7' }} />
                    <span className="text-xs text-figma-text-secondary">Secondary</span>
                  </div>
                  <div className="flex flex-col items-center gap-1">
                    <div className="h-8 w-8 rounded border border-gray-600" style={{ backgroundColor: '#111827' }} />
                    <span className="text-xs text-figma-text-secondary">Neutral</span>
                  </div>
                </div>
                <Button onClick={handleCreateColorStyles} className="w-full" size="sm">
                  Create Color Styles
                </Button>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="typography" className="space-y-4">
            <p className="text-sm text-figma-text-secondary">
              Create text styles for mobile and tablet responsive designs.
            </p>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Sample Text Styles</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="space-y-2 text-figma-text">
                  <div className="text-2xl font-bold">Heading 1</div>
                  <div className="text-xl font-bold">Heading 2</div>
                  <div className="text-base">Body Text</div>
                </div>
                <Button onClick={handleCreateTextStyles} className="w-full" size="sm">
                  Create Text Styles
                </Button>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
};
