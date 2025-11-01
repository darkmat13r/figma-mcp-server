# Icon Handlers Test Specification

## Overview

This document describes the test cases for the `iconHandlers.ts` module, following TDD principles. These tests should be implemented when a testing framework (Jest, Vitest, etc.) is added to the project.

## Test Setup

```typescript
import { handleCreateLucideIcon } from './iconHandlers';
import { ParamNames, ErrorMessages } from './constants';

// Mock Figma API
global.figma = {
  createNodeFromSvg: jest.fn(),
  createFrame: jest.fn(),
  createVector: jest.fn(),
  currentPage: {
    appendChild: jest.fn(),
  },
};

// Mock DOMParser
global.DOMParser = jest.fn();
```

## Test Suites

### 1. SVG Parsing Tests

#### Test: Parse valid SVG with viewBox
```typescript
describe('parseSVGContent', () => {
  it('should parse SVG with viewBox and extract paths', () => {
    const svgContent = `
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">
        <path d="M2 9.5a5.5 5.5 0 0 1 9.591-3.676"/>
      </svg>
    `;

    const result = parseSVGContent(svgContent);

    expect(result.viewBox).toEqual({ x: 0, y: 0, width: 24, height: 24 });
    expect(result.paths).toHaveLength(1);
    expect(result.paths[0].d).toBe('M2 9.5a5.5 5.5 0 0 1 9.591-3.676');
  });
});
```

#### Test: Parse SVG with multiple paths
```typescript
it('should parse SVG with multiple paths', () => {
  const svgContent = `
    <svg viewBox="0 0 24 24">
      <path d="M1 1" stroke="currentColor"/>
      <path d="M2 2" fill="none"/>
      <path d="M3 3" stroke="#000000" stroke-width="2"/>
    </svg>
  `;

  const result = parseSVGContent(svgContent);

  expect(result.paths).toHaveLength(3);
  expect(result.paths[0].stroke).toBe('currentColor');
  expect(result.paths[2].strokeWidth).toBe(2);
});
```

#### Test: Error on invalid SVG
```typescript
it('should throw error for invalid SVG', () => {
  const invalidSVG = '<not-valid-svg>';

  expect(() => parseSVGContent(invalidSVG))
    .toThrow(ErrorMessages.INVALID_SVG);
});
```

#### Test: Error on SVG without viewBox
```typescript
it('should throw error for SVG without viewBox', () => {
  const svgContent = '<svg><path d="M1 1"/></svg>';

  expect(() => parseSVGContent(svgContent))
    .toThrow('SVG must have a viewBox attribute');
});
```

#### Test: Error on SVG without paths
```typescript
it('should throw error for SVG without paths', () => {
  const svgContent = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/></svg>';

  expect(() => parseSVGContent(svgContent))
    .toThrow(ErrorMessages.NO_SVG_PATHS);
});
```

### 2. ViewBox Parsing Tests

#### Test: Parse standard viewBox format
```typescript
describe('parseViewBox', () => {
  it('should parse standard viewBox format', () => {
    const result = parseViewBox('0 0 24 24');

    expect(result).toEqual({ x: 0, y: 0, width: 24, height: 24 });
  });

  it('should parse viewBox with negative values', () => {
    const result = parseViewBox('-10 -10 100 100');

    expect(result).toEqual({ x: -10, y: -10, width: 100, height: 100 });
  });

  it('should throw error for invalid format', () => {
    expect(() => parseViewBox('0 0 24'))
      .toThrow('Invalid viewBox format');
  });
});
```

### 3. Color Parsing Tests

#### Test: Parse hex colors
```typescript
describe('parseHexColor', () => {
  it('should parse 6-digit hex color', () => {
    const result = parseHexColor('#FF5733');

    expect(result.r).toBeCloseTo(1.0, 2);
    expect(result.g).toBeCloseTo(0.34, 2);
    expect(result.b).toBeCloseTo(0.2, 2);
  });

  it('should parse 3-digit hex color', () => {
    const result = parseHexColor('#F53');

    expect(result.r).toBeCloseTo(1.0, 2);
    expect(result.g).toBeCloseTo(0.33, 2);
    expect(result.b).toBeCloseTo(0.2, 2);
  });

  it('should parse hex without # prefix', () => {
    const result = parseHexColor('FF5733');

    expect(result.r).toBeCloseTo(1.0, 2);
  });

  it('should throw error for invalid hex', () => {
    expect(() => parseHexColor('#GGGGGG'))
      .toThrow('Invalid hex color');
  });
});
```

