/**
 * StatusBadge Component
 *
 * A reusable badge component for displaying status information.
 * Supports different status types with corresponding visual styles.
 */

import React from 'react';
import { Badge } from '@/ui/components/ui/badge';
import { cn } from '@/ui/lib/utils';

export type StatusType = 'success' | 'error' | 'warning' | 'info' | 'neutral' | 'loading';

interface StatusBadgeProps {
  status: StatusType;
  label: string;
  className?: string;
  showDot?: boolean;
}

const statusConfig: Record<
  StatusType,
  {
    variant: 'default' | 'secondary' | 'destructive' | 'outline';
    dotColor: string;
  }
> = {
  success: {
    variant: 'default',
    dotColor: 'bg-green-500',
  },
  error: {
    variant: 'destructive',
    dotColor: 'bg-red-500',
  },
  warning: {
    variant: 'secondary',
    dotColor: 'bg-yellow-500',
  },
  info: {
    variant: 'default',
    dotColor: 'bg-blue-500',
  },
  neutral: {
    variant: 'secondary',
    dotColor: 'bg-gray-500',
  },
  loading: {
    variant: 'outline',
    dotColor: 'bg-blue-400 animate-pulse',
  },
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  label,
  className,
  showDot = true,
}) => {
  const config = statusConfig[status];

  return (
    <Badge variant={config.variant} className={cn('flex items-center gap-1.5', className)}>
      {showDot && (
        <span className={cn('h-2 w-2 rounded-full', config.dotColor)} />
      )}
      {label}
    </Badge>
  );
};
