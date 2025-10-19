/**
 * FormField Component
 *
 * A reusable form field component that combines Label and Input
 * with consistent spacing and error handling.
 */

import React from 'react';
import { Label } from '@/ui/components/ui/label';
import { Input } from '@/ui/components/ui/input';
import { cn } from '@/ui/lib/utils';

interface FormFieldProps {
  label: string;
  id: string;
  type?: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  disabled?: boolean;
  error?: string;
  required?: boolean;
  helperText?: string;
  className?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  id,
  type = 'text',
  value,
  onChange,
  placeholder,
  disabled = false,
  error,
  required = false,
  helperText,
  className,
}) => {
  return (
    <div className={cn('space-y-2', className)}>
      <Label htmlFor={id} className="text-sm font-medium">
        {label}
        {required && <span className="ml-1 text-destructive">*</span>}
      </Label>
      <Input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className={cn(error && 'border-destructive focus-visible:ring-destructive')}
      />
      {error && (
        <p className="text-xs text-destructive">{error}</p>
      )}
      {helperText && !error && (
        <p className="text-xs text-muted-foreground">{helperText}</p>
      )}
    </div>
  );
};
