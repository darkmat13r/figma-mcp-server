# Design System

A comprehensive design system for the Figma MCP Bridge plugin, built with shadcn/ui components and Tailwind CSS.

## Overview

This design system provides a consistent, reusable, and maintainable component library that ensures visual consistency across the entire plugin interface.

## Architecture

```
design-system/
├── tokens.ts          # Design tokens (colors, spacing, typography, etc.)
├── typography.ts      # Typography system and text styles
└── index.ts          # Central export point

components/
├── ui/               # Base shadcn/ui components
│   ├── button.tsx
│   ├── input.tsx
│   ├── card.tsx
│   ├── badge.tsx
│   ├── alert.tsx
│   ├── tabs.tsx
│   ├── label.tsx
│   ├── separator.tsx
│   ├── scroll-area.tsx
│   ├── switch.tsx
│   └── select.tsx
│
└── composed/         # Composed/custom components
    ├── StatusBadge.tsx
    ├── FormField.tsx
    ├── Section.tsx
    ├── ColorSwatch.tsx
    ├── EmptyState.tsx
    └── index.ts
```

## Design Tokens

### Colors

The design system includes a comprehensive color palette:

**Primary Colors** - Main brand colors
```typescript
import { colors } from '@/ui/design-system';

colors.primary[500] // #0EA5E9
colors.primary[600] // #0284C7
```

**Semantic Colors** - For specific contexts
```typescript
colors.success[500] // #22C55E
colors.warning[500] // #F59E0B
colors.error[500]   // #EF4444
colors.info[500]    // #3B82F6
```

**Figma Theme Colors** - Figma-specific dark theme
```typescript
colors.figma.bg              // #2C2C2C
colors.figma.bgSecondary     // #1E1E1E
colors.figma.border          // #3C3C3C
colors.figma.text            // #FFFFFF
colors.figma.textSecondary   // #B3B3B3
```

### Spacing

Consistent spacing values based on a 4px grid:

```typescript
import { spacing } from '@/ui/design-system';

spacing[1]  // 0.25rem (4px)
spacing[2]  // 0.5rem (8px)
spacing[4]  // 1rem (16px)
spacing[6]  // 1.5rem (24px)
```

### Typography

Predefined text styles for consistent typography:

```typescript
import { textStyles, getTextStyleClass } from '@/ui/design-system';

// Using className
<h1 className={getTextStyleClass('h1')}>Heading</h1>

// Using style object
<span style={textStyles.body}>Body text</span>
```

Available text styles:
- `displayLarge`, `displayMedium`, `displaySmall`
- `h1`, `h2`, `h3`, `h4`, `h5`, `h6`
- `bodyLarge`, `body`, `bodySmall`
- `label`, `labelSmall`
- `caption`, `captionSmall`
- `code`, `codeSmall`

## Component Library

### Base Components (shadcn/ui)

These are the foundational components from shadcn/ui:

#### Button
```tsx
import { Button } from '@/ui/components/ui/button';

<Button variant="default">Click me</Button>
<Button variant="destructive" size="sm">Delete</Button>
```

Variants: `default`, `destructive`, `outline`, `secondary`, `ghost`, `link`
Sizes: `default`, `sm`, `lg`, `icon`

#### Input
```tsx
import { Input } from '@/ui/components/ui/input';

<Input
  type="text"
  placeholder="Enter value..."
  value={value}
  onChange={(e) => setValue(e.target.value)}
/>
```

#### Card
```tsx
import { Card, CardHeader, CardTitle, CardContent } from '@/ui/components/ui/card';

<Card>
  <CardHeader>
    <CardTitle>Title</CardTitle>
  </CardHeader>
  <CardContent>
    Content here
  </CardContent>
</Card>
```

### Composed Components

Higher-level components built from base components:

#### StatusBadge
Displays status with visual indicators.

```tsx
import { StatusBadge } from '@/ui/components/composed';

<StatusBadge status="success" label="Connected" />
<StatusBadge status="error" label="Failed" showDot={true} />
```

Status types: `success`, `error`, `warning`, `info`, `neutral`, `loading`

#### FormField
Complete form field with label, input, and error handling.

```tsx
import { FormField } from '@/ui/components/composed';

<FormField
  label="Server URL"
  id="server-url"
  value={url}
  onChange={setUrl}
  placeholder="ws://localhost:8080"
  error={errorMessage}
  required
/>
```

#### Section
Container for grouping related content.

```tsx
import { Section } from '@/ui/components/composed';

<Section
  title="Settings"
  description="Configure your preferences"
  action={<Button>Save</Button>}
  withSeparator
>
  {children}
</Section>
```

#### ColorSwatch
Display color samples with name and hex value.

