import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  Calendar,
  Users,
  Bell,
  BookOpen,
  Search,
  Briefcase,
  LifeBuoy,
  User,
  LogOut,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import { cn } from '../../lib/utils'

const navItems = [
  { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { name: 'Events', path: '/events', icon: Calendar },
  { name: 'Clubs', path: '/clubs', icon: Users },
  { name: 'Announcements', path: '/announcements', icon: Bell },
  { name: 'Resources', path: '/resources', icon: BookOpen },
  { name: 'Lost & Found', path: '/lost-found', icon: Search },
  { name: 'Opportunities', path: '/opportunities', icon: Briefcase },
  { name: 'Help Desk', path: '/help-desk', icon: LifeBuoy },
  { name: 'Profile', path: '/profile', icon: User },
]

export function Sidebar({ className }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <aside
      className={cn(
        'hidden md:flex flex-col w-64 border-r border-border/60 bg-card/60 backdrop-blur-md h-screen sticky top-0 justify-between p-4',
        className
      )}
    >
      <div>
        {/* Brand Header */}
        <div className="flex items-center gap-3 px-3 py-3 mb-6">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-primary-foreground font-black text-lg shadow-xs">
            C
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-base tracking-tight text-foreground">
                CampusOS
              </span>
              <span className="text-[10px] font-medium tracking-wide uppercase px-1.5 py-0.5 rounded bg-muted text-muted-foreground">
                v1.0
              </span>
            </div>
            <p className="text-xs text-muted-foreground truncate">Student Operating System</p>
          </div>
        </div>

        {/* Navigation list */}
        <nav className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all group',
                    isActive
                      ? 'bg-primary text-primary-foreground shadow-xs'
                      : 'text-muted-foreground hover:text-foreground hover:bg-accent/70'
                  )
                }
              >
                <Icon className="h-4 w-4 shrink-0 transition-transform group-hover:scale-105" />
                <span>{item.name}</span>
              </NavLink>
            )
          })}
        </nav>
      </div>

      {/* User profile footer */}
      <div className="pt-4 border-t border-border/50">
        <div className="flex items-center justify-between p-2 rounded-lg bg-accent/40">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary font-semibold text-xs border border-primary/20">
              {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-xs font-semibold truncate text-foreground">{user?.name}</p>
              <p className="text-[11px] text-muted-foreground truncate capitalize">{user?.role?.toLowerCase()}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            title="Sign out"
            className="p-1.5 rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </aside>
  )
}
