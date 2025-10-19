/**
 * DesignSystemPanel Component
 *
 * Displays design system tools for creating color and typography styles.
 * Uses design system components for consistent styling.
 */

import React from 'react';
import { ColorToken, TextStyleToken } from '../types';
import { Button } from '@/ui/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/ui/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/ui/components/ui/tabs';
import { ScrollArea } from '@/ui/components/ui/scroll-area';
import { Section } from '@/ui/components/composed/Section';
import { ColorSwatch } from '@/ui/components/composed/ColorSwatch';

interface DesignSystemPanelProps {
  connected: boolean;
}

// Sample color palette from design tokens
const sampleColors = [
  { name: 'Primary', hex: '#0EA5E9', description: 'Primary brand color' },
  { name: 'Primary Hover', hex: '#0284C7', description: 'Primary hover state' },
  { name: 'Primary Active', hex: '#0369A1', description: 'Primary active state' },
  { name: 'Secondary', hex: '#A855F7', description: 'Secondary accent' },
  { name: 'Neutral Light', hex: '#F9FAFB', description: 'Light background' },
  { name: 'Neutral Dark', hex: '#111827', description: 'Dark text' },
];

// Sample typography styles
const sampleTypographyStyles = [
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

export const DesignSystemPanel: React.FC<DesignSystemPanelProps> = ({
  connected,
}) => {

  const handleCreateColorStyles = () => {
    // Convert sample colors to ColorToken format
    const colors: ColorToken[] = sampleColors.map(color => ({
      name: `color/${color.name.toLowerCase().replace(/\s+/g, '-')}`,
      hex: color.hex,
      description: color.description,
    }));

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
    const styles: TextStyleToken[] = sampleTypographyStyles;

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
    <ScrollArea className="flex-1">
      <div className="p-4">
        <Section
          title="Design System"
          description="Create and manage design tokens for your Figma project"
        >
          <Tabs defaultValue="colors" className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="colors">Colors</TabsTrigger>
              <TabsTrigger value="typography">Typography</TabsTrigger>
            </TabsList>

            <TabsContent value="colors" className="space-y-4 mt-4">
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Color Palette</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <p className="text-xs text-muted-foreground">
                    Generate color styles from the predefined palette below.
                  </p>

                  <div className="grid grid-cols-3 gap-4">
                    {sampleColors.map((color) => (
                      <ColorSwatch
                        key={color.hex}
                        color={color.hex}
                        name={color.name}
                        size="sm"
                        showHex={false}
                      />
                    ))}
                  </div>

                  <Button
                    onClick={handleCreateColorStyles}
                    className="w-full"
                    size="sm"
                  >
                    Create Color Styles
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>

            <TabsContent value="typography" className="space-y-4 mt-4">
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Typography Styles</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <p className="text-xs text-muted-foreground">
                    Create responsive text styles for mobile and tablet designs.
                  </p>

                  <div className="space-y-3 border border-border rounded-lg p-4 bg-muted/30">
                    {sampleTypographyStyles.map((style) => (
                      <div key={style.name} className="space-y-1">
                        <div
                          className="text-foreground"
                          style={{
                            fontSize: `${style.fontSize * 0.5}px`,
                            fontWeight: style.fontWeight,
                            lineHeight: style.lineHeight,
                          }}
                        >
                          {style.name}
                        </div>
                        <div className="text-xs text-muted-foreground font-mono">
                          {style.fontSize}px / {style.fontWeight}
                        </div>
                      </div>
                    ))}
                  </div>

                  <Button
                    onClick={handleCreateTextStyles}
                    className="w-full"
                    size="sm"
                  >
                    Create Text Styles
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>
          </Tabs>
        </Section>
      </div>
    </ScrollArea>
  );
};
