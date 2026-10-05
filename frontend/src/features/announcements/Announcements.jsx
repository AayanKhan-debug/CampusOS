import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { announcementsApi } from '../../services/announcementsApi'
import { PageHeader } from '../../components/common/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { Input } from '../../components/ui/Input'
import { Select } from '../../components/ui/Select'
import { Skeleton } from '../../components/ui/Skeleton'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '../../components/ui/Dialog'
import { EmptyState } from '../../components/common/EmptyState'
import { formatDate, formatDateTime } from '../../lib/utils'
import { toast } from 'sonner'
import {
  Bell,
  Search,
  Pin,
  CheckCircle2,
  Calendar,
  User,
  Eye,
} from 'lucide-react'

const CATEGORIES = [
  'ALL',
  'ACADEMIC',
  'EXAM',
  'PLACEMENT',
  'EVENT',
  'CLUB',
  'GENERAL',
]

export function Announcements() {
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState('all') // 'all' | 'unread'
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('ALL')
  const [page, setPage] = useState(0)

  // Selected announcement for reading full detail in dialog
  const [selectedAnnouncement, setSelectedAnnouncement] = useState(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)

  // Query: All Announcements
  const { data: allData, isLoading: loadingAll } = useQuery({
    queryKey: ['announcements', category, search, page],
    queryFn: () => {
      const params = { page, size: 10 }
      if (category !== 'ALL') params.category = category
      if (search.trim()) params.search = search.trim()
      return announcementsApi.getAnnouncements(params)
    },
    enabled: activeTab === 'all',
  })

  // Query: Unread Announcements
  const { data: unreadData, isLoading: loadingUnread } = useQuery({
    queryKey: ['announcements-unread', page],
    queryFn: () => announcementsApi.getUnreadAnnouncements({ page, size: 10 }),
    enabled: activeTab === 'unread',
  })

  // Mark as read mutation
  const markReadMutation = useMutation({
    mutationFn: (id) => announcementsApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
      queryClient.invalidateQueries({ queryKey: ['announcements-unread'] })
      queryClient.invalidateQueries({ queryKey: ['unread-announcements'] })
      toast.success('Marked as read')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to mark as read')
    },
  })

  const currentData = activeTab === 'all' ? allData : unreadData
  const isLoading = activeTab === 'all' ? loadingAll : loadingUnread

  const handleOpenDetail = (announcement) => {
    setSelectedAnnouncement(announcement)
    setIsDetailOpen(true)
    // Automatically mark read if opening detail
    markReadMutation.mutate(announcement.id)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Campus Announcements"
        description="Official campus notices, academic schedules, exam circulars, and updates"
      >
        <div className="flex rounded-lg border border-border p-1 bg-muted/40">
          <button
            onClick={() => {
              setActiveTab('all')
              setPage(0)
            }}
            className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
              activeTab === 'all'
                ? 'bg-background text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            All Notices
          </button>
          <button
            onClick={() => {
              setActiveTab('unread')
              setPage(0)
            }}
            className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
              activeTab === 'unread'
                ? 'bg-background text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            Unread
          </button>
        </div>
      </PageHeader>

      {/* Filter row for All tab */}
      {activeTab === 'all' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 bg-card p-3.5 rounded-xl border border-border/70 shadow-xs">
          <div className="relative md:col-span-2">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search announcements..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                setPage(0)
              }}
              className="pl-9"
            />
          </div>
          <div>
            <Select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value)
                setPage(0)
              }}
            >
              {CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>
                  {cat === 'ALL' ? 'All Categories' : cat}
                </option>
              ))}
            </Select>
          </div>
        </div>
      )}

      {/* Announcements List */}
      <div>
        {isLoading ? (
          <div className="space-y-4">
            {[...Array(4)].map((_, i) => (
              <Card key={i} className="p-5 space-y-3">
                <Skeleton className="h-5 w-1/3" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-4 w-1/4" />
              </Card>
            ))}
          </div>
        ) : currentData?.content?.length > 0 ? (
          <div className="space-y-4">
            {currentData.content.map((ann) => (
              <Card
                key={ann.id}
                className={`transition-all hover:border-primary/40 ${
                  ann.isPinned
                    ? 'border-amber-500/40 bg-amber-500/3 dark:bg-amber-500/6 shadow-xs'
                    : 'border-border/70 bg-card'
                }`}
              >
                <div className="p-5 space-y-3">
                  <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1.5 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        {ann.isPinned && (
                          <Badge variant="warning" className="gap-1 text-xs">
                            <Pin className="h-3 w-3" /> Pinned Notice
                          </Badge>
                        )}
                        <Badge variant="outline" className="text-xs font-semibold">
                          {ann.category}
                        </Badge>
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <Calendar className="h-3 w-3" />
                          {formatDate(ann.publishedAt || ann.createdAt)}
                        </span>
                      </div>

                      <h3
                        onClick={() => handleOpenDetail(ann)}
                        className="text-base font-bold text-foreground cursor-pointer hover:text-primary transition-colors"
                      >
                        {ann.title}
                      </h3>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <Button
                        size="xs"
                        variant="ghost"
                        onClick={() => markReadMutation.mutate(ann.id)}
                        title="Mark as read"
                      >
                        <CheckCircle2 className="h-4 w-4 text-muted-foreground hover:text-foreground" />
                      </Button>
                      <Button
                        size="xs"
                        variant="outline"
                        onClick={() => handleOpenDetail(ann)}
                      >
                        <Eye className="mr-1 h-3.5 w-3.5" /> Read
                      </Button>
                    </div>
                  </div>

                  <p className="text-xs text-muted-foreground line-clamp-3 leading-relaxed">
                    {ann.content}
                  </p>

                  {ann.createdBy && (
                    <div className="pt-2 border-t border-border/50 text-[11px] text-muted-foreground flex items-center gap-2">
                      <User className="h-3 w-3" />
                      <span>Posted by {ann.createdBy.name}</span>
                    </div>
                  )}
                </div>
              </Card>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={Bell}
            title={activeTab === 'unread' ? 'No unread announcements' : 'No announcements found'}
            description={
              activeTab === 'unread'
                ? "You've read all published announcements. Great job staying informed!"
                : 'No announcements match your search criteria.'
            }
          />
        )}

        {/* Pagination */}
        {currentData?.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <Button
              size="sm"
              variant="outline"
              disabled={currentData.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {currentData.pageNumber + 1} of {currentData.totalPages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={currentData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </div>

      {/* Detail Dialog */}
      <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <div className="flex items-center gap-2 mb-1">
              {selectedAnnouncement?.isPinned && (
                <Badge variant="warning" className="gap-1 text-xs">
                  <Pin className="h-3 w-3" /> Pinned
                </Badge>
              )}
              <Badge variant="outline" className="text-xs">
                {selectedAnnouncement?.category}
              </Badge>
            </div>
            <DialogTitle className="text-xl font-bold leading-snug">
              {selectedAnnouncement?.title}
            </DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground pt-1 flex items-center gap-3">
              <span>Published {formatDateTime(selectedAnnouncement?.publishedAt || selectedAnnouncement?.createdAt)}</span>
              {selectedAnnouncement?.createdBy && (
                <span>• By {selectedAnnouncement.createdBy.name}</span>
              )}
            </DialogDescription>
          </DialogHeader>

          <div className="my-3 space-y-4 max-h-[60vh] overflow-y-auto pr-1">
            {selectedAnnouncement?.image && (
              <img
                src={selectedAnnouncement.image}
                alt={selectedAnnouncement.title}
                className="w-full max-h-60 rounded-lg object-cover border border-border"
              />
            )}
            <div className="text-sm text-foreground whitespace-pre-wrap leading-relaxed">
              {selectedAnnouncement?.content}
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDetailOpen(false)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default Announcements
