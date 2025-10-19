# Design System Migration Guide

This guide helps developers understand the changes and how to work with the new design system.

## What Changed

### Before (Old Approach)
- ✗ Hardcoded colors and spacing values
- ✗ Inconsistent component styling
- ✗ Direct use of Tailwind utilities without structure
- ✗ Components mixed presentation and logic
- ✗ No centralized design tokens
- ✗ Limited reusability

### After (New Approach)
- ✓ Centralized design tokens in `/src/ui/design-system/`
- ✓ Composed, reusable components
- ✓ Consistent styling across all panels
- ✓ Better separation of concerns
- ✓ Full TypeScript support
- ✓ Comprehensive documentation

## Breaking Changes

### 1. Import Paths Changed

**Before**:
```tsx
import { Button } from './ui/button';
import { Input } from './ui/input';
```

**After**:
```tsx
// Base components
import { Button, Input } from '@/ui/components/ui/button';

// Composed components
import { StatusBadge, FormField, Section } from '@/ui/components/composed';

// Design tokens
import { colors, spacing, typography } from '@/ui/design-system';
```

### 2. Badge Usage Changed

**Before**:
```tsx
<Badge variant={getStatusBadgeVariant() as any}>
  {getStatusText()}
</Badge>
```

**After**:
```tsx
<StatusBadge status="success" label="Connected" showDot={true} />
```

### 3. Form Fields Changed

**Before**:
```tsx
<Input
  type="text"
  value={value}
  onChange={(e) => setValue(e.target.value)}
  placeholder="Enter value"
/>
```

**After**:
```tsx
<FormField
  label="Field Label"
  id="field-id"
  value={value}
  onChange={setValue}
  placeholder="Enter value"
  error={errorMessage}
  required
/>
```

### 4. Section Structure Changed

**Before**:
```tsx
<div className="p-4">
  <h2 className="text-lg font-semibold">Title</h2>
  <p className="text-sm text-gray-500">Description</p>
  {children}
</div>
```

**After**:
```tsx
<Section title="Title" description="Description">
  {children}
</Section>
```

## Migration Steps for Existing Components

### Step 1: Update Imports

Replace old import paths with new ones from the design system:

```tsx
// Old
import { Button } from './ui/button';

// New
import { Button } from '@/ui/components/ui/button';
import { Section } from '@/ui/components/composed';
```

### Step 2: Replace Hardcoded Colors

```tsx
// Old
<div className="bg-gray-700 text-white border-gray-600">

// New
<div className="bg-figma-bg-secondary text-figma-text border-figma-border">
```

### Step 3: Use Composed Components

Replace common patterns with composed components:

```tsx
// Old - Manual form field
<div className="space-y-2">
  <label className="text-sm">Label</label>
  <Input value={value} onChange={(e) => setValue(e.target.value)} />
  {error && <p className="text-xs text-red-500">{error}</p>}
</div>

// New - FormField component
<FormField
  label="Label"
  id="field-id"
  value={value}
  onChange={setValue}
  error={error}
/>
```

### Step 4: Add JSDoc Comments

All new components should have documentation:

```tsx
/**
 * ComponentName
 *
 * Brief description of what this component does and when to use it.
 *
 * @example
 * <ComponentName prop="value" />
 */
```

### Step 5: Use Design Tokens

Replace magic numbers and colors with design tokens:

```tsx
// Old
const buttonStyles = {
  padding: '16px',
  color: '#0EA5E9',
  fontSize: '14px',
};

// New
import { spacing, colors, typography } from '@/ui/design-system';

const buttonStyles = {
  padding: spacing[4],
  color: colors.primary[500],
  fontSize: typography.fontSize.sm,
};
```

## Common Migration Patterns

### Pattern 1: Status Display

**Before**:
```tsx
const getVariant = () => {
  switch (state) {
    case 'connected': return 'default';
    case 'error': return 'destructive';
    default: return 'secondary';
  }
};

<Badge variant={getVariant()}>{state}</Badge>
```

**After**:
```tsx
const getStatus = (): StatusType => {
  switch (state) {
    case 'connected': return 'success';
    case 'error': return 'error';
    default: return 'neutral';
  }
};

<StatusBadge status={getStatus()} label={state} />
```

### Pattern 2: Empty States

**Before**:
```tsx
{items.length === 0 ? (
  <p className="text-sm text-gray-500">No items found</p>
) : (
  <div>{items.map(renderItem)}</div>
)}
```

**After**:
```tsx
{items.length === 0 ? (
  <EmptyState
    title="No items found"
    description="Try adjusting your filters"
  />
) : (
  <div>{items.map(renderItem)}</div>
)}
```

### Pattern 3: Color Swatches

**Before**:
```tsx
<div className="flex flex-col items-center">
  <div
    className="h-8 w-8 rounded border border-gray-600"
    style={{ backgroundColor: color }}
  />
  <span className="text-xs">{name}</span>
  <span className="text-xs font-mono">{hex}</span>
</div>
```

