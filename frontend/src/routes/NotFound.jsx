import React from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { Button } from '../components/ui/Button'

export function NotFound() {
  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center bg-background px-4 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-foreground font-black text-2xl mb-4">
        404
      </div>
      <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
        Page Not Found
      </h1>
      <p className="mt-2 text-sm text-muted-foreground max-w-sm">
        The page you are looking for doesn't exist, has been removed, or is temporarily unavailable.
      </p>
      <div className="mt-6">
        <Link to="/dashboard">
          <Button variant="default">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Dashboard
          </Button>
        </Link>
      </div>
    </div>
  )
}

export default NotFound