### 4. Vector Node Creation Tests

#### Test: Create vector from path data
```typescript
describe('createVectorFromPath', () => {
  it('should create vector node with path data', () => {
    const mockVector = {
      vectorPaths: [],
    };
    figma.createVector.mockReturnValue(mockVector);

    const pathData = { d: 'M1 1 L2 2' };
    const viewBox = { x: 0, y: 0, width: 24, height: 24 };

    const result = createVectorFromPath(pathData, viewBox);

    expect(figma.createVector).toHaveBeenCalled();
    expect(result.vectorPaths).toEqual([{
      windingRule: 'NONZERO',
      data: 'M1 1 L2 2',
    }]);
  });
});
```

### 5. Color Application Tests

#### Test: Apply fill color
```typescript
describe('applyVectorColor', () => {
  it('should apply fill color to vector', () => {
    const mockVector = { fills: [], strokes: [] };
    const pathData = { d: 'M1 1', fill: '#FF0000' };

    applyVectorColor(mockVector, pathData);

    expect(mockVector.fills).toHaveLength(1);
    expect(mockVector.fills[0].type).toBe('SOLID');
    expect(mockVector.fills[0].color.r).toBeCloseTo(1.0, 2);
  });

  it('should apply stroke color to vector', () => {
    const mockVector = { fills: [], strokes: [] };
    const pathData = { d: 'M1 1', stroke: '#00FF00', strokeWidth: 2 };

    applyVectorColor(mockVector, pathData);

    expect(mockVector.strokes).toHaveLength(1);
    expect(mockVector.strokes[0].color.g).toBeCloseTo(1.0, 2);
    expect(mockVector.strokeWeight).toBe(2);
  });

  it('should prioritize user color over SVG color', () => {
    const mockVector = { fills: [] };
    const pathData = { d: 'M1 1', fill: '#FF0000' };
    const userColor = '#0000FF';

    applyVectorColor(mockVector, pathData, userColor);

    expect(mockVector.fills[0].color.b).toBeCloseTo(1.0, 2);
  });
});
```

### 6. Main Handler Tests

#### Test: Create icon with valid parameters
```typescript
describe('handleCreateLucideIcon', () => {
  it('should create icon node with valid parameters', async () => {
    const mockFrame = {
      id: 'test-node-id',
      name: '',
      x: 0,
      y: 0,
      width: 24,
      height: 24,
      appendChild: jest.fn(),
      resize: jest.fn(),
    };

    figma.createNodeFromSvg.mockReturnValue(mockFrame);

    const params = {
      iconName: 'heart',
      svgContent: '<svg viewBox="0 0 24 24"><path d="M1 1"/></svg>',
      size: 48,
      x: 100,
      y: 200,
    };

    const result = await handleCreateLucideIcon(params);

    expect(result.iconNodeId).toBe('test-node-id');
    expect(result.iconName).toBe('heart');
    expect(mockFrame.name).toBe('heart');
    expect(mockFrame.x).toBe(100);
    expect(mockFrame.y).toBe(200);
    expect(figma.currentPage.appendChild).toHaveBeenCalledWith(mockFrame);
  });
});
```

#### Test: Error on missing iconName
```typescript
it('should throw error when iconName is missing', async () => {
  const params = {
    svgContent: '<svg viewBox="0 0 24 24"><path d="M1 1"/></svg>',
  };

  await expect(handleCreateLucideIcon(params))
    .rejects
    .toThrow(ErrorMessages.missingParam(ParamNames.ICON_NAME));
});
```

#### Test: Error on missing svgContent
```typescript
it('should throw error when svgContent is missing', async () => {
  const params = {
    iconName: 'heart',
  };

  await expect(handleCreateLucideIcon(params))
    .rejects
    .toThrow(ErrorMessages.missingParam(ParamNames.SVG_CONTENT));
});
```

