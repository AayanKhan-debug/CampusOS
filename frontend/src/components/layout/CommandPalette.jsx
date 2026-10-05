import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
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
  Sun,
  Moon,
  LogOut,
  X,
} from 'lucide-react'
import { useTheme } from '../../context/ThemeContext'
import { useAuth } from '../../context/AuthContext'
import { cn } from '../../lib/utils'

export function CommandPalette({ isOpen, onClose }) {
  const [query, setQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)
  const inputRef = useRef(null)
  const navigate = useNavigate()
  const { theme, toggleTheme } = useTheme()
  const { logout } = useAuth()

  const commands = [
    { label: 'Go to Dashboard', icon: LayoutDashboard, category: 'Navigation', action: () => navigate('/dashboard') },
    { label: 'Go to Events', icon: Calendar, category: 'Navigation', action: () => navigate('/events') },
    { label: 'Go to Clubs', icon: Users, category: 'Navigation', action: () => navigate('/clubs') },
    { label: 'Go to Announcements', icon: Bell, category: 'Navigation', action: () => navigate('/announcements') },
    { label: 'Go to Resource Hub', icon: BookOpen, category: 'Navigation', action: () => navigate('/resources') },
    { label: 'Go to Lost & Found', icon: Search, category: 'Navigation', action: () => navigate('/lost-found') },
    { label: 'Go to Opportunities', icon: Briefcase, category: 'Navigation', action: () => navigate('/opportunities') },
    { label: 'Go to Campus Help Desk', icon: LifeBuoy, category: 'Navigation', action: () => navigate('/help-desk') },
    { label: 'Go to Profile', icon: User, category: 'Navigation', action: () => navigate('/profile') },
    {
      label: theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode',
      icon: theme === 'dark' ? Sun : Moon,
      category: 'Preferences',
      action: () => toggleTheme(),
    },
    {
      label: 'Sign Out',
      icon: LogOut,
      category: 'Account',
      action: () => {
        logout()
        navigate('/login')
      },
    },
  ]

  const filtered = commands.filter((cmd) =>
    cmd.label.toLowerCase().includes(query.toLowerCase())
  )

  useEffect(() => {
    if (isOpen) {
      setQuery('')
      setSelectedIndex(0)
      setTimeout(() => inputRef.current?.focus(), 50)
    }
  }, [isOpen])

  // Handle keyboard events (Escape, Enter, Arrows)
  useEffect(() => {
    if (!isOpen) return

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose()
      } else if (e.key === 'ArrowDown') {
        e.preventDefault()
        setSelectedIndex((prev) => (prev < filtered.length - 1 ? prev + 1 : 0))
      } else if (e.key === 'ArrowUp') {
        e.preventDefault()
        setSelectedIndex((prev) => (prev > 0 ? prev - 1 : filtered.length - 1))
      } else if (e.key === 'Enter') {
        e.preventDefault()
        if (filtered[selectedIndex]) {
          filtered[selectedIndex].action()
          onClose()
        }
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, filtered, selectedIndex, onClose])

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-20 bg-black/60 backdrop-blur-xs p-4 animate-in fade-in-0 duration-150">
      <div
        className="w-full max-w-lg rounded-xl border border-border bg-card shadow-2xl overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Search input header */}
        <div className="flex items-center px-4 border-b border-border">
          <Search className="h-4 w-4 text-muted-foreground shrink-0 mr-3" />
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value)
              setSelectedIndex(0)
            }}
            placeholder="Type a command or jump to..."
            className="w-full bg-transparent py-3.5 text-sm outline-hidden placeholder:text-muted-foreground text-foreground"
          />
          <button
            onClick={onClose}
            className="p-1 rounded text-muted-foreground hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Command list */}
        <div className="max-h-72 overflow-y-auto p-2 space-y-1">
          {filtered.length === 0 ? (
            <p className="p-4 text-center text-xs text-muted-foreground">
              No matching commands found.
            </p>
          ) : (
            filtered.map((cmd, idx) => {
              const Icon = cmd.icon
              const isSelected = idx === selectedIndex
              return (
                <div
                  key={cmd.label}
                  onClick={() => {
                    cmd.action()
                    onClose()
                  }}
                  onMouseEnter={() => setSelectedIndex(idx)}
                  className={cn(
                    'flex items-center justify-between px-3 py-2.5 rounded-lg text-sm cursor-pointer transition-colors',
                    isSelected
                      ? 'bg-primary text-primary-foreground font-medium'
                      : 'text-foreground hover:bg-muted'
                  )}
                >
                  <div className="flex items-center gap-3">
                    <Icon className="h-4 w-4 shrink-0" />
                    <span>{cmd.label}</span>
                  </div>
                  <span
                    className={cn(
                      'text-[10px] tracking-wide uppercase',
                      isSelected ? 'text-primary-foreground/80' : 'text-muted-foreground'
                    )}
                  >
                    {cmd.category}
                  </span>
                </div>
              )
            })
          )}
        </div>

        <div className="border-t border-border/60 bg-muted/30 px-4 py-2 text-[11px] text-muted-foreground flex justify-between">
          <span>Use ↑ and ↓ to navigate</span>
          <span>Press ESC to close</span>
        </div>
      </div>
    </div>
  )
}
