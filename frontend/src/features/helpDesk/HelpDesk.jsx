import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { helpDeskApi } from '../../services/helpDeskApi'
import { PageHeader } from '../../components/common/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { Input } from '../../components/ui/Input'
import { Textarea } from '../../components/ui/Textarea'
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
  HelpCircle,
  Plus,
  CheckCircle2,
  Trash2,
  MapPin,
  Wifi,
  Zap,
  Home,
  GraduationCap,
  Building,
  Droplet,
} from 'lucide-react'

const ISSUE_CATEGORIES = [
  'INTERNET',
  'ELECTRICAL',
  'HOSTEL',
  'CLASSROOM',
  'INFRASTRUCTURE',
  'CLEANLINESS',
  'WATER',
  'OTHER',
]

const ISSUE_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

const issueSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters').max(200),
  category: z.enum([
    'INTERNET',
    'ELECTRICAL',
    'HOSTEL',
    'CLASSROOM',
    'INFRASTRUCTURE',
    'CLEANLINESS',
    'WATER',
    'OTHER',
  ]),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']),
  location: z.string().min(2, 'Location is required').max(200),
  description: z.string().min(10, 'Description must be at least 10 characters').max(5000),
})

function getCategoryIcon(cat) {
  switch (cat) {
    case 'INTERNET':
      return <Wifi className="h-4 w-4 text-blue-500" />
    case 'ELECTRICAL':
      return <Zap className="h-4 w-4 text-amber-500" />
    case 'HOSTEL':
      return <Home className="h-4 w-4 text-purple-500" />
    case 'CLASSROOM':
      return <GraduationCap className="h-4 w-4 text-emerald-500" />
    case 'INFRASTRUCTURE':
      return <Building className="h-4 w-4 text-indigo-500" />
    case 'WATER':
      return <Droplet className="h-4 w-4 text-cyan-500" />
    default:
      return <HelpCircle className="h-4 w-4 text-muted-foreground" />
  }
}

