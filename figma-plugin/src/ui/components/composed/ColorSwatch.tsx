/**
 * ColorSwatch Component
 *
 * A reusable component for displaying color swatches
 * with name and hex value.
 */

import React from 'react';
import { cn } from '@/ui/lib/utils';

interface ColorSwatchProps {
  color: string;
  name: string;
  size?: 'sm' | 'md' | 'lg';
  showHex?: boolean;
  className?: string;
  onClick?: () => void;
}

const sizeMap = {
  sm: 'h-8 w-8',
  md: 'h-12 w-12',
  lg: 'h-16 w-16',
};

export const ColorSwatch: React.FC<ColorSwatchProps> = ({
  color,
  name,
  size = 'md',
  showHex = true,
  className,
  onClick,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center gap-2 cursor-pointer transition-transform hover:scale-105',
        className
      )}
      onClick={onClick}
    >
      <div
        className={cn(
          'rounded-lg border border-border shadow-sm',
          sizeMap[size]
        )}
        style={{ backgroundColor: color }}
        title={`${name} - ${color}`}
      />
      <div className="flex flex-col items-center gap-0.5">
        <span className="text-xs font-medium">
          {name}
        </span>
        {showHex && (
          <span className="text-xs text-muted-foreground font-mono">
            {color}
          </span>
        )}
      </div>
    </div>
  );
};
