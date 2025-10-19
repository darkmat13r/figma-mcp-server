# Figma Plugin ES Compatibility Fix

**Date**: 2025-01-19
**Issue**: Syntax error with nullish coalescing operator (`??`)
**Status**: ✅ **FIXED**

---

## 🐛 Problem

When loading the plugin in Figma, the following error occurred:

```
Syntax error on line 283: Unexpected token ?
frame.resize(params[ParamNames.WIDTH] ?? Defaults.DEFAULT_WIDTH, ...)
                                       ^
Error: Syntax error on line 283: Unexpected token ?
```

### Root Cause

The Figma plugin runtime doesn't support ES2020 features, specifically the **nullish coalescing operator** (`??`) introduced in ES2020.

The plugin code was using:
```typescript
params[ParamNames.WIDTH] ?? Defaults.DEFAULT_WIDTH
```

But Figma's JavaScript environment only supports up to **ES2017** features.

---

## ✅ Solution

### 1. **Replaced Nullish Coalescing Operators**

Changed all `??` operators to explicit ternary expressions:

**Before (ES2020)**:
```typescript
const width = params[ParamNames.WIDTH] ?? Defaults.DEFAULT_WIDTH;
```

**After (ES2017-compatible)**:
```typescript
const width = params[ParamNames.WIDTH] !== undefined ? params[ParamNames.WIDTH] : Defaults.DEFAULT_WIDTH;
```

### 2. **Updated TypeScript Target**

Changed `tsconfig.json` to target ES2017 instead of ES2020:

**Before**:
```json
{
  "compilerOptions": {
    "target": "ES2020",
    "lib": ["ES2020", "DOM"],
    // ...
  }
}
```

**After**:
```json
{
  "compilerOptions": {
    "target": "ES2017",
    "lib": ["ES2017", "DOM"],
    // ...
  }
}
```

---

## 📝 Files Modified

### `/figma-plugin/src/plugin/nodeHandlers.ts`
Replaced 8 instances of `??` operator in the following functions:
- `createFrame` (line 103-104)
- `createComponent` (line 127-128)
- `createRectangle` (line 173-174)
- `createEllipse` (line 189-190)
- `createText` (line 205-206)
- `createPolygon` (line 249)
- `createStar` (line 269)
- `createLine` (line 288-289)

### `/figma-plugin/tsconfig.json`
- Changed `target` from `ES2020` to `ES2017`
- Changed `lib` from `["ES2020", "DOM"]` to `["ES2017", "DOM"]`

---

## 🧪 Verification

### Build Results

```bash
$ npm run build

ui (webpack 5.102.1) compiled successfully in 4103 ms ✅
plugin (webpack 5.102.1) compiled successfully in 1983 ms ✅

Output:
- code.js: 7.76 KiB (increased from 7.46 KiB due to ternary expansion)
```

### Figma Runtime Test

The plugin now loads successfully in Figma without syntax errors.

---

## 📊 ES Feature Compatibility

### Supported in Figma (ES2017)
- ✅ `async`/`await`
- ✅ Object spread (`{...obj}`)
- ✅ Array spread (`[...arr]`)
- ✅ Arrow functions
- ✅ Template literals
- ✅ Destructuring
- ✅ `const`/`let`
- ✅ Classes
- ✅ Promises

### NOT Supported in Figma (ES2020+)
- ❌ Nullish coalescing (`??`)
- ❌ Optional chaining (`?.`)
- ❌ `BigInt`
- ❌ `globalThis`
- ❌ `String.prototype.matchAll`
- ❌ Dynamic imports (partially)

---

## 🎯 Best Practices for Figma Plugin Development

### 1. **Always Use ES2017 Target**
```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "ES2017",
    "lib": ["ES2017", "DOM"]
  }
}
```

### 2. **Avoid Modern Operators**

**Instead of nullish coalescing**:
```typescript
// ❌ Don't use
const value = param ?? defaultValue;

// ✅ Use
const value = param !== undefined ? param : defaultValue;
```

**Instead of optional chaining**:
```typescript
// ❌ Don't use
const name = obj?.nested?.property;

// ✅ Use
const name = obj && obj.nested && obj.nested.property;
```

### 3. **Test in Figma Early**

Always test the built plugin in Figma before extensive development to catch compatibility issues early.

### 4. **Use Babel for Transpilation (Alternative)**

If you need modern features, configure Babel to transpile to ES2017:

```javascript
// webpack.config.js
module.exports = {
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: [
                ['@babel/preset-env', { targets: { chrome: '62' } }]
              ]
            }
          },
          'ts-loader'
        ]
      }
    ]
  }
};
```

---

## 🔍 Impact Analysis

### Code Size Impact
- **Before**: 7.46 KiB
- **After**: 7.76 KiB
- **Increase**: +0.30 KiB (+4%)

The slight increase is due to ternary expressions being more verbose than `??` operator.

### Performance Impact
- **Negligible**: Both approaches have similar runtime performance
- Ternary expressions are actually native ES5 features, potentially faster in Figma's runtime

### Maintainability Impact
- **Neutral**: Code is slightly more verbose but equally readable
- Follows established JavaScript patterns pre-ES2020

---

## ✅ Verification Checklist

- [x] Replaced all `??` operators with ternary expressions
- [x] Updated `tsconfig.json` target to ES2017
- [x] Build completes without errors
- [x] No TypeScript compilation errors
- [x] Plugin loads in Figma without syntax errors
- [x] All 12 Category 1 tools remain functional

---

## 📚 References

- [Figma Plugin API - Browser Environment](https://www.figma.com/plugin-docs/setup/#browser-environment)
- [ES2017 Specification](https://262.ecma-international.org/8.0/)
- [MDN - Nullish Coalescing Operator](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Nullish_coalescing_operator)

---

## 🎉 Status

**Status**: ✅ **FIXED AND VERIFIED**

The Figma plugin is now compatible with Figma's ES2017 runtime environment and all 12 Category 1 tools work correctly.

**Build**: Passing
**Figma Runtime**: Compatible
**Tools**: All 12 functional
