import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { eventsApi } from '../../services/eventsApi'
import { PageHeader } from '../../components/common/PageHeader'
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '../../components/ui/Card'
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
import { formatDate } from '../../lib/utils'
import { toast } from 'sonner'
import {
  Calendar,
  MapPin,
  Users,
  Search,
  CheckCircle2,
} from 'lucide-react'

const CATEGORIES = [
  'ALL',
  'TECHNICAL',
  'CULTURAL',
  'SPORTS',
  'WORKSHOP',
  'SEMINAR',
  'HACKATHON',
  'COMPETITION',
  'CLUB',
  'OTHER',
]

const STATUSES = ['ALL', 'UPCOMING', 'ONGOING', 'COMPLETED', 'CANCELLED']

export function Events() {
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState('browse') // 'browse' | 'my-events'
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('ALL')
  const [status, setStatus] = useState('ALL')
  const [page, setPage] = useState(0)

  // Registration modal state
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [actionType, setActionType] = useState(null) // 'register' | 'cancel'
  const [isDialogOpen, setIsDialogOpen] = useState(false)

  // Query: Browse Events
  const { data: eventsData, isLoading: loadingEvents } = useQuery({
    queryKey: ['events', category, status, search, page],
    queryFn: () => {
      const params = { page, size: 9 }
      if (category !== 'ALL') params.category = category
      if (status !== 'ALL') params.status = status
      if (search.trim()) params.search = search.trim()
      return eventsApi.getEvents(params)
    },
    enabled: activeTab === 'browse',
  })

  // Query: My Events
  const { data: myEventsData, isLoading: loadingMyEvents } = useQuery({
    queryKey: ['my-events', page],
    queryFn: () => eventsApi.getMyEvents({ page, size: 10 }),
    enabled: activeTab === 'my-events',
  })

  // Mutations
  const registerMutation = useMutation({
    mutationFn: (eventId) => eventsApi.registerForEvent(eventId),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['events'] })
      queryClient.invalidateQueries({ queryKey: ['my-events'] })
      setIsDialogOpen(false)
      const isWaitlisted = res.status === 'WAITLISTED'
      toast.success(
        isWaitlisted
          ? 'Event capacity reached! You have been added to the waitlist.'
          : 'Successfully registered for event!'
      )
    },
    onError: (err) => {
      toast.error(err.message || 'Registration failed')
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (eventId) => eventsApi.cancelRegistration(eventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['events'] })
      queryClient.invalidateQueries({ queryKey: ['my-events'] })
      setIsDialogOpen(false)
      toast.success('Registration cancelled')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to cancel registration')
    },
  })

  // Build a set of registered event IDs for quick lookup in browse mode
  const registeredEventMap = new Map()
  if (myEventsData?.content) {
    myEventsData.content.forEach((reg) => {
      if (reg.status !== 'CANCELLED') {
        registeredEventMap.set(reg.eventId, reg.status)
      }
    })
  }

  const handleOpenRegisterDialog = (event) => {
    setSelectedEvent(event)
    setActionType('register')
    setIsDialogOpen(true)
  }

  const handleOpenCancelDialog = (eventOrReg) => {
    setSelectedEvent(eventOrReg)
    setActionType('cancel')
    setIsDialogOpen(true)
  }

  const handleConfirmAction = () => {
    if (!selectedEvent) return
    const eventId = selectedEvent.eventId || selectedEvent.id
    if (actionType === 'register') {
      registerMutation.mutate(eventId)
    } else {
      cancelMutation.mutate(eventId)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Campus Events"
        description="Discover hackathons, guest lectures, cultural fests, and workshops"
      >
        <div className="flex rounded-lg border border-border p-1 bg-muted/40">
          <button
            onClick={() => {
              setActiveTab('browse')
              setPage(0)
            }}
            className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
              activeTab === 'browse'
                ? 'bg-background text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            All Events
          </button>
          <button
            onClick={() => {
              setActiveTab('my-events')
              setPage(0)
            }}
            className={`px-3 py-1.5 text-xs font-semibold rounded-md transition-all ${
              activeTab === 'my-events'
                ? 'bg-background text-foreground shadow-xs'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            My Registrations
          </button>
        </div>
      </PageHeader>

      {/* Filters (Browse mode) */}
      {activeTab === 'browse' && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-card p-3.5 rounded-xl border border-border/70 shadow-xs">
          <div className="relative md:col-span-2">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search events by title or keyword..."
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
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>
                  {c === 'ALL' ? 'All Categories' : c}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value)
                setPage(0)
              }}
            >
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s === 'ALL' ? 'All Statuses' : s}
                </option>
              ))}
            </Select>
          </div>
        </div>
      )}

      {/* Browse Tab Content */}
      {activeTab === 'browse' && (
        <div>
          {loadingEvents ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(6)].map((_, i) => (
                <Card key={i} className="p-4 space-y-4">
                  <Skeleton className="h-5 w-3/4" />
                  <Skeleton className="h-16 w-full" />
                  <Skeleton className="h-8 w-full" />
                </Card>
              ))}
            </div>
          ) : eventsData?.content?.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {eventsData.content.map((event) => {
                const userRegStatus = registeredEventMap.get(event.id)
                const isFull = event.capacity && event.confirmedCount >= event.capacity
                const isPastDeadline =
                  event.registrationDeadline &&
                  new Date(event.registrationDeadline).getTime() < Date.now()
                const isCancelled = event.status === 'CANCELLED'

                return (
                  <Card
                    key={event.id}
                    className="flex flex-col justify-between overflow-hidden hover:border-primary/40 transition-all hover:shadow-md"
                  >
                    <div>
                      {/* Image header if available, or category badge */}
                      {event.image ? (
                        <div className="h-36 w-full overflow-hidden bg-muted">
                          <img
                            src={event.image}
                            alt={event.title}
                            className="h-full w-full object-cover"
                          />
                        </div>
                      ) : null}

                      <CardHeader className="p-4 pb-2">
                        <div className="flex items-center justify-between gap-2 mb-2">
                          <Badge variant="outline" className="text-[10px] font-semibold">
                            {event.category}
                          </Badge>
                          <Badge
                            variant={
                              event.status === 'UPCOMING'
                                ? 'default'
                                : event.status === 'ONGOING'
                                ? 'warning'
                                : event.status === 'CANCELLED'
                                ? 'destructive'
                                : 'secondary'
                            }
                            className="text-[10px]"
                          >
                            {event.status}
                          </Badge>
                        </div>
                        <CardTitle className="text-base font-bold leading-snug line-clamp-2">
                          {event.title}
                        </CardTitle>
                        {event.clubName && (
                          <p className="text-xs font-medium text-primary">
                            By {event.clubName}
                          </p>
                        )}
                      </CardHeader>

                      <CardContent className="p-4 pt-0 space-y-3">
                        <p className="text-xs text-muted-foreground line-clamp-3">
                          {event.description}
                        </p>

                        <div className="space-y-1.5 text-xs text-muted-foreground pt-1 border-t border-border/50">
                          <div className="flex items-center gap-2">
                            <Calendar className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                            <span>{formatDate(event.date)}</span>
                            {event.startTime && <span>· {event.startTime}</span>}
                          </div>
                          <div className="flex items-center gap-2">
                            <MapPin className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                            <span className="truncate">{event.location}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <Users className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                            <span>
                              {event.confirmedCount || 0}
                              {event.capacity ? ` / ${event.capacity}` : ''} registered
                            </span>
                            {event.waitlistedCount > 0 && (
                              <span className="text-amber-500 text-[11px]">
                                ({event.waitlistedCount} waitlisted)
                              </span>
                            )}
                          </div>
                        </div>

                        {/* Capacity progress bar */}
                        {event.capacity && (
                          <div className="w-full bg-muted rounded-full h-1.5 overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                isFull ? 'bg-amber-500' : 'bg-primary'
                              }`}
                              style={{
                                width: `${Math.min(
                                  100,
                                  ((event.confirmedCount || 0) / event.capacity) * 100
                                )}%`,
                              }}
                            />
                          </div>
                        )}
                      </CardContent>
                    </div>

                    <CardFooter className="p-4 pt-0 border-t border-border/40 mt-3">
                      {userRegStatus ? (
                        <div className="flex items-center justify-between w-full">
                          <Badge
                            variant={userRegStatus === 'CONFIRMED' ? 'success' : 'warning'}
                            className="text-xs"
                          >
                            <CheckCircle2 className="mr-1 h-3 w-3" />
                            {userRegStatus}
                          </Badge>
                          <Button
                            size="xs"
                            variant="outline"
                            className="text-destructive hover:bg-destructive/10"
                            onClick={() => handleOpenCancelDialog(event)}
                          >
                            Cancel
                          </Button>
                        </div>
                      ) : isCancelled ? (
                        <Button size="sm" variant="outline" disabled className="w-full">
                          Cancelled
                        </Button>
                      ) : isPastDeadline ? (
                        <Button size="sm" variant="outline" disabled className="w-full">
                          Registration Closed
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          className="w-full"
                          variant={isFull ? 'secondary' : 'default'}
                          onClick={() => handleOpenRegisterDialog(event)}
                        >
                          {isFull ? 'Join Waitlist' : 'Register Now'}
                        </Button>
                      )}
                    </CardFooter>
                  </Card>
                )
              })}
            </div>
          ) : (
            <EmptyState
              icon={Calendar}
              title="No events found"
              description="No campus events match your active filters or search criteria."
            />
          )}

          {/* Pagination */}
          {eventsData?.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 pt-6">
              <Button
                size="sm"
                variant="outline"
                disabled={eventsData.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <span className="text-xs text-muted-foreground">
                Page {eventsData.pageNumber + 1} of {eventsData.totalPages}
              </span>
              <Button
                size="sm"
                variant="outline"
                disabled={eventsData.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </div>
      )}

      {/* My Registrations Tab Content */}
      {activeTab === 'my-events' && (
        <div className="space-y-4">
          {loadingMyEvents ? (
            <div className="space-y-3">
              {[...Array(3)].map((_, i) => (
                <Skeleton key={i} className="h-20 w-full rounded-xl" />
              ))}
            </div>
          ) : myEventsData?.content?.length > 0 ? (
            <div className="space-y-3">
              {myEventsData.content.map((reg) => (
                <div
                  key={reg.id}
                  className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl border border-border/70 bg-card hover:border-primary/40 transition-colors gap-4"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-base font-semibold text-foreground">
                        {reg.eventTitle || `Event #${reg.eventId}`}
                      </h4>
                      <Badge
                        variant={
                          reg.status === 'CONFIRMED'
                            ? 'success'
                            : reg.status === 'WAITLISTED'
                            ? 'warning'
                            : 'outline-solid'
                        }
                      >
                        {reg.status}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      Registered on {formatDate(reg.registeredAt)}
                    </p>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    {reg.status !== 'CANCELLED' && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="text-destructive hover:bg-destructive/10"
                        onClick={() => handleOpenCancelDialog(reg)}
                      >
                        Cancel Registration
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={Calendar}
              title="No registrations yet"
              description="You have not registered for any campus events yet. Explore events to get involved!"
              action={
                <Button onClick={() => setActiveTab('browse')}>Browse Events</Button>
              }
            />
          )}
        </div>
      )}

      {/* Confirmation Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {actionType === 'register' ? 'Confirm Registration' : 'Cancel Registration'}
            </DialogTitle>
            <DialogDescription>
              {actionType === 'register'
                ? `Are you sure you want to register for "${selectedEvent?.title}"?`
                : `Are you sure you want to cancel your registration for "${
                    selectedEvent?.eventTitle || selectedEvent?.title
                  }"? Your spot will be released.`}
            </DialogDescription>
          </DialogHeader>

          <DialogFooter className="mt-4">
            <Button
              variant="outline"
              onClick={() => setIsDialogOpen(false)}
              disabled={registerMutation.isPending || cancelMutation.isPending}
            >
              Back
            </Button>
            <Button
              variant={actionType === 'register' ? 'default' : 'destructive'}
              onClick={handleConfirmAction}
              disabled={registerMutation.isPending || cancelMutation.isPending}
            >
              {registerMutation.isPending || cancelMutation.isPending
                ? 'Processing...'
                : actionType === 'register'
                ? 'Confirm Register'
                : 'Yes, Cancel'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default Events
