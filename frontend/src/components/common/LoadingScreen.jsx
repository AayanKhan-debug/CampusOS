import React from 'react'
import { Loader2 } from 'lucide-react'
import { Skeleton } from '../ui/Skeleton'
import { cn } from '../../lib/utils'

export function LoadingScreen({ message = 'Loading CampusOS...', className }) {
  return (
    <div
      className={cn(
        'flex min-h-screen w-full flex-col items-center justify-center bg-background',
        className
      )}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-primary-foreground font-bold shadow-md">
        C
      </div>
      <div className="mt-4 flex items-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin text-primary" />
        <span>{message}</span>
      </div>
    </div>
  )
}

export function PageFallback() {
  return (
    <div className="space-y-6 w-full animate-fade-in py-2">
      <div className="flex items-center justify-between pb-6 border-b border-border/40">
        <div className="space-y-2">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-4 w-72" />
        </div>
        <Skeleton className="h-9 w-28 rounded-lg" />
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Skeleton className="h-32 w-full rounded-xl" />
        <Skeleton className="h-32 w-full rounded-xl" />
        <Skeleton className="h-32 w-full rounded-xl" />
      </div>
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  )
}

export default LoadingScreen