export function HelpDesk() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [categoryFilter, setCategoryFilter] = useState('ALL')
  const [page, setPage] = useState(0)

  // Dialogs
  const [isSubmitOpen, setIsSubmitOpen] = useState(false)
  const [selectedIssue, setSelectedIssue] = useState(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)

  // Form
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(issueSchema),
    defaultValues: {
      title: '',
      category: 'INTERNET',
      priority: 'MEDIUM',
      location: '',
      description: '',
    },
  })

  // Query: My Issues
  const { data: issuesData, isLoading } = useQuery({
    queryKey: ['my-issues', statusFilter, categoryFilter, page],
    queryFn: () => {
      const params = { page, size: 10 }
      if (statusFilter !== 'ALL') params.status = statusFilter
      if (categoryFilter !== 'ALL') params.category = categoryFilter
      return helpDeskApi.getMyIssues(params)
    },
  })

  // Create Ticket Mutation
  const createMutation = useMutation({
    mutationFn: (data) => helpDeskApi.createIssue(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-issues'] })
      setIsSubmitOpen(false)
      reset()
      toast.success('Help desk ticket submitted successfully!')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to submit ticket')
    },
  })

  // Close Resolved Ticket Mutation
  const closeMutation = useMutation({
    mutationFn: (id) => helpDeskApi.closeMyIssue(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-issues'] })
      setIsDetailOpen(false)
      toast.success('Ticket marked as closed')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to close ticket')
    },
  })

  // Delete Submitted Ticket Mutation
  const deleteMutation = useMutation({
    mutationFn: (id) => helpDeskApi.deleteIssue(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-issues'] })
      toast.success('Ticket withdrawn and deleted')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to delete ticket')
    },
  })

  const onSubmitTicket = (data) => {
    createMutation.mutate(data)
  }

  const issues = issuesData?.content || []

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Campus Help Desk"
        description="Report facility, network, electrical, or hostel maintenance issues"
      >
        <Button onClick={() => setIsSubmitOpen(true)} className="gap-1.5 shadow-xs">
          <Plus className="h-4 w-4" /> Submit Ticket
        </Button>
      </PageHeader>

      {/* Filter Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 bg-card p-3.5 rounded-xl border border-border/70 shadow-xs">
        <div>
          <Select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(0)
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="SUBMITTED">SUBMITTED (Pending review)</option>
            <option value="IN_PROGRESS">IN_PROGRESS (Under investigation)</option>
            <option value="RESOLVED">RESOLVED (Action taken)</option>
            <option value="CLOSED">CLOSED (Completed)</option>
          </Select>
        </div>

        <div>
          <Select
            value={categoryFilter}
            onChange={(e) => {
              setCategoryFilter(e.target.value)
              setPage(0)
            }}
          >
            <option value="ALL">All Issue Categories</option>
            {ISSUE_CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {/* Tickets List */}
      <div>
        {isLoading ? (
          <div className="space-y-3">
            {[...Array(4)].map((_, i) => (
              <Card key={i} className="p-4 space-y-2">
                <Skeleton className="h-5 w-1/3" />
                <Skeleton className="h-10 w-full" />
              </Card>
            ))}
          </div>
        ) : issues.length > 0 ? (
          <div className="space-y-3">
            {issues.map((ticket) => {
              const canDelete = ticket.status === 'SUBMITTED'
              const canClose = ticket.status === 'RESOLVED'

              return (
                <Card
                  key={ticket.id}
                  className="p-4 rounded-xl border border-border/70 hover:border-primary/40 transition-all hover:shadow-xs"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                    <div className="space-y-1.5 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="p-1 rounded-md bg-muted/60">
                          {getCategoryIcon(ticket.category)}
                        </span>
                        <Badge variant="outline" className="text-[10px] font-semibold">
                          {ticket.category}
                        </Badge>
                        <Badge
                          variant={
                            ticket.priority === 'URGENT'
                              ? 'destructive'
                              : ticket.priority === 'HIGH'
                              ? 'warning'
                              : ticket.priority === 'MEDIUM'
                              ? 'secondary'
                              : 'outline-solid'
                          }
                          className="text-[10px]"
                        >
                          {ticket.priority} Priority
                        </Badge>
                        <Badge
                          variant={
                            ticket.status === 'RESOLVED' || ticket.status === 'CLOSED'
                              ? 'success'
                              : ticket.status === 'IN_PROGRESS'
                              ? 'warning'
                              : 'default'
                          }
                          className="text-[10px]"
                        >
                          {ticket.status}
                        </Badge>
                      </div>

                      <h3
                        onClick={() => {
                          setSelectedIssue(ticket)
                          setIsDetailOpen(true)
                        }}
                        className="text-base font-bold text-foreground cursor-pointer hover:text-primary transition-colors"
                      >
                        {ticket.title}
                      </h3>

                      <p className="text-xs text-muted-foreground line-clamp-2">
                        {ticket.description}
                      </p>

                      <div className="flex items-center gap-4 text-xs text-muted-foreground pt-1">
                        <span className="flex items-center gap-1">
                          <MapPin className="h-3 w-3" /> {ticket.location}
                        </span>
                        <span>•</span>
                        <span>Reported {formatDate(ticket.createdAt)}</span>
                        {ticket.assignedTo && (
                          <>
                            <span>•</span>
                            <span>Assigned to: {ticket.assignedTo.name}</span>
                          </>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <Button
                        size="xs"
                        variant="outline"
                        onClick={() => {
                          setSelectedIssue(ticket)
                          setIsDetailOpen(true)
                        }}
                      >
                        View Details
                      </Button>

                      {canClose && (
                        <Button
                          size="xs"
                          variant="default"
                          className="bg-emerald-600 hover:bg-emerald-700 text-white"
                          onClick={() => closeMutation.mutate(ticket.id)}
                        >
                          <CheckCircle2 className="h-3.5 w-3.5 mr-1" /> Close Ticket
                        </Button>
                      )}

                      {canDelete && (
                        <Button
                          size="xs"
                          variant="ghost"
                          className="text-destructive hover:bg-destructive/10"
                          onClick={() => {
                            if (window.confirm('Withdraw this submitted ticket?')) {
                              deleteMutation.mutate(ticket.id)
                            }
                          }}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      )}
                    </div>
                  </div>
                </Card>
              )
            })}
          </div>
        ) : (
          <EmptyState
            icon={HelpCircle}
            title="No support tickets"
            description="You haven't reported any campus help desk issues matching the selected filters."
            action={
              <Button onClick={() => setIsSubmitOpen(true)}>Submit a ticket</Button>
            }
          />
        )}

        {/* Pagination */}
        {issuesData?.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <Button
              size="sm"
              variant="outline"
              disabled={issuesData.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {issuesData.pageNumber + 1} of {issuesData.totalPages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={issuesData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </div>

      {/* Submit Ticket Dialog */}
      <Dialog open={isSubmitOpen} onOpenChange={setIsSubmitOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Report Campus Issue</DialogTitle>
            <DialogDescription>
              Submit a support ticket to the campus facilities & administration desk.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit(onSubmitTicket)} className="space-y-3.5 my-2">
            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Issue Title *</label>
              <Input
                placeholder="e.g. Wi-Fi router not working in Block B 3rd floor"
                {...register('title')}
              />
              {errors.title && (
                <p className="text-[11px] text-destructive">{errors.title.message}</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Category *</label>
                <Select {...register('category')}>
                  {ISSUE_CATEGORIES.map((cat) => (
                    <option key={cat} value={cat}>
                      {cat}
                    </option>
                  ))}
                </Select>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Priority *</label>
                <Select {...register('priority')}>
                  {ISSUE_PRIORITIES.map((pri) => (
                    <option key={pri} value={pri}>
                      {pri}
                    </option>
                  ))}
                </Select>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Specific Location *</label>
              <Input
                placeholder="e.g. Hostel 4 Room 204 or Library Study Hall"
                {...register('location')}
              />
              {errors.location && (
                <p className="text-[11px] text-destructive">{errors.location.message}</p>
              )}
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Description *</label>
              <Textarea
                placeholder="Describe the issue in detail, when it began, and any symptoms..."
                rows={3}
                {...register('description')}
              />
              {errors.description && (
                <p className="text-[11px] text-destructive">{errors.description.message}</p>
              )}
            </div>

            <DialogFooter className="pt-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsSubmitOpen(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Submitting...' : 'Submit Ticket'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Ticket Detail Dialog */}
      <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <div className="flex items-center gap-2 mb-1">
              <Badge variant="outline" className="text-xs">
                {selectedIssue?.category}
              </Badge>
              <Badge variant="secondary" className="text-xs">
                {selectedIssue?.priority} Priority
              </Badge>
              <Badge
                variant={
                  selectedIssue?.status === 'RESOLVED' || selectedIssue?.status === 'CLOSED'
                    ? 'success'
                    : selectedIssue?.status === 'IN_PROGRESS'
                    ? 'warning'
                    : 'default'
                }
                className="text-xs"
              >
                {selectedIssue?.status}
              </Badge>
            </div>
            <DialogTitle className="text-lg font-bold">{selectedIssue?.title}</DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground pt-1">
              Location: {selectedIssue?.location} • Reported on{' '}
              {formatDateTime(selectedIssue?.createdAt)}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 my-2 text-xs">
            <div>
              <span className="font-semibold text-foreground block mb-1">Description:</span>
              <p className="text-muted-foreground leading-relaxed whitespace-pre-wrap">
                {selectedIssue?.description}
              </p>
            </div>

            {selectedIssue?.assignedTo && (
              <div className="p-3 bg-muted/40 rounded-lg">
                <span className="font-semibold text-foreground block mb-0.5">Assigned Staff:</span>
                <p className="text-muted-foreground">
                  {selectedIssue.assignedTo.name} ({selectedIssue.assignedTo.email})
                </p>
              </div>
            )}

            {selectedIssue?.resolutionNotes && (
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-lg">
                <span className="font-semibold text-emerald-700 dark:text-emerald-400 block mb-0.5">
                  Resolution Notes:
                </span>
                <p className="text-foreground">{selectedIssue.resolutionNotes}</p>
              </div>
            )}
          </div>

          <DialogFooter className="mt-4">
            {selectedIssue?.status === 'RESOLVED' && (
              <Button
                variant="default"
                className="bg-emerald-600 hover:bg-emerald-700 text-white"
                onClick={() => closeMutation.mutate(selectedIssue.id)}
                disabled={closeMutation.isPending}
              >
                {closeMutation.isPending ? 'Closing...' : 'Confirm Resolution & Close'}
              </Button>
            )}
            <Button variant="outline" onClick={() => setIsDetailOpen(false)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default HelpDesk
