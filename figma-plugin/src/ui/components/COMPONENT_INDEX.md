# Component Index

Quick reference guide for all available components in the design system.

## Base Components (shadcn/ui)

Located in `/src/ui/components/ui/`

| Component | File | Description | Key Props |
|-----------|------|-------------|-----------|
| **Button** | `button.tsx` | Clickable button element | `variant`, `size`, `disabled`, `onClick` |
| **Input** | `input.tsx` | Text input field | `type`, `value`, `onChange`, `placeholder`, `disabled` |
| **Card** | `card.tsx` | Container with sections | N/A (use sub-components) |
| **CardHeader** | `card.tsx` | Card header section | `className` |
| **CardTitle** | `card.tsx` | Card title | `className` |
| **CardDescription** | `card.tsx` | Card description | `className` |
| **CardContent** | `card.tsx` | Card main content | `className` |
| **CardFooter** | `card.tsx` | Card footer section | `className` |
| **Badge** | `badge.tsx` | Small status indicator | `variant` |
| **Alert** | `alert.tsx` | Alert/notification box | `variant` |
| **AlertTitle** | `alert.tsx` | Alert title | `className` |
| **AlertDescription** | `alert.tsx` | Alert description | `className` |
| **Tabs** | `tabs.tsx` | Tabbed interface | `defaultValue`, `value`, `onValueChange` |
| **TabsList** | `tabs.tsx` | Container for tab triggers | `className` |
| **TabsTrigger** | `tabs.tsx` | Individual tab button | `value` |
| **TabsContent** | `tabs.tsx` | Tab panel content | `value` |
| **Label** | `label.tsx` | Form label | `htmlFor` |
| **Separator** | `separator.tsx` | Visual divider | `orientation`, `className` |
| **ScrollArea** | `scroll-area.tsx` | Scrollable container | `className` |
| **Switch** | `switch.tsx` | Toggle switch | `checked`, `onCheckedChange`, `disabled` |
| **Select** | `select.tsx` | Dropdown select | `value`, `onValueChange` |

## Composed Components

Located in `/src/ui/components/composed/`

### StatusBadge

**File**: `StatusBadge.tsx`

**Purpose**: Display status information with visual indicators

**Props**:
```typescript
{
  status: 'success' | 'error' | 'warning' | 'info' | 'neutral' | 'loading';
  label: string;
  className?: string;
  showDot?: boolean;
}
```

**Example**:
```tsx
<StatusBadge status="success" label="Connected" showDot={true} />
```

---

### FormField

**File**: `FormField.tsx`

**Purpose**: Complete form field with label, input, and error handling

**Props**:
```typescript
{
  label: string;
  id: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  error?: string;
  required?: boolean;
  helperText?: string;
  className?: string;
}
```

**Example**:
```tsx
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

---

### Section

**File**: `Section.tsx`

**Purpose**: Container for grouping related content with consistent structure

**Props**:
```typescript
{
  title?: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
  headerClassName?: string;
  contentClassName?: string;
  withSeparator?: boolean;
  action?: React.ReactNode;
}
```

**Example**:
```tsx
<Section
  title="Settings"
  description="Configure your preferences"
  action={<Button>Save</Button>}
  withSeparator
>
  {children}
</Section>
```

---

### ColorSwatch

**File**: `ColorSwatch.tsx`

**Purpose**: Display color samples with name and hex value

**Props**:
```typescript
{
  color: string;
  name: string;
  size?: 'sm' | 'md' | 'lg';
  showHex?: boolean;
  className?: string;
  onClick?: () => void;
}
```

**Example**:
```tsx
<ColorSwatch
  color="#0EA5E9"
  name="Primary"
  size="md"
  showHex={true}
  onClick={() => console.log('Clicked')}
/>
```

---

### EmptyState

**File**: `EmptyState.tsx`

**Purpose**: Placeholder for empty content areas

**Props**:
```typescript
{
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}
```

**Example**:
```tsx
<EmptyState
  title="No items found"
  description="Try adjusting your filters"
  action={{
    label: "Create New",
    onClick: handleCreate
  }}
