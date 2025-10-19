# FigmaMcp API Catalog

This directory contains API documentation and class catalogs for the FigmaMcp project.

## Structure

- **`server/`** - Server-side Kotlin classes and APIs
- **`plugin/`** - Figma plugin TypeScript classes and APIs
- **`mcp/`** - MCP protocol definitions and interfaces

## Purpose

This catalog serves as a reference for:
- Public APIs and their signatures
- Class purposes and responsibilities
- Data structures and types
- Configuration interfaces

## Not Included

This is NOT for:
- Implementation details or step-by-step guides
- Change history or progress tracking
- Internal implementation notes

## Format

Each file documents one or more related classes using this format:

```markdown
# ClassName

**Purpose**: What this class does
**Location**: `path/to/file`
**Dependencies**: Major dependencies

## Public API
List of public methods/properties

## Usage Example
Brief usage example if needed
```

## Maintenance

Update these docs when:
- Adding new public classes or APIs
- Changing public method signatures
- Modifying data structures
- Adding configuration options
