/**
 * Section Component
 *
 * A reusable section component for grouping related content
 * with a consistent header, description, and content area.
 */

import React from 'react';
import { cn } from '@/ui/lib/utils';
import { Separator } from '@/ui/components/ui/separator';

interface SectionProps {
  title?: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
  headerClassName?: string;
  contentClassName?: string;
  withSeparator?: boolean;
  action?: React.ReactNode;
}

export const Section: React.FC<SectionProps> = ({
  title,
  description,
  children,
  className,
  headerClassName,
  contentClassName,
  withSeparator = false,
  action,
}) => {
  return (
    <div className={cn('space-y-4', className)}>
      {(title || description || action) && (
        <>
          <div className={cn('flex items-start justify-between gap-4', headerClassName)}>
            <div className="space-y-1 flex-1">
              {title && (
                <h3 className="text-lg font-semibold">
                  {title}
                </h3>
              )}
              {description && (
                <p className="text-sm text-muted-foreground">
                  {description}
                </p>
              )}
            </div>
            {action && <div className="flex-shrink-0">{action}</div>}
          </div>
          {withSeparator && <Separator />}
        </>
      )}
      <div className={cn(contentClassName)}>
        {children}
      </div>
    </div>
  );
};
