# Figma MCP Tools - Comprehensive Design Plan

**Goal**: Expose all essential Figma Plugin APIs via MCP tools to enable Claude Code to design complex UIs.

## Tool Organization Strategy

Tools are organized into logical categories based on Figma's API structure and common UI design workflows, from high-level UI generation to low-level API control.

---

## CATEGORY 0: HIGH-LEVEL UI GENERATION TOOLS (8 tools)

### Purpose
Generate complete UI patterns and screens with a single tool call. These tools use AI-driven composition of lower-level APIs to create production-ready designs.

### Tools

#### 0.1 `figma_generate_screen`
**Purpose**: Generate complete application screens from descriptions
```typescript
Parameters:
- description: string (required) - Detailed description of the screen
  Example: "Login screen with email/password fields, remember me checkbox, login button, and forgot password link"
- screenType?: "mobile" | "tablet" | "desktop" | "responsive" (default: "desktop")
- width?: number (auto-calculated based on screenType if not provided)
- height?: number (auto-calculated based on screenType if not provided)
- designSystem?: {
  colorScheme?: "light" | "dark" | "auto"
  primaryColor?: string (hex)
  fontFamily?: string
  cornerRadius?: number
  spacing?: number (base spacing unit)
}
Returns: {
  frameId: string
  components: {id: string, type: string, name: string}[]
}
```

#### 0.2 `figma_generate_component`
**Purpose**: Generate reusable UI components from descriptions
```typescript
Parameters:
- description: string (required)
  Example: "Primary button with hover and disabled states, medium size, rounded corners"
- componentType?: "button" | "input" | "card" | "navbar" | "modal" | "form" | "auto" (default: "auto")
- variants?: string[] (e.g., ["primary", "secondary", "danger"])
- states?: string[] (e.g., ["default", "hover", "active", "disabled"])
- responsive?: boolean
- designSystem?: DesignSystemConfig
Returns: {
  componentId: string
  variantIds?: string[]
  propertyDefinitions?: ComponentProperty[]
}
```

#### 0.3 `figma_generate_form`
**Purpose**: Generate complete form layouts
```typescript
Parameters:
- fields: FormField[] (required)
  FormField: {
    name: string
    type: "text" | "email" | "password" | "number" | "textarea" | "select" | "checkbox" | "radio" | "date"
    label: string
    placeholder?: string
    required?: boolean
    validation?: string
    options?: string[] (for select/radio)
  }
- submitButton?: {text: string, variant?: string}
- layout?: "vertical" | "horizontal" | "grid" (default: "vertical")
- width?: number
- designSystem?: DesignSystemConfig
Returns: {
  formId: string
  fieldIds: Record<string, string>
  submitButtonId?: string
}
```

#### 0.4 `figma_generate_navigation`
**Purpose**: Generate navigation bars/menus
```typescript
Parameters:
- type: "top-nav" | "side-nav" | "bottom-nav" | "breadcrumb" | "tabs" (required)
- items: NavItem[] (required)
  NavItem: {
    label: string
    icon?: string (icon name or URL)
    badge?: string | number
    children?: NavItem[] (for nested menus)
  }
- logo?: {text?: string, imageUrl?: string}
- actions?: NavItem[] (e.g., user profile, notifications)
- width?: number
- height?: number
- designSystem?: DesignSystemConfig
Returns: {
  navId: string
  itemIds: Record<string, string>
}
```

#### 0.5 `figma_generate_card_layout`
**Purpose**: Generate card-based layouts (galleries, grids, lists)
```typescript
Parameters:
- layoutType: "grid" | "masonry" | "list" | "carousel" (required)
- cardType: "image-card" | "profile-card" | "product-card" | "article-card" | "custom" (required)
- cardContent?: {
  image?: boolean
  title?: boolean
  description?: boolean
  tags?: boolean
  actions?: string[] (e.g., ["like", "share", "comment"])
}
- columns?: number (for grid)
- rows?: number
- itemSpacing?: number
- width?: number
- designSystem?: DesignSystemConfig
Returns: {
  layoutId: string
  cardComponentId: string
  cardInstanceIds: string[]
}
```

