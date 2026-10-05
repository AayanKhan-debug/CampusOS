import React from 'react'
import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  Calendar,
  Users,
  Bell,
  Search,
  User,
} from 'lucide-react'
import { cn } from '../../lib/utils'

const mobileNavItems = [
  { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { name: 'Events', path: '/events', icon: Calendar },
  { name: 'Clubs', path: '/clubs', icon: Users },
  { name: 'Notices', path: '/announcements', icon: Bell },
  { name: 'Lost/Found', path: '/lost-found', icon: Search },
  { name: 'Profile', path: '/profile', icon: User },
]

export function MobileNav() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 flex h-16 items-center justify-around border-t border-border/80 bg-background/95 px-2 backdrop-blur-md md:hidden">
      {mobileNavItems.map((item) => {
        const Icon = item.icon
        return (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              cn(
                'flex flex-col items-center justify-center gap-1 py-1 px-2 text-[11px] font-medium transition-colors',
                isActive
                  ? 'text-primary font-semibold'
                  : 'text-muted-foreground hover:text-foreground'
              )
            }
          >
            <Icon className="h-5 w-5" />
            <span>{item.name}</span>
          </NavLink>
        )
      })}
    </nav>
  )
}