**After**:
```tsx
<ColorSwatch
  color={color}
  name={name}
  size="sm"
  showHex={true}
/>
```

### Pattern 4: Scrollable Content

**Before**:
```tsx
<div className="overflow-y-auto max-h-64">
  {content}
</div>
```

**After**:
```tsx
<ScrollArea className="max-h-64">
  {content}
</ScrollArea>
```

## Working with the Design System

### Adding a New Component

1. **Decide the component type**:
   - Base component? → Add to `/src/ui/components/ui/` (use shadcn/ui)
   - Composed component? → Add to `/src/ui/components/composed/`
   - Feature panel? → Add to `/src/ui/components/`

2. **Create the component file**:
```tsx
/**
 * MyNewComponent
 *
 * Description of the component.
 */

import React from 'react';
import { cn } from '@/ui/lib/utils';

interface MyNewComponentProps {
  // Props
}

export const MyNewComponent: React.FC<MyNewComponentProps> = (props) => {
  return (
    // JSX
  );
};
```

3. **Export from index**:
```tsx
// In /src/ui/components/composed/index.ts
export * from './MyNewComponent';
```

4. **Update documentation**:
   - Add to `COMPONENT_INDEX.md`
   - Add usage example to `README.md`

### Using Design Tokens

Always prefer design tokens over hardcoded values:

```tsx
// ✗ Bad
<div style={{ color: '#0EA5E9', padding: '16px' }}>

// ✓ Good
import { colors, spacing } from '@/ui/design-system';

<div style={{ color: colors.primary[500], padding: spacing[4] }}>

// ✓ Better (with Tailwind)
<div className="text-primary-500 p-4">
```

### Typography Best Practices

```tsx
import { getTextStyleClass } from '@/ui/design-system';

// ✓ Use predefined text styles
<h1 className={getTextStyleClass('h1')}>Heading</h1>
<p className={getTextStyleClass('body')}>Body text</p>

// ✓ Or use Tailwind utilities
<h2 className="text-xl font-bold">Heading</h2>
```

### Color Usage Guidelines

1. **Background colors**:
   - Main background: `bg-figma-bg`
   - Secondary background: `bg-figma-bg-secondary`
   - Card background: `bg-figma-bg` or `bg-card`

2. **Text colors**:
   - Primary text: `text-figma-text`
   - Secondary text: `text-figma-text-secondary`
   - Tertiary text: `text-figma-text-tertiary`

3. **Border colors**:
   - Default borders: `border-figma-border`
   - Interactive borders: `border-primary-500`

4. **Semantic colors**:
   - Success: `bg-green-500` or `colors.success[500]`
   - Error: `bg-red-500` or `colors.error[500]`
   - Warning: `bg-yellow-500` or `colors.warning[500]`
   - Info: `bg-blue-500` or `colors.info[500]`

## Testing Your Changes

### 1. Build Test
```bash
npm run build
```

Should compile without errors.

### 2. Type Check
```bash
npm run typecheck
```

Should pass all TypeScript checks.

### 3. Lint Check
```bash
npm run lint
```

Should follow all ESLint rules.

### 4. Visual Testing
1. Load the plugin in Figma
2. Check all panels render correctly
3. Test all interactive elements
4. Verify dark theme consistency

## Troubleshooting

### Import Errors

**Problem**: `Cannot find module '@/ui/components/composed'`

**Solution**: Ensure your `tsconfig.json` has the path alias configured:
```json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

### Color Not Working

**Problem**: `text-figma-text-tertiary` not applying color

**Solution**: Ensure the color is in `tailwind.config.js`:
```js
colors: {
  figma: {
    'text-tertiary': '#8C8C8C',
  }
}
```

### Component Not Found

**Problem**: Component import fails

**Solution**: Check that the component is exported from its index file:
```tsx
// In /src/ui/components/composed/index.ts
export * from './MyComponent';
```

## Best Practices Checklist

When creating or updating components:

- [ ] Use design tokens instead of hardcoded values
- [ ] Add TypeScript types for all props
- [ ] Include JSDoc comments
- [ ] Use composed components when possible
- [ ] Follow the established file structure
- [ ] Export from appropriate index files
- [ ] Test in both light and dark themes (if applicable)
- [ ] Ensure accessibility (keyboard navigation, ARIA labels)
- [ ] Add to component documentation
- [ ] Verify build succeeds

## Getting Help

1. **Design System Docs**: See `/src/ui/design-system/README.md`
2. **Component Index**: See `/src/ui/components/COMPONENT_INDEX.md`
3. **Examples**: Look at existing refactored panels (ConnectionPanel, DesignSystemPanel, SelectionPanel)
4. **shadcn/ui Docs**: https://ui.shadcn.com
5. **Tailwind Docs**: https://tailwindcss.com/docs

## Summary

The new design system provides:
- ✓ Consistency across all components
- ✓ Better developer experience with TypeScript
- ✓ Easier maintenance with centralized tokens
- ✓ Reusable component patterns
- ✓ Comprehensive documentation

Follow this guide when working with the codebase to ensure consistency and maintainability.