#### 0.6 `figma_generate_data_visualization`
**Purpose**: Generate charts and data visualizations
```typescript
Parameters:
- type: "bar-chart" | "line-chart" | "pie-chart" | "donut-chart" | "area-chart" | "table" (required)
- data?: {
  labels: string[]
  datasets: {name: string, values: number[], color?: string}[]
}
- width?: number (default: 600)
- height?: number (default: 400)
- legend?: boolean
- gridLines?: boolean
- colorScheme?: string[]
- designSystem?: DesignSystemConfig
Returns: {
  chartId: string
  elementIds: {legend?: string, axes?: string[], bars?: string[]}
}
```

#### 0.7 `figma_generate_modal_dialog`
**Purpose**: Generate modal dialogs and overlays
```typescript
Parameters:
- type: "alert" | "confirm" | "form" | "custom" (required)
- title: string (required)
- content?: string (description/message)
- size?: "small" | "medium" | "large" | "full"
- actions?: {label: string, variant?: "primary" | "secondary" | "danger"}[]
- customContent?: {type: string, config: any} (for complex modals)
- closable?: boolean (default: true)
- designSystem?: DesignSystemConfig
Returns: {
  modalId: string
  overlayId: string
  contentId: string
  actionIds?: Record<string, string>
}
```

#### 0.8 `figma_apply_design_system`
**Purpose**: Apply consistent design system to existing frames
```typescript
Parameters:
- targetId: string (required) - Frame or component to apply system to
- designSystem: DesignSystemConfig (required)
  DesignSystemConfig: {
    colors: {
      primary: string
      secondary?: string
      background: string
      text: string
      error?: string
      warning?: string
      success?: string
    }
    typography: {
      fontFamily: string
      headingSizes: {h1: number, h2: number, h3: number, h4: number}
      bodySize: number
      lineHeight: number
    }
    spacing: {
      unit: number (base unit, e.g., 8)
      scale: number[] (e.g., [0.5, 1, 1.5, 2, 3, 4, 6, 8])
    }
    corners: {
      small: number
      medium: number
      large: number
    }
    shadows: {
      small: Effect
      medium: Effect
      large: Effect
    }
  }
- createVariables?: boolean (create design tokens)
- createStyles?: boolean (create shared styles)
Returns: {
  variableCollectionId?: string
  styleIds?: Record<string, string>
  modifiedNodeIds: string[]
}
```

---

## CATEGORY 1: NODE CREATION TOOLS (12 tools)

### Purpose
Create various Figma node types for building UI hierarchies.

### Tools

#### 1.1 `figma_create_frame`
**Purpose**: Create container frames (primary layout building block)
```typescript
Parameters:
- name?: string
- width?: number (default: 100)
- height?: number (default: 100)
- x?: number (default: 0)
- y?: number (default: 0)
- layoutMode?: "NONE" | "HORIZONTAL" | "VERTICAL"
- fills?: Paint[]
```

#### 1.2 `figma_create_component`
**Purpose**: Create reusable component masters
```typescript
Parameters:
- name: string (required)
- width?: number
- height?: number
- description?: string
```

#### 1.3 `figma_create_instance`
**Purpose**: Create component instances
```typescript
Parameters:
- componentId: string (required) - ID of master component
- x?: number
- y?: number
```

#### 1.4 `figma_create_rectangle`
**Purpose**: Create rectangle shapes
```typescript
Parameters:
- width: number (required)
- height: number (required)
- x?: number
- y?: number
- fills?: Paint[]
- cornerRadius?: number
- strokes?: Paint[]
- strokeWeight?: number
```

#### 1.5 `figma_create_ellipse`
**Purpose**: Create circular/oval shapes
```typescript
Parameters:
- width: number (required)
- height: number (required)
- x?: number
- y?: number
- fills?: Paint[]
```

#### 1.6 `figma_create_text`
**Purpose**: Create text nodes
```typescript
Parameters:
- text: string (required)
- fontFamily?: string (default: "Inter")
- fontStyle?: string (default: "Regular")
- fontSize?: number (default: 16)
- textAlignHorizontal?: "LEFT" | "CENTER" | "RIGHT" | "JUSTIFIED"
- fills?: Paint[]
- x?: number
- y?: number
```

#### 1.7 `figma_create_polygon`
**Purpose**: Create polygon shapes
```typescript
Parameters:
- sides: number (required, 3-100)
- radius: number (required)
- x?: number
- y?: number
- fills?: Paint[]
```

#### 1.8 `figma_create_star`
**Purpose**: Create star shapes
```typescript
Parameters:
- points: number (required, 3-100)
- radius: number (required)
- innerRadius?: number
- x?: number
- y?: number
- fills?: Paint[]
```

