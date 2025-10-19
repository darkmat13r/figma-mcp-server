# Quick Reference Guide - react-tdd-engineer Agent

## Component Index Workflow

### Every Component Creation Follows This Path:

```
1. Check Index → 2. Report Findings → 3. Decide → 4. Create (if needed) → 5. Update Index
```

---

## Step-by-Step Guide

### Step 1: Invoke the Agent

**When to use**:
- Creating new React components
- Refactoring existing components
- Building features with testing requirements
- Implementing SOLID principles

**How to invoke**:
```
User: "I need a [component description]"
Claude: "I'll use the react-tdd-engineer agent..."
```

---

### Step 2: Agent Checks Component Index

**What happens**:
```
📖 Checking Component Index for existing components...
Reading: /src/ui/components/COMPONENT_INDEX.md

Reviewing sections:
- Base Components (shadcn/ui)
- Composed Components
- Feature Panels
- Design Tokens
```

**What to expect**:
- Agent reads the entire component index
- Agent searches for similar components
- Agent reports all relevant findings

---

### Step 3: Agent Reports Findings

**Example output**:
```
Found relevant components:
- StatusBadge: Displays status with visual indicators
  - Props: status, label, showDot
  - File: composed/StatusBadge.tsx

- EmptyState: Placeholder for empty content
  - Props: title, description, action
  - File: composed/EmptyState.tsx

Decision: StatusBadge may fulfill the requirement with status="loading"
```

---

### Step 4: Agent Makes Decision

**Three possible outcomes**:

#### A. Reuse Existing Component ✅
```
✅ Recommendation: Use existing component
<StatusBadge status="loading" label="Loading..." />

This component already exists and meets your requirements.
```

#### B. Ask Clarifying Questions ❓
```
❓ Questions to clarify:
- Do you need a full-page loading state or inline?
- Should it show progress percentage?
- What animation style? (spinner, dots, bar)

Based on your answer, I can recommend the appropriate component.
```

#### C. Create New Component 🆕
```
🆕 Creating new component: LoadingSpinner

Justification:
- No existing component provides spinner animation
- StatusBadge is for status display, not loading animation
- New component serves distinct purpose (Single Responsibility)

Proceeding with TDD workflow...
```

---

### Step 5: If Creating New Component

**TDD Workflow**:
```
1. ✍️  Write failing tests
2. 🔨 Implement minimal code to pass tests
3. ♻️  Refactor while keeping tests green
4. ✅ Verify all quality checks
5. 📝 Update Component Index
```

---

### Step 6: Component Index Update

**Required information**:
```markdown
### ComponentName

**File**: `path/to/ComponentName.tsx`

**Purpose**: Brief description of what this component does

**Props**:
```typescript
{
  prop1: string;
  prop2?: number;
  prop3: 'option1' | 'option2';
}
```

**Example**:
```tsx
<ComponentName
  prop1="value"
  prop2={42}
  prop3="option1"
/>
```

**Used Components**: Button, Card, Icon
```

---

## Quality Assurance Checklist

Before completion, verify:

### Component Index
- [ ] Checked index before starting
- [ ] Searched all relevant sections
- [ ] Updated index if new component created
- [ ] Documentation format matches template

### Code Quality
- [ ] Tests written first (TDD)
- [ ] All tests passing
- [ ] No hardcoded colors/values
- [ ] Semantic Tailwind theme colors used
- [ ] TypeScript types complete
- [ ] Props properly documented

### SOLID Principles
- [ ] Single Responsibility maintained
- [ ] Component open for extension
- [ ] Proper interface segregation
- [ ] Dependencies abstracted

### Documentation
- [ ] Props interface documented
- [ ] Usage examples provided
- [ ] Purpose clearly stated
- [ ] Sub-components listed

---

## Common Scenarios

### Scenario 1: Simple Request
```
User: "I need a button component"

Agent:
1. Checks index
2. Finds: Button (ui/button.tsx)
3. Recommends: Use existing <Button variant="primary">

Result: No new component needed ✅
```

### Scenario 2: Similar Component Exists
```
User: "I need a loading indicator"

Agent:
1. Checks index
2. Finds: StatusBadge with status="loading"
3. Asks: "Is StatusBadge sufficient, or do you need animation?"

Result: Either reuse or create with justification
```