#### Test: Default values
```typescript
it('should use default values for optional parameters', async () => {
  const mockFrame = {
    id: 'test-id',
    name: '',
    x: 0,
    y: 0,
    width: 24,
    height: 24,
    appendChild: jest.fn(),
    resize: jest.fn(),
  };

  figma.createNodeFromSvg.mockReturnValue(mockFrame);

  const params = {
    iconName: 'star',
    svgContent: '<svg viewBox="0 0 24 24"><path d="M1 1"/></svg>',
  };

  const result = await handleCreateLucideIcon(params);

  expect(mockFrame.x).toBe(0);
  expect(mockFrame.y).toBe(0);
  // Size default is 24, so resize is called with scale factor
});
```

#### Test: Resize to target size
```typescript
it('should resize icon to target size', async () => {
  const mockFrame = {
    id: 'test-id',
    name: '',
    x: 0,
    y: 0,
    width: 24,
    height: 24,
    appendChild: jest.fn(),
    resize: jest.fn(),
  };

  figma.createNodeFromSvg.mockReturnValue(mockFrame);

  const params = {
    iconName: 'icon',
    svgContent: '<svg viewBox="0 0 24 24"><path d="M1 1"/></svg>',
    size: 48,
  };

  await handleCreateLucideIcon(params);

  expect(mockFrame.resize).toHaveBeenCalledWith(48, 48);
});
```

#### Test: Fallback to manual parsing
```typescript
it('should fallback to manual parsing when native parser fails', async () => {
  figma.createNodeFromSvg.mockImplementation(() => {
    throw new Error('Native parser failed');
  });

  const mockFrame = { id: 'frame-id', appendChild: jest.fn() };
  const mockVector = { id: 'vector-id' };

  figma.createFrame.mockReturnValue(mockFrame);
  figma.createVector.mockReturnValue(mockVector);

  const params = {
    iconName: 'icon',
    svgContent: '<svg viewBox="0 0 24 24"><path d="M1 1"/></svg>',
  };

  const result = await handleCreateLucideIcon(params);

  expect(figma.createFrame).toHaveBeenCalled();
  expect(figma.createVector).toHaveBeenCalled();
  expect(mockFrame.appendChild).toHaveBeenCalledWith(mockVector);
});
```

### 7. Integration Tests

#### Test: Real Lucide icon SVG
```typescript
describe('Integration: Real Lucide Icons', () => {
  it('should create heart icon from Lucide SVG', async () => {
    const heartSVG = `
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
      </svg>
    `;

    const params = {
      iconName: 'heart',
      svgContent: heartSVG,
      color: '#FF0000',
      size: 32,
    };

    const result = await handleCreateLucideIcon(params);

    expect(result.iconName).toBe('heart');
    expect(result.iconNodeId).toBeTruthy();
  });

  it('should create check icon from Lucide SVG', async () => {
    const checkSVG = `
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="20 6 9 17 4 12"/>
      </svg>
    `;

    // Note: Lucide uses polyline, but the handler should convert or handle it
    // This test verifies error handling or transformation

    const params = {
      iconName: 'check',
      svgContent: checkSVG,
    };

    // Should either succeed or provide clear error
    const result = await handleCreateLucideIcon(params);
    expect(result).toBeDefined();
  });
});
```

## Test Coverage Goals

- **Line Coverage**: > 90%
- **Branch Coverage**: > 85%
- **Function Coverage**: 100%

## Key Testing Principles Applied

1. **Test-First**: Tests written before implementation
2. **Single Responsibility**: Each test tests one specific behavior
3. **Arrange-Act-Assert**: Clear test structure
4. **Independence**: Tests don't depend on each other
5. **Meaningful Names**: Test names describe expected behavior
6. **Edge Cases**: Tests cover error conditions and boundary cases

## Running Tests (When Infrastructure Added)

```bash
# Install test dependencies
npm install --save-dev jest @types/jest ts-jest @figma/plugin-typings

# Run tests
npm test

# Run tests with coverage
npm test -- --coverage

# Run tests in watch mode
npm test -- --watch
```

## Notes

- The Figma Plugin API doesn't support all DOM APIs, so some mocking is required
- DOMParser may need to be polyfilled or mocked for the test environment
- Vector path creation is complex and may require integration tests with actual Figma
- Consider adding visual regression tests for icon appearance