#### 1.9 `figma_create_line`
**Purpose**: Create line shapes
```typescript
Parameters:
- x1: number (required)
- y1: number (required)
- x2: number (required)
- y2: number (required)
- strokes?: Paint[]
- strokeWeight?: number
```

#### 1.10 `figma_create_group`
**Purpose**: Create groups from selected nodes
```typescript
Parameters:
- nodeIds: string[] (required) - IDs of nodes to group
- name?: string
```

#### 1.11 `figma_create_section`
**Purpose**: Create organizational sections
```typescript
Parameters:
- name: string (required)
- width?: number
- height?: number
```

#### 1.12 `figma_create_boolean_operation`
**Purpose**: Create boolean operations (union, subtract, intersect)
```typescript
Parameters:
- operation: "UNION" | "SUBTRACT" | "INTERSECT" | "EXCLUDE"
- nodeIds: string[] (required, min 2)
```

---

## CATEGORY 2: LAYOUT MANAGEMENT TOOLS (6 tools)

### Purpose
Control auto-layout, positioning, and responsive design.

### Tools

#### 2.1 `figma_set_auto_layout`
**Purpose**: Configure auto-layout on frames
```typescript
Parameters:
- nodeId: string (required)
- layoutMode: "HORIZONTAL" | "VERTICAL" (required)
- primaryAxisAlignItems?: "MIN" | "CENTER" | "MAX" | "SPACE_BETWEEN"
- counterAxisAlignItems?: "MIN" | "CENTER" | "MAX"
- itemSpacing?: number
- paddingTop?: number
- paddingRight?: number
- paddingBottom?: number
- paddingLeft?: number
- layoutWrap?: "NO_WRAP" | "WRAP"
```

#### 2.2 `figma_set_constraints`
**Purpose**: Set responsive constraints
```typescript
Parameters:
- nodeId: string (required)
- horizontal: "MIN" | "CENTER" | "MAX" | "STRETCH" | "SCALE"
- vertical: "MIN" | "CENTER" | "MAX" | "STRETCH" | "SCALE"
```

#### 2.3 `figma_set_position`
**Purpose**: Position nodes precisely
```typescript
Parameters:
- nodeId: string (required)
- x?: number
- y?: number
- rotation?: number
```

#### 2.4 `figma_set_size`
**Purpose**: Resize nodes
```typescript
Parameters:
- nodeId: string (required)
- width?: number
- height?: number
- constrainProportions?: boolean
```

#### 2.5 `figma_arrange_nodes`
**Purpose**: Distribute and align multiple nodes
```typescript
Parameters:
- nodeIds: string[] (required)
- distribute?: "HORIZONTAL" | "VERTICAL" | "SPACING"
- align?: "LEFT" | "CENTER" | "RIGHT" | "TOP" | "MIDDLE" | "BOTTOM"
- spacing?: number
```

#### 2.6 `figma_reorder_children`
**Purpose**: Change z-index/layer order
```typescript
Parameters:
- nodeId: string (required) - Node to reorder
- newIndex: number (required) - New position in parent
```

---

## CATEGORY 3: STYLING TOOLS (8 tools)

### Purpose
Apply visual styles (colors, strokes, effects).

### Tools

#### 3.1 `figma_set_fills`
**Purpose**: Set node fill colors/gradients
```typescript
Parameters:
- nodeId: string (required)
- fills: Paint[] (required)
  - type: "SOLID" | "GRADIENT_LINEAR" | "GRADIENT_RADIAL" | "IMAGE"
  - color?: {r, g, b, a}
  - gradientStops?: Array<{color, position}>
  - imageRef?: string
  - opacity?: number
  - blendMode?: BlendMode
```

#### 3.2 `figma_set_strokes`
**Purpose**: Set node stroke/border
```typescript
Parameters:
- nodeId: string (required)
- strokes: Paint[] (required)
- strokeWeight?: number
- strokeAlign?: "INSIDE" | "CENTER" | "OUTSIDE"
- strokeCap?: "NONE" | "ROUND" | "SQUARE" | "ARROW_LINES" | "ARROW_EQUILATERAL"
- strokeJoin?: "MITER" | "BEVEL" | "ROUND"
```