### Scenario 3: New Component Needed
```
User: "I need a pagination component"

Agent:
1. Checks index
2. Finds: No pagination component exists
3. Justifies: New component needed
4. Creates: Pagination.tsx with TDD
5. Updates: Component Index

Result: New component created and documented ✅
```

### Scenario 4: Refactoring Request
```
User: "Refactor this component to follow SOLID"

Agent:
1. Checks index for reusable components
2. Identifies: Hardcoded values to extract
3. Finds: Existing components to reuse
4. Refactors: Improves structure
5. Updates: Index if new shared components created

Result: Cleaner code using existing components ✅
```

---

## Component Categories

### Base Components
**What**: Foundational UI primitives from shadcn/ui
**Examples**: Button, Input, Card, Badge, Alert
**When to use**: Direct usage in features
**Location**: `/src/ui/components/ui/`

### Composed Components
**What**: Reusable combinations of base components
**Examples**: FormField, StatusBadge, Section, ColorSwatch
**When to use**: Common UI patterns across features
**Location**: `/src/ui/components/composed/`

### Feature Panels
**What**: Complex, feature-specific components
**Examples**: ConnectionPanel, DesignSystemPanel, SelectionPanel
**When to use**: Major feature implementations
**Location**: `/src/ui/components/`

### Design Tokens
**What**: Colors, spacing, typography constants
**Examples**: colors.primary, spacing[4], textStyles.h1
**When to use**: Everywhere (no hardcoded values!)
**Location**: `/src/ui/design-system/`

---

## Anti-Patterns to Avoid

### ❌ Skipping Index Check
```
Bad: Creating component without checking index
Good: Always read index first
```

### ❌ Creating Duplicates
```
Bad: Creating LoadingBadge when StatusBadge exists
Good: Reuse StatusBadge with status="loading"
```

### ❌ Incomplete Documentation
```
Bad: Adding to index without props/examples
Good: Complete template with all sections filled
```

### ❌ Hardcoded Values
```
Bad: className="bg-blue-600"
Good: className="bg-primary"
```

### ❌ Poor Categorization
```
Bad: Putting FormField in Feature Panels
Good: Putting FormField in Composed Components
```

---

## Tips for Success

1. **Trust the Index** - It's the single source of truth
2. **Ask Questions** - Agent will clarify requirements before creating
3. **Reuse First** - Creating new components is last resort
4. **Document Well** - Future you will thank you
5. **Follow TDD** - Tests before implementation
6. **Use Semantic Colors** - Never hardcode theme values
7. **Keep Components Focused** - Single Responsibility Principle
8. **Update Immediately** - Don't leave index out of sync

---

## Debugging

### Issue: Agent not checking index
**Solution**: Ensure COMPONENT_INDEX.md exists at `/src/ui/components/COMPONENT_INDEX.md`

### Issue: Agent creates duplicate component
**Solution**: Check if component is properly documented in index

### Issue: Index format is inconsistent
**Solution**: Follow the template exactly, including code block formatting

### Issue: Component not found in index
**Solution**: Verify component was added to correct section

---

## Quick Commands

Check if component exists:
```bash
grep -i "ComponentName" /src/ui/components/COMPONENT_INDEX.md
```

List all composed components:
```bash
sed -n '/## Composed Components/,/## Feature Panels/p' /src/ui/components/COMPONENT_INDEX.md
```

Verify index structure:
```bash
grep "^##" /src/ui/components/COMPONENT_INDEX.md
```

---

## Additional Resources

- **Agent Config**: `.claude/agents/react-tdd-engineer.md`
- **Full Changelog**: `.claude/agents/CHANGELOG.md`
- **Component Index**: `/src/ui/components/COMPONENT_INDEX.md`
- **Design System**: `/src/ui/design-system/`

---

## Summary

The react-tdd-engineer agent now:
- ✅ Checks Component Index FIRST (mandatory)
- ✅ Reports findings before creating
- ✅ Recommends reuse when possible
- ✅ Documents new components immediately
- ✅ Follows TDD methodology
- ✅ Enforces SOLID principles
- ✅ Prevents duplicate components
- ✅ Maintains clean documentation

**Result**: Better code reuse, consistent documentation, cleaner architecture