/>
```

## Feature Panels

Located in `/src/ui/components/`

### ConnectionPanel

**File**: `ConnectionPanel.tsx`

**Purpose**: MCP server connection management

**Props**:
```typescript
{
  connectionState: ConnectionState;
  error: string | null;
  onConnect: (url: string) => void;
  onDisconnect: () => void;
}
```

**Used Components**:
- Section
- StatusBadge
- FormField
- Button
- Alert

---

### DesignSystemPanel

**File**: `DesignSystemPanel.tsx`

**Purpose**: Design system token creation (colors, typography)

**Props**:
```typescript
{
  connected: boolean;
}
```

**Used Components**:
- Section
- ScrollArea
- Tabs, TabsList, TabsTrigger, TabsContent
- Card, CardHeader, CardTitle, CardContent
- ColorSwatch
- Button

---

### SelectionPanel

**File**: `SelectionPanel.tsx`

**Purpose**: Display selected Figma nodes

**Props**:
```typescript
{
  selection: SelectionInfo[];
}
```

**Used Components**:
- Section
- ScrollArea
- Card, CardContent
- Badge
- EmptyState

## Design Tokens

Located in `/src/ui/design-system/`

### Colors

**Import**: `import { colors } from '@/ui/design-system';`

**Available**:
- `colors.primary[50-900]`
- `colors.secondary[50-900]`
- `colors.neutral[50-900]`
- `colors.success[50, 500, 700]`
- `colors.warning[50, 500, 700]`
- `colors.error[50, 500, 700]`
- `colors.info[50, 500, 700]`
- `colors.figma.bg`, `bgSecondary`, `border`, `text`, `textSecondary`, `textTertiary`

### Spacing

**Import**: `import { spacing } from '@/ui/design-system';`

**Available**: `spacing[0-24]` (rem units)

### Typography

**Import**: `import { typography, textStyles, getTextStyleClass } from '@/ui/design-system';`

**Text Styles**:
- Display: `displayLarge`, `displayMedium`, `displaySmall`
- Headings: `h1`, `h2`, `h3`, `h4`, `h5`, `h6`
- Body: `bodyLarge`, `body`, `bodySmall`
- Labels: `label`, `labelSmall`
- Captions: `caption`, `captionSmall`
- Code: `code`, `codeSmall`

**Usage**:
```tsx
<h1 className={getTextStyleClass('h1')}>Title</h1>
```

## Utility Functions

### cn (Class Name Merger)

**File**: `/src/ui/lib/utils.ts`

**Purpose**: Merge Tailwind classes with conflict resolution

**Import**: `import { cn } from '@/ui/lib/utils';`

**Usage**:
```tsx
<div className={cn('base-classes', conditionalClass && 'extra-class', className)}>
```

## Quick Start Guide

### Creating a New Component

1. **Choose the right base**: Use shadcn/ui components as building blocks
2. **Create in composed/**: If it's a reusable pattern, add to `composed/`
3. **Use design tokens**: Import from `@/ui/design-system`
4. **Add types**: Full TypeScript support required
5. **Document**: Add JSDoc comments
6. **Export**: Add to `composed/index.ts`

### Example New Component

```tsx
/**
 * MyComponent
 *
 * Description of what this component does.
 */

import React from 'react';
import { cn } from '@/ui/lib/utils';
import { Card, CardContent } from '@/ui/components/ui/card';
import { Button } from '@/ui/components/ui/button';

interface MyComponentProps {
  title: string;
  onAction: () => void;
  className?: string;
}

export const MyComponent: React.FC<MyComponentProps> = ({
  title,
  onAction,
  className,
}) => {
  return (
    <Card className={cn('bg-figma-bg border-figma-border', className)}>
      <CardContent className="p-4">
        <h3 className="text-lg font-semibold text-figma-text mb-2">
          {title}
        </h3>
        <Button onClick={onAction} size="sm">
          Take Action
        </Button>
      </CardContent>
    </Card>
  );
};
```

## Common Patterns

### Form Layout
```tsx
<Section title="Form Title">
  <div className="space-y-4">
    <FormField label="Field 1" id="field1" value={v1} onChange={setV1} />
    <FormField label="Field 2" id="field2" value={v2} onChange={setV2} />
    <Button className="w-full">Submit</Button>
  </div>
</Section>
```

### List with Empty State
```tsx
{items.length === 0 ? (
  <EmptyState
    title="No items"
    description="Start by creating one"
    action={{ label: "Create", onClick: handleCreate }}
  />
) : (
  <div className="space-y-2">
    {items.map(item => <ItemCard key={item.id} {...item} />)}
  </div>
)}
```

### Tabbed Interface
```tsx
<Tabs defaultValue="tab1">
  <TabsList className="grid w-full grid-cols-2">
    <TabsTrigger value="tab1">Tab 1</TabsTrigger>
    <TabsTrigger value="tab2">Tab 2</TabsTrigger>
  </TabsList>
  <TabsContent value="tab1">Content 1</TabsContent>
  <TabsContent value="tab2">Content 2</TabsContent>
</Tabs>
```

### Status Display
```tsx
<div className="flex items-center justify-between">
  <span className="text-sm text-figma-text">Status</span>
  <StatusBadge status="success" label="Active" />
</div>
```
