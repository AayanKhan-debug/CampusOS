import React from 'react'
import { cn } from '../../lib/utils'

export function Badge({ className, variant = 'default', ...props }) {
  const variants = {
    default:
      'border-transparent bg-primary text-primary-foreground shadow-sm hover:bg-primary/80',
    secondary:
      'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
    destructive:
      'border-transparent bg-destructive/15 text-destructive font-medium border border-destructive/20',
    outline: 'text-foreground border border-border',
    success:
      'border-transparent bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 font-medium border border-emerald-500/20',
    warning:
      'border-transparent bg-amber-500/15 text-amber-600 dark:text-amber-400 font-medium border border-amber-500/20',
    info:
      'border-transparent bg-blue-500/15 text-blue-600 dark:text-blue-400 font-medium border border-blue-500/20',
  }

  return (
    <div
      className={cn(
        'inline-flex items-center rounded-md border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-hidden focus:ring-2 focus:ring-ring focus:ring-offset-2',
        variants[variant],
        className
      )}
      {...props}
    />
  )
}