#### 3.3 `figma_set_effects`
**Purpose**: Add shadows, blurs, etc.
```typescript
Parameters:
- nodeId: string (required)
- effects: Effect[] (required)
  - type: "DROP_SHADOW" | "INNER_SHADOW" | "LAYER_BLUR" | "BACKGROUND_BLUR"
  - color?: {r, g, b, a}
  - offset?: {x, y}
  - radius?: number
  - spread?: number
  - visible?: boolean
  - blendMode?: BlendMode
```

#### 3.4 `figma_set_opacity`
**Purpose**: Set node opacity and blend mode
```typescript
Parameters:
- nodeId: string (required)
- opacity: number (required, 0-1)
- blendMode?: BlendMode
```

#### 3.5 `figma_set_corner_radius`
**Purpose**: Round corners on rectangles/frames
```typescript
Parameters:
- nodeId: string (required)
- cornerRadius?: number (all corners)
- topLeftRadius?: number
- topRightRadius?: number
- bottomLeftRadius?: number
- bottomRightRadius?: number
- cornerSmoothing?: number (0-1)
```

#### 3.6 `figma_apply_paint_style`
**Purpose**: Apply existing fill/stroke styles
```typescript
Parameters:
- nodeId: string (required)
- styleType: "FILL" | "STROKE"
- styleId: string (required) - ID of existing style
```

#### 3.7 `figma_apply_effect_style`
**Purpose**: Apply existing effect styles
```typescript
Parameters:
- nodeId: string (required)
- styleId: string (required) - ID of existing effect style
```

#### 3.8 `figma_apply_text_style`
**Purpose**: Apply existing text styles
```typescript
Parameters:
- nodeId: string (required)
- styleId: string (required) - ID of existing text style
```

---

## CATEGORY 4: TYPOGRAPHY TOOLS (6 tools)

### Purpose
Control text properties and formatting.

### Tools

#### 4.1 `figma_set_text_content`
**Purpose**: Change text content
```typescript
Parameters:
- nodeId: string (required)
- characters: string (required)
- preserveFormatting?: boolean
```

#### 4.2 `figma_set_text_style`
**Purpose**: Set text formatting
```typescript
Parameters:
- nodeId: string (required)
- fontFamily?: string
- fontStyle?: string
- fontSize?: number
- fontWeight?: number
- lineHeight?: {value: number, unit: "PIXELS" | "PERCENT" | "AUTO"}
- letterSpacing?: {value: number, unit: "PIXELS" | "PERCENT"}
- textCase?: "ORIGINAL" | "UPPER" | "LOWER" | "TITLE"
- textDecoration?: "NONE" | "STRIKETHROUGH" | "UNDERLINE"
```

#### 4.3 `figma_set_text_alignment`
**Purpose**: Control text alignment
```typescript
Parameters:
- nodeId: string (required)
- textAlignHorizontal?: "LEFT" | "CENTER" | "RIGHT" | "JUSTIFIED"
- textAlignVertical?: "TOP" | "CENTER" | "BOTTOM"
```

#### 4.4 `figma_set_text_auto_resize`
**Purpose**: Control text box auto-sizing
```typescript
Parameters:
- nodeId: string (required)
- textAutoResize: "NONE" | "HEIGHT" | "WIDTH_AND_HEIGHT" (required)
- maxLines?: number
```

#### 4.5 `figma_set_text_truncation`
**Purpose**: Control text overflow
```typescript
Parameters:
- nodeId: string (required)
- textTruncation: "DISABLED" | "ENDING" (required)
- maxLines?: number
```

#### 4.6 `figma_load_font`
**Purpose**: Load fonts before use
```typescript
Parameters:
- fontFamily: string (required)
- fontStyle: string (required)
Returns: {available: boolean, message?: string}
```

---

## CATEGORY 5: COMPONENT & VARIANT TOOLS (7 tools)

### Purpose
Work with components, instances, and variants.

### Tools

#### 5.1 `figma_create_component_from_node`
**Purpose**: Convert existing node to component
```typescript
Parameters:
- nodeId: string (required)
- name?: string
- description?: string
```

#### 5.2 `figma_set_component_properties`
**Purpose**: Define component properties
```typescript
Parameters:
- componentId: string (required)
- properties: ComponentPropertyDefinition[] (required)
  - name: string
  - type: "BOOLEAN" | "TEXT" | "INSTANCE_SWAP" | "VARIANT"
  - defaultValue?: any
  - variantOptions?: string[]
```