```tsx
import { ColorSwatch } from '@/ui/components/composed';

<ColorSwatch
  color="#0EA5E9"
  name="Primary"
  size="md"
  showHex={true}
  onClick={handleClick}
/>
```

Sizes: `sm`, `md`, `lg`

#### EmptyState
Placeholder for empty content areas.

```tsx
import { EmptyState } from '@/ui/components/composed';

<EmptyState
  title="No items found"
  description="Try adjusting your filters"
  action={{
    label: "Create New",
    onClick: handleCreate
  }}
/>
```

## Usage Guidelines

### Importing Components

Always use the centralized imports:

```tsx
// Design system
import { colors, spacing, textStyles } from '@/ui/design-system';

// Base components
import { Button, Input, Card } from '@/ui/components/ui/button';

// Composed components
import { StatusBadge, FormField, Section } from '@/ui/components/composed';
```

### Color Usage

Use Tailwind classes with design tokens:

```tsx
// Figma theme colors
<div className="bg-figma-bg text-figma-text border-figma-border">
  <p className="text-figma-text-secondary">Secondary text</p>
</div>

// Semantic colors
<div className="bg-primary-500 hover:bg-primary-600">
  <span className="text-error-500">Error message</span>
</div>
```

### Spacing

Use Tailwind spacing utilities:

```tsx
<div className="p-4 space-y-3">
  <div className="mb-2">Item 1</div>
  <div className="gap-4 flex">Items with gap</div>
</div>
```

### Typography

Use text styles for consistency:

```tsx
import { getTextStyleClass } from '@/ui/design-system';

<h1 className={getTextStyleClass('h1')}>Main Heading</h1>
<p className={getTextStyleClass('body')}>Body text</p>
<span className={getTextStyleClass('caption')}>Small caption</span>
```

## Component File Structure

Each component should follow this structure:

```tsx
/**
 * ComponentName Component
 *
 * Brief description of the component's purpose.
 * Explain when and how to use it.
 */

import React from 'react';
import { cn } from '@/ui/lib/utils';
// ... other imports

interface ComponentNameProps {
  // Props definition
}

export const ComponentName: React.FC<ComponentNameProps> = ({
  // Destructured props
}) => {
  // Component logic

  return (
    // JSX
  );
};
```

## Best Practices

1. **Consistency**: Always use design tokens instead of hardcoded values
2. **Composition**: Build complex components from simpler ones
3. **Accessibility**: Ensure all components are keyboard-navigable and screen-reader friendly
4. **Documentation**: Add JSDoc comments to all components
5. **Type Safety**: Use TypeScript for all component props
6. **Reusability**: Keep components generic and configurable
7. **File Organization**: One component per file, in its own directory if it has multiple files

## Theming

The design system uses CSS variables for theming. The Figma plugin uses a dark theme by default:

```css
:root {
  --background: 0 0% 17%;
  --foreground: 0 0% 100%;
  --primary: 199 89% 48%;
  /* ... more variables */
}
```

Colors can be customized by modifying the CSS variables in `styles.css`.

## Examples

### Creating a New Feature Panel

```tsx
import React from 'react';
import { Section } from '@/ui/components/composed';
import { Card, CardContent } from '@/ui/components/ui/card';
import { Button } from '@/ui/components/ui/button';

export const MyFeaturePanel: React.FC = () => {
  return (
    <div className="p-4">
      <Section
        title="My Feature"
        description="This is a new feature panel"
      >
        <Card className="bg-figma-bg border-figma-border">
          <CardContent className="p-4">
            <p className="text-sm text-figma-text-secondary mb-4">
              Feature content goes here
            </p>
            <Button className="w-full">
              Take Action
            </Button>
          </CardContent>
        </Card>
      </Section>
    </div>
  );
};
```

### Creating a Form

```tsx
import React, { useState } from 'react';
import { FormField, Section } from '@/ui/components/composed';
import { Button } from '@/ui/components/ui/button';

export const SettingsForm: React.FC = () => {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');

  return (
    <Section title="Settings">
      <div className="space-y-4">
        <FormField
          label="Name"
          id="name"
          value={name}
          onChange={setName}
          required
        />
        <FormField
          label="Email"
          id="email"
          type="email"
          value={email}
          onChange={setEmail}
          required
        />
        <Button className="w-full">Save Settings</Button>
      </div>
    </Section>
  );
};
```

## Maintenance

When adding new components:

1. Create the component in the appropriate directory (`ui/` or `composed/`)
2. Add comprehensive JSDoc documentation
3. Export from the index file
4. Update this README with usage examples
5. Ensure it follows the design system tokens
6. Add TypeScript types for all props

## Resources

- [shadcn/ui Documentation](https://ui.shadcn.com)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [Radix UI Primitives](https://www.radix-ui.com/primitives)
