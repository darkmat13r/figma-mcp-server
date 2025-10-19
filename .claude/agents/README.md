# Custom Agent Documentation

This directory contains customized agents for the FigmaMcp project.

## react-tdd-engineer

**Purpose**: React component development with Test-Driven Development methodology and SOLID principles

**Key Features**:
- Component Index tracking to prevent duplicate components
- Mandatory COMPONENT_INDEX.md checking before creating new components
- Automatic documentation of new components in the index
- TDD workflow with tests-first approach
- SOLID principles enforcement
- Centralized theming and DRY principles

### Component Index Integration

The agent is configured to:

1. **Always check** `/src/ui/components/COMPONENT_INDEX.md` before creating new components
2. **Search** for similar existing components across:
   - Base Components (shadcn/ui)
   - Composed Components
   - Feature Panels
   - Design Tokens
3. **Reuse** existing components when possible
4. **Document** new components immediately after creation

### Workflow Steps

When creating a new component, the agent will:

1. Read COMPONENT_INDEX.md
2. Report findings of similar components
3. Recommend reusing existing components if applicable
4. Create new component only if justified
5. Update COMPONENT_INDEX.md with complete documentation:
   - File path
   - Purpose description
   - Props interface (TypeScript)
   - Usage example
   - Used sub-components

### Example Output

```
📖 Checking Component Index for existing components...
Reading: /src/ui/components/COMPONENT_INDEX.md

Found relevant components:
- StatusBadge: Has 'loading' status option
- EmptyState: Can show loading states

Decision: StatusBadge with status="loading" may fulfill the requirement.
```

### Quality Assurance Checklist

Before marking work complete, the agent verifies:
- ✅ Component Index checked for duplicates
- ✅ Component Index updated if new component created
- ✅ All tests pass
- ✅ No hardcoded values
- ✅ SOLID principles followed
- ✅ TypeScript types complete

## Customizing Agents

To customize agents in this project:

1. Edit `.claude/agents/<agent-name>.md`
2. Modify the frontmatter (name, description, model)
3. Update the system prompt below the `---` separator
4. Test the agent by invoking it through Claude Code

### Frontmatter Options

```yaml
---
name: agent-name
description: When to use this agent
model: sonnet | opus | haiku
tools: [Read, Write, Edit, Bash] # optional - defaults to all tools
---
```

### Best Practices

- Keep agents focused on a single responsibility
- Provide clear examples in the description field
- Use the model that best fits the task complexity
- Document any project-specific conventions
- Include quality assurance checklists

## Project-Specific Conventions

This project uses:
- **Component Index**: `/src/ui/components/COMPONENT_INDEX.md`
- **Design System**: shadcn/ui base components
- **Testing**: React Testing Library + Jest
- **Styling**: Tailwind CSS with semantic theme colors
- **Architecture**: Atomic Design pattern

## Additional Resources

- [Claude Code Agents Documentation](https://docs.claude.com/en/docs/claude-code/sub-agents)
- [Custom Agents Guide](https://claudelog.com/mechanics/custom-agents/)
- [Subagent Best Practices](https://medianeth.dev/blog/claude-code-frameworks-subagents-2025)