#### 5.3 `figma_set_instance_properties`
**Purpose**: Override instance properties
```typescript
Parameters:
- instanceId: string (required)
- properties: Record<string, any> (required)
```

#### 5.4 `figma_detach_instance`
**Purpose**: Break instance link to component
```typescript
Parameters:
- instanceId: string (required)
```

#### 5.5 `figma_swap_instance`
**Purpose**: Swap to different component
```typescript
Parameters:
- instanceId: string (required)
- newComponentId: string (required)
```

#### 5.6 `figma_create_component_set`
**Purpose**: Create variant set from components
```typescript
Parameters:
- componentIds: string[] (required)
- name?: string
```

#### 5.7 `figma_add_variant`
**Purpose**: Add variant to component set
```typescript
Parameters:
- componentSetId: string (required)
- properties: Record<string, string> (required)
```

---

## CATEGORY 6: VARIABLE & TOKEN TOOLS (6 tools)

### Purpose
Work with design tokens and variables.

### Tools

#### 6.1 `figma_create_variable_collection`
**Purpose**: Create variable collection
```typescript
Parameters:
- name: string (required)
- modes?: string[] (e.g., ["Light", "Dark"])
```

#### 6.2 `figma_create_variable`
**Purpose**: Create design token variable
```typescript
Parameters:
- name: string (required)
- collectionId: string (required)
- type: "BOOLEAN" | "FLOAT" | "STRING" | "COLOR" (required)
- values?: Record<string, any> (values per mode)
```

#### 6.3 `figma_bind_variable`
**Purpose**: Bind variable to node property
```typescript
Parameters:
- nodeId: string (required)
- field: string (required) - e.g., "fills", "opacity", "width"
- variableId: string (required)
```

#### 6.4 `figma_get_variables`
**Purpose**: List available variables
```typescript
Parameters:
- collectionId?: string (filter by collection)
- type?: "BOOLEAN" | "FLOAT" | "STRING" | "COLOR"
Returns: Variable[]
```

#### 6.5 `figma_set_variable_value`
**Purpose**: Update variable value
```typescript
Parameters:
- variableId: string (required)
- value: any (required)
- modeId?: string (for specific mode)
```

#### 6.6 `figma_unbind_variable`
**Purpose**: Remove variable binding
```typescript
Parameters:
- nodeId: string (required)
- field: string (required)
```

---

## CATEGORY 7: HIERARCHY & QUERY TOOLS (13 tools)

### Purpose
Navigate, query, and manipulate node hierarchy. Get page information, search across the document, and manage pages.

### Tools

#### 7.1 `figma_get_node_info`
**Purpose**: Get detailed node information
```typescript
Parameters:
- nodeId: string (required)
Returns: {
  id, name, type,
  x, y, width, height, rotation,
  visible, locked, opacity,
  parent?, children?,
  fills?, strokes?, effects?,
  constraints?, layoutMode?, ...all relevant properties
}
```

#### 7.2 `figma_get_selection`
**Purpose**: Get currently selected nodes
```typescript
Returns: {nodes: NodeInfo[]}
```

#### 7.3 `figma_set_selection`
**Purpose**: Change selected nodes
```typescript
Parameters:
- nodeIds: string[] (required)
```

#### 7.4 `figma_find_nodes`
**Purpose**: Search for nodes by criteria
```typescript
Parameters:
- name?: string (regex pattern)
- type?: NodeType | NodeType[]
- parentId?: string (search within parent)
- recursive?: boolean (search descendants)
Returns: {nodes: NodeInfo[]}
```

#### 7.5 `figma_get_children`
**Purpose**: Get node's children
```typescript
Parameters:
- nodeId: string (required)
- recursive?: boolean
Returns: {children: NodeInfo[]}
```

#### 7.6 `figma_get_parent`
**Purpose**: Get node's parent
```typescript
Parameters:
- nodeId: string (required)
Returns: NodeInfo | null
```

#### 7.7 `figma_move_node`
**Purpose**: Move node to different parent
```typescript
Parameters:
- nodeId: string (required)
- newParentId: string (required)
- index?: number (position in new parent)
```

#### 7.8 `figma_clone_node`
**Purpose**: Duplicate node
```typescript
Parameters:
- nodeId: string (required)
- deep?: boolean (include children)
Returns: {clonedNodeId: string}
```

#### 7.9 `figma_get_current_page_nodes`
**Purpose**: Get all top-level nodes on the currently open page
```typescript
Returns: {
  pageId: string,
  pageName: string,
  nodes: NodeInfo[] (top-level children of current page)
}
```

#### 7.10 `figma_search_nodes`
**Purpose**: Advanced search for nodes by text, ID, or component instance
```typescript
Parameters:
- searchText?: string (search in node names and text content)
- nodeId?: string (find specific node by ID)
- componentId?: string (find all instances of a component)
- searchInCurrentPageOnly?: boolean (default: false, search entire document)
Returns: {
  nodes: NodeInfo[],
  totalFound: number
}
```

#### 7.11 `figma_get_all_pages`
**Purpose**: Get list of all pages in the document
```typescript
Returns: {
  pages: Array<{id: string, name: string, isCurrent: boolean}>
}
```

#### 7.12 `figma_switch_page`
**Purpose**: Switch to a different page
```typescript
Parameters:
- pageId?: string (page ID to switch to)
- pageName?: string (page name to switch to - alternative to pageId)
Returns: {
  pageId: string,
  pageName: string,
  message: string
}
```

#### 7.13 `figma_create_page`
**Purpose**: Create a new page in the document
```typescript
Parameters:
- name?: string (page name, default: "Page N")
- switchToPage?: boolean (switch to newly created page, default: true)
Returns: {
  pageId: string,
  pageName: string,
  message: string
}
```

---

## CATEGORY 8: STYLE MANAGEMENT TOOLS (5 tools)

### Purpose
Create and manage reusable styles.

### Tools

#### 8.1 `figma_create_paint_style`
**Purpose**: Create fill/stroke style
```typescript
Parameters:
- name: string (required)
- type: "FILL" | "STROKE"
- paints: Paint[] (required)
- description?: string
```

#### 8.2 `figma_create_text_style`
**Purpose**: Create text style
```typescript
Parameters:
- name: string (required)
- fontFamily: string (required)
- fontStyle: string (required)
- fontSize: number (required)
- lineHeight?: {value, unit}
- letterSpacing?: {value, unit}
- textCase?: TextCase
- textDecoration?: TextDecoration
- description?: string
```

#### 8.3 `figma_create_effect_style`
**Purpose**: Create effect style
```typescript
Parameters:
- name: string (required)
- effects: Effect[] (required)
- description?: string
```

#### 8.4 `figma_list_styles`
**Purpose**: Get all styles in document
```typescript
Parameters:
- type?: "FILL" | "STROKE" | "TEXT" | "EFFECT" | "GRID"
Returns: {styles: Style[]}
```

#### 8.5 `figma_delete_style`
**Purpose**: Remove style
```typescript
Parameters:
- styleId: string (required)
```

---

## CATEGORY 9: IMAGE & MEDIA TOOLS (4 tools)

### Purpose
Work with images and media assets.

### Tools

#### 9.1 `figma_create_image`
**Purpose**: Create image from URL or base64
```typescript
Parameters:
- imageData: string (required) - URL or base64
- width?: number
- height?: number
- x?: number
- y?: number
```

#### 9.2 `figma_set_image_fill`
**Purpose**: Apply image as fill
```typescript
Parameters:
- nodeId: string (required)
- imageData: string (required)
- scaleMode?: "FILL" | "FIT" | "CROP" | "TILE"
- rotation?: number
- opacity?: number
```

#### 9.3 `figma_export_node`
**Purpose**: Export node as image
```typescript
Parameters:
- nodeId: string (required)
- format: "PNG" | "JPG" | "SVG" | "PDF"
- scale?: number (1-4)
- constraint?: {type: "SCALE" | "WIDTH" | "HEIGHT", value: number}
Returns: {imageData: string} (base64)
```

#### 9.4 `figma_get_image_fills`
**Purpose**: Get image references from node
```typescript
Parameters:
- nodeId: string (required)
Returns: {images: ImagePaint[]}
```

---

## CATEGORY 10: UTILITY TOOLS (6 tools)

### Purpose
Helper functions and document operations.

### Tools

#### 10.1 `figma_delete_node`
**Purpose**: Remove node from document
```typescript
Parameters:
- nodeId: string (required)
```

#### 10.2 `figma_show_node`
**Purpose**: Scroll viewport to node
```typescript
Parameters:
- nodeId: string (required)
- zoom?: number
```

