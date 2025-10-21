/**
 * Figma Plugin Style Utilities
 *
 * ## Purpose
 * Shared utility functions for style validation and sanitization
 * Used across node creation and style handlers
 *
 * ## SOLID Principles
 * - Single Responsibility: Each function validates one specific style type
 * - Open-Closed: New validators can be added without modifying existing ones
 */

/**
 * Validate and sanitize fill objects
 * Ensures fills conform to Figma's strict validation requirements
 */
export function validateAndSanitizeFills(fills: any[]): Paint[] {
  if (!Array.isArray(fills)) {
    throw new Error('Fills must be an array');
  }

  return fills.map((fill, index) => {
    if (!fill || typeof fill !== 'object') {
      throw new Error(`Fill at index ${index} must be an object`);
    }

    const fillType = fill.type;
    if (!fillType) {
      throw new Error(`Fill at index ${index} missing required 'type' property`);
    }

    try {
      switch (fillType) {
        case 'SOLID': {
          const color = fill.color;
          if (!color || typeof color !== 'object') {
            throw new Error(`SOLID fill at index ${index} must have a 'color' object`);
          }

          // Validate color has r, g, b (a is optional)
          if (typeof color.r !== 'number' || typeof color.g !== 'number' || typeof color.b !== 'number') {
            throw new Error(
              `SOLID fill color at index ${index} must have r, g, b properties as numbers. ` +
              `Got: ${JSON.stringify(color)}`
            );
          }

          // Build complete object with all properties at once
          const sanitizedFill: any = {
            type: 'SOLID',
            color: {
              r: color.r,
              g: color.g,
              b: color.b
            }
          };

          // Add optional properties if present
          if (typeof fill.opacity === 'number') {
            sanitizedFill.opacity = fill.opacity;
          }
          if (fill.visible !== undefined) {
            sanitizedFill.visible = Boolean(fill.visible);
          }
          if (fill.blendMode) {
            sanitizedFill.blendMode = fill.blendMode;
          }

          return sanitizedFill as SolidPaint;
        }

        case 'GRADIENT_LINEAR':
        case 'GRADIENT_RADIAL':
        case 'GRADIENT_ANGULAR':
        case 'GRADIENT_DIAMOND': {
          const gradientStops = fill.gradientStops;
          if (!Array.isArray(gradientStops) || gradientStops.length === 0) {
            throw new Error(
              `${fillType} fill at index ${index} must have 'gradientStops' array with at least one stop`
            );
          }

          // Validate gradient stops
          const sanitizedStops = gradientStops.map((stop: any, stopIndex: number) => {
            if (!stop || typeof stop !== 'object') {
              throw new Error(`Gradient stop ${stopIndex} at fill ${index} must be an object`);
            }
            if (!stop.color || typeof stop.color !== 'object') {
              throw new Error(`Gradient stop ${stopIndex} at fill ${index} must have a color object`);
            }
            if (typeof stop.position !== 'number') {
              throw new Error(`Gradient stop ${stopIndex} at fill ${index} must have a position number`);
            }

            return {
              color: {
                r: stop.color.r,
                g: stop.color.g,
                b: stop.color.b,
                a: typeof stop.color.a === 'number' ? stop.color.a : 1
              },
              position: stop.position
            };
          });

          // Gradient transform is REQUIRED for all gradients
          // If not provided, use identity transform
          const gradientTransform = fill.gradientTransform || [
            [1, 0, 0],
            [0, 1, 0]
          ];

          // Build complete object with all properties at once
          const sanitizedFill: any = {
            type: fillType,
            gradientStops: sanitizedStops,
            gradientTransform: gradientTransform
          };

          // Add optional properties
          if (typeof fill.opacity === 'number') {
            sanitizedFill.opacity = fill.opacity;
          }
          if (fill.visible !== undefined) {
            sanitizedFill.visible = Boolean(fill.visible);
          }
          if (fill.blendMode) {
            sanitizedFill.blendMode = fill.blendMode;
          }

          return sanitizedFill as GradientPaint;
        }

        case 'IMAGE': {
          if (!fill.imageHash && !fill.imageRef) {
            throw new Error(`IMAGE fill at index ${index} must have 'imageHash' property`);
          }

          // Build complete object with all properties at once
          const sanitizedFill: any = {
            type: 'IMAGE',
            scaleMode: fill.scaleMode || 'FILL',
            imageHash: fill.imageHash || fill.imageRef
          };

          // Add optional properties
          if (fill.imageTransform) {
            sanitizedFill.imageTransform = fill.imageTransform;
          }
          if (typeof fill.opacity === 'number') {
            sanitizedFill.opacity = fill.opacity;
          }
          if (fill.visible !== undefined) {
            sanitizedFill.visible = Boolean(fill.visible);
          }
          if (fill.blendMode) {
            sanitizedFill.blendMode = fill.blendMode;
          }
          if (fill.filters) {
            sanitizedFill.filters = fill.filters;
          }

          return sanitizedFill as ImagePaint;
        }

        default:
          throw new Error(`Unknown fill type '${fillType}' at index ${index}`);
      }
    } catch (error) {
      // Re-throw with context about which fill failed
      const errorMsg = error instanceof Error ? error.message : String(error);
      throw new Error(`in set_fills (${fillType}): ${errorMsg}`);
    }
  }) as Paint[];
}