#### 10.3 `figma_rename_node`
**Purpose**: Change node name
```typescript
Parameters:
- nodeId: string (required)
- name: string (required)
```

#### 10.4 `figma_set_visible`
**Purpose**: Show/hide node
```typescript
Parameters:
- nodeId: string (required)
- visible: boolean (required)
```

#### 10.5 `figma_set_locked`
**Purpose**: Lock/unlock node
```typescript
Parameters:
- nodeId: string (required)
- locked: boolean (required)
```

#### 10.6 `figma_notify`
**Purpose**: Show notification to user
```typescript
Parameters:
- message: string (required)
- timeout?: number (milliseconds)
- error?: boolean
```

---

## IMPLEMENTATION PRIORITIES

### Phase 0: AI-Powered UI Generation (8 tools) - RECOMMENDED START
**Goal**: Enable rapid, description-based UI creation
**Why First**: Highest user value, uses Claude's strengths, creates complete UIs from natural language

- `figma_generate_screen` - Complete screens from descriptions
- `figma_generate_component` - Reusable components with variants
- `figma_generate_form` - Form layouts
- `figma_generate_navigation` - Nav bars and menus
- `figma_generate_card_layout` - Card grids and galleries
- `figma_generate_data_visualization` - Charts and tables
- `figma_generate_modal_dialog` - Modals and dialogs
- `figma_apply_design_system` - Apply design tokens

**Implementation Note**: These tools compose lower-level APIs internally, so they can be built progressively.

### Phase 1: Essential UI Building (20 tools)
**Goal**: Enable basic UI construction (foundation for Phase 0)
- All Category 1 (Node Creation): 12 tools
- Category 2 (Layout): 6 tools
- Category 7 (Hierarchy): figma_get_node_info, figma_get_selection

### Phase 2: Styling & Typography (14 tools)
**Goal**: Enable visual design
- All Category 3 (Styling): 8 tools
- All Category 4 (Typography): 6 tools

### Phase 3: Advanced Features (18 tools)
**Goal**: Enable design systems
- Category 5 (Components): 7 tools
- Category 6 (Variables): 6 tools
- Category 8 (Styles): 5 tools

### Phase 4: Utilities (19 tools)
**Goal**: Complete functionality
- Remaining Category 7 tools: 11 tools
- Category 9 (Images): 4 tools
- Category 10 (Utilities): 6 tools

---

## TWO IMPLEMENTATION APPROACHES

### Approach A: Top-Down (RECOMMENDED)
**Start with Phase 0, then build supporting APIs as needed**

Advantages:
- Immediate high-value functionality
- Natural language interfaces leverage Claude's strengths
- Can iterate quickly on user feedback
- Lower-level APIs built only when needed

Implementation:
1. Start with simple versions of Phase 0 tools using existing low-level tools
2. Expand Phase 0 capabilities as lower-level APIs are added
3. Progressively enhance with Phases 1-4

### Approach B: Bottom-Up (TRADITIONAL)
**Build foundation first (Phases 1-4), then Phase 0**

Advantages:
- Complete API coverage before high-level abstractions
- Easier to test individual components
- More predictable implementation path

Implementation:
1. Complete Phases 1-4 in order
2. Build Phase 0 as a composition layer on top

---

## TOTAL TOOL COUNT

**79 MCP Tools** organized into 11 categories

- **Category 0**: High-Level UI Generation (8 tools)
- **Category 1**: Node Creation (12 tools)
- **Category 2**: Layout Management (6 tools)
- **Category 3**: Styling (8 tools)
- **Category 4**: Typography (6 tools)
- **Category 5**: Components & Variants (7 tools)
- **Category 6**: Variables & Tokens (6 tools)
- **Category 7**: Hierarchy & Query (13 tools) - EXPANDED!
- **Category 8**: Style Management (5 tools)
- **Category 9**: Image & Media (4 tools)
- **Category 10**: Utilities (6 tools)

This provides comprehensive coverage from high-level UI generation to low-level Figma Plugin API control, enabling both rapid prototyping and precise design control.

---

## NEXT STEPS

1. ✅ Create this comprehensive plan with high-level UI generation tools
2. ⏳ Review and prioritize with user
3. ⏳ Implement Phase 0 (8 high-level generation tools) OR Phase 1 (20 low-level tools)
4. ⏳ Test with real UI design scenarios
5. ⏳ Iterate and expand based on usage patterns
