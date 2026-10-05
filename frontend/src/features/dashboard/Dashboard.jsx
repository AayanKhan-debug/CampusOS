import React from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../context/AuthContext'
import { eventsApi } from '../../services/eventsApi'
import { announcementsApi } from '../../services/announcementsApi'
import { opportunitiesApi } from '../../services/opportunitiesApi'
import { helpDeskApi } from '../../services/helpDeskApi'
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { Skeleton } from '../../components/ui/Skeleton'
import { formatDate } from '../../lib/utils'
import { toast } from 'sonner'
import {
  Calendar,
  Bell,
  Briefcase,
  HelpCircle,
  Users,
  Compass,
  ArrowRight,
  Pin,
  CheckCircle2,
  Clock,
  Sparkles,
} from 'lucide-react'

export function Dashboard() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  // Queries
  const { data: myEventsData, isLoading: loadingEvents } = useQuery({
    queryKey: ['my-events'],
    queryFn: () => eventsApi.getMyEvents({ page: 0, size: 4 }),
  })

  const { data: upcomingEventsData } = useQuery({
    queryKey: ['events', 'UPCOMING'],
    queryFn: () => eventsApi.getEvents({ status: 'UPCOMING', page: 0, size: 3 }),
  })

  const { data: unreadAnnouncementsData, isLoading: loadingAnnouncements } = useQuery({
    queryKey: ['unread-announcements'],
    queryFn: () => announcementsApi.getUnreadAnnouncements({ page: 0, size: 5 }),
  })

  const { data: opportunitiesData, isLoading: loadingOpportunities } = useQuery({
    queryKey: ['opportunities-recent'],
    queryFn: () => opportunitiesApi.getOpportunities({ page: 0, size: 3 }),
  })

  const { data: myIssuesData, isLoading: loadingIssues } = useQuery({
    queryKey: ['my-issues'],
    queryFn: () => helpDeskApi.getMyIssues({ page: 0, size: 3 }),
  })

  // Mark announcement as read mutation
  const markReadMutation = useMutation({
    mutationFn: (id) => announcementsApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unread-announcements'] })
      queryClient.invalidateQueries({ queryKey: ['announcements'] })
      toast.success('Marked as read')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to mark as read')
    },
  })

  const myEvents = myEventsData?.content || []
  const upcomingEvents = upcomingEventsData?.content || []
  const unreadAnnouncements = unreadAnnouncementsData?.content || []
  const opportunities = opportunitiesData?.content || []
  const myIssues = myIssuesData?.content || []

  const activeIssuesCount = myIssues.filter(
    (issue) => issue.status === 'SUBMITTED' || issue.status === 'IN_PROGRESS'
  ).length

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Welcome Hero */}
      <div className="relative overflow-hidden rounded-2xl border border-border/60 bg-linear-to-r from-card to-muted/40 p-6 md:p-8">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-semibold text-primary">
                <Sparkles className="h-3 w-3" /> CampusOS Hub
              </span>
              {user?.department && (
                <span className="text-xs text-muted-foreground font-medium">
                  {user.department} {user?.year ? `· Year ${user.year}` : ''}
                </span>
              )}
            </div>
            <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-foreground">
              Welcome back, {user?.name || 'Student'}!
            </h1>
            <p className="text-sm text-muted-foreground max-w-xl">
              Stay ahead with your campus activities, registered events, announcements, and student resources all in one place.
            </p>
          </div>

          {/* Quick Action Buttons */}
          <div className="flex flex-wrap gap-2.5">
            <Link to="/events">
              <Button size="sm" variant="default" className="shadow-xs">
                <Calendar className="mr-1.5 h-4 w-4" /> Browse Events
              </Button>
            </Link>
            <Link to="/clubs">
              <Button size="sm" variant="outline">
                <Users className="mr-1.5 h-4 w-4" /> Clubs
              </Button>
            </Link>
            <Link to="/resources">
              <Button size="sm" variant="outline">
                <Compass className="mr-1.5 h-4 w-4" /> Resource Hub
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Stats Ribbon */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card className="p-4 flex flex-col justify-between hover:border-primary/40 transition-colors">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-xs font-medium uppercase tracking-wider">Registered Events</span>
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Calendar className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-foreground">
              {myEventsData?.totalElements ?? myEvents.length}
            </span>
            <Link to="/events" className="mt-1 block text-xs text-muted-foreground hover:text-primary transition-colors">
              View your registrations →
            </Link>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between hover:border-primary/40 transition-colors">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-xs font-medium uppercase tracking-wider">Unread Notices</span>
            <div className="p-2 rounded-lg bg-amber-500/10 text-amber-500">
              <Bell className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-foreground">
              {unreadAnnouncementsData?.totalElements ?? unreadAnnouncements.length}
            </span>
            <Link to="/announcements" className="mt-1 block text-xs text-muted-foreground hover:text-primary transition-colors">
              Catch up on feed →
            </Link>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between hover:border-primary/40 transition-colors">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-xs font-medium uppercase tracking-wider">Active Tickets</span>
            <div className="p-2 rounded-lg bg-blue-500/10 text-blue-500">
              <HelpCircle className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-foreground">
              {activeIssuesCount}
            </span>
            <Link to="/help-desk" className="mt-1 block text-xs text-muted-foreground hover:text-primary transition-colors">
              Help Desk status →
            </Link>
          </div>
        </Card>

        <Card className="p-4 flex flex-col justify-between hover:border-primary/40 transition-colors">
          <div className="flex items-center justify-between text-muted-foreground">
            <span className="text-xs font-medium uppercase tracking-wider">Opportunities</span>
            <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-500">
              <Briefcase className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-foreground">
              {opportunitiesData?.totalElements ?? opportunities.length}
            </span>
            <Link to="/opportunities" className="mt-1 block text-xs text-muted-foreground hover:text-primary transition-colors">
              Explore listings →
            </Link>
          </div>
        </Card>
      </div>

      {/* Main Grid: Events & Announcements */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Registered or Upcoming Events */}
        <Card className="flex flex-col">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold">Your Registered Events</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">Sessions and activities you've signed up for</p>
            </div>
            <Link to="/events" className="text-xs text-primary font-medium hover:underline inline-flex items-center gap-1">
              Browse all <ArrowRight className="h-3 w-3" />
            </Link>
          </CardHeader>
          <CardContent className="flex-1 space-y-3">
            {loadingEvents ? (
              <div className="space-y-3">
                <Skeleton className="h-16 w-full rounded-lg" />
                <Skeleton className="h-16 w-full rounded-lg" />
              </div>
            ) : myEvents.length > 0 ? (
              myEvents.map((reg) => (
                <div
                  key={reg.id}
                  className="flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/20 hover:bg-muted/40 transition-colors"
                >
                  <div className="space-y-1 pr-2">
                    <p className="text-sm font-semibold text-foreground line-clamp-1">
                      {reg.eventTitle || `Event #${reg.eventId}`}
                    </p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <Clock className="h-3 w-3" />
                      <span>Registered {formatDate(reg.registeredAt)}</span>
                    </div>
                  </div>
                  <Badge
                    variant={
                      reg.status === 'CONFIRMED'
                        ? 'default'
                        : reg.status === 'WAITLISTED'
                        ? 'warning'
                        : 'outline-solid'
                    }
                  >
                    {reg.status}
                  </Badge>
                </div>
              ))
            ) : upcomingEvents.length > 0 ? (
              <div className="space-y-3">
                <p className="text-xs text-muted-foreground italic">
                  You haven't registered for any events yet. Here are upcoming events on campus:
                </p>
                {upcomingEvents.map((evt) => (
                  <div
                    key={evt.id}
                    className="flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/20 hover:bg-muted/40 transition-colors"
                  >
                    <div className="space-y-1">
                      <p className="text-sm font-semibold text-foreground line-clamp-1">{evt.title}</p>
                      <p className="text-xs text-muted-foreground">
                        {formatDate(evt.date)} · {evt.location}
                      </p>
                    </div>
                    <Link to="/events">
                      <Button size="xs" variant="outline">
                        Register
                      </Button>
                    </Link>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 text-muted-foreground text-xs">
                No upcoming events found. Check back later!
              </div>
            )}
          </CardContent>
        </Card>

        {/* Right Column: Campus Announcements */}
        <Card className="flex flex-col">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold">Campus Announcements</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">Important notices from departments and clubs</p>
            </div>
            <Link to="/announcements" className="text-xs text-primary font-medium hover:underline inline-flex items-center gap-1">
              View all <ArrowRight className="h-3 w-3" />
            </Link>
          </CardHeader>
          <CardContent className="flex-1 space-y-3">
            {loadingAnnouncements ? (
              <div className="space-y-3">
                <Skeleton className="h-16 w-full rounded-lg" />
                <Skeleton className="h-16 w-full rounded-lg" />
              </div>
            ) : unreadAnnouncements.length > 0 ? (
              unreadAnnouncements.map((ann) => (
                <div
                  key={ann.id}
                  className={`p-3.5 rounded-lg border transition-colors ${
                    ann.isPinned
                      ? 'border-amber-500/40 bg-amber-500/5 dark:bg-amber-500/10'
                      : 'border-border/60 bg-muted/20 hover:bg-muted/40'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="space-y-1">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {ann.isPinned && (
                          <Badge variant="warning" className="gap-1 text-[10px] py-0 px-1.5">
                            <Pin className="h-2.5 w-2.5" /> Pinned
                          </Badge>
                        )}
                        <Badge variant="secondary" className="text-[10px] py-0 px-1.5">
                          {ann.category}
                        </Badge>
                        <span className="text-[11px] text-muted-foreground">
                          {formatDate(ann.publishedAt || ann.createdAt)}
                        </span>
                      </div>
                      <h4 className="text-sm font-semibold text-foreground line-clamp-1">{ann.title}</h4>
                      <p className="text-xs text-muted-foreground line-clamp-2">{ann.content}</p>
                    </div>
                    <Button
                      size="xs"
                      variant="ghost"
                      className="text-muted-foreground hover:text-foreground shrink-0"
                      onClick={() => markReadMutation.mutate(ann.id)}
                      title="Mark as read"
                    >
                      <CheckCircle2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-muted-foreground text-xs">
                You're all caught up! No unread announcements.
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Secondary Grid: Opportunities & Help Desk / Lost & Found */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Latest Opportunities */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold">Latest Opportunities</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">Internships, hackathons, and research gigs</p>
            </div>
            <Link to="/opportunities" className="text-xs text-primary font-medium hover:underline inline-flex items-center gap-1">
              Explore <ArrowRight className="h-3 w-3" />
            </Link>
          </CardHeader>
          <CardContent className="space-y-3">
            {loadingOpportunities ? (
              <Skeleton className="h-24 w-full rounded-lg" />
            ) : opportunities.length > 0 ? (
              opportunities.map((opp) => (
                <div
                  key={opp.id}
                  className="flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/20 hover:bg-muted/40 transition-colors"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-[10px]">
                        {opp.type}
                      </Badge>
                      {opp.isRemote && (
                        <span className="text-[10px] font-medium text-emerald-600 dark:text-emerald-400">
                          Remote
                        </span>
                      )}
                    </div>
                    <p className="text-sm font-semibold text-foreground">{opp.title}</p>
                    <p className="text-xs text-muted-foreground">{opp.organization}</p>
                  </div>
                  {opp.deadline && (
                    <div className="text-right">
                      <span className="text-[11px] text-muted-foreground block">Deadline</span>
                      <span className="text-xs font-medium text-foreground">{formatDate(opp.deadline)}</span>
                    </div>
                  )}
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-muted-foreground text-xs">
                No opportunities listed right now.
              </div>
            )}
          </CardContent>
        </Card>

        {/* My Tickets & Lost/Found quick snapshot */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <div>
              <CardTitle className="text-base font-semibold">Campus Help Desk Tickets</CardTitle>
              <p className="text-xs text-muted-foreground mt-0.5">Track your open maintenance and campus reports</p>
            </div>
            <Link to="/help-desk" className="text-xs text-primary font-medium hover:underline inline-flex items-center gap-1">
              New Ticket <ArrowRight className="h-3 w-3" />
            </Link>
          </CardHeader>
          <CardContent className="space-y-3">
            {loadingIssues ? (
              <Skeleton className="h-24 w-full rounded-lg" />
            ) : myIssues.length > 0 ? (
              myIssues.map((issue) => (
                <div
                  key={issue.id}
                  className="flex items-center justify-between p-3.5 rounded-lg border border-border/60 bg-muted/20 hover:bg-muted/40 transition-colors"
                >
                  <div className="space-y-1">
                    <p className="text-sm font-semibold text-foreground line-clamp-1">{issue.title}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <span>{issue.category}</span>
                      <span>•</span>
                      <span>{issue.location}</span>
                    </div>
                  </div>
                  <Badge
                    variant={
                      issue.status === 'RESOLVED' || issue.status === 'CLOSED'
                        ? 'success'
                        : issue.status === 'IN_PROGRESS'
                        ? 'warning'
                        : 'secondary'
                    }
                  >
                    {issue.status}
                  </Badge>
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-muted-foreground text-xs space-y-2">
                <p>You have no active help desk tickets.</p>
                <Link to="/help-desk">
                  <Button size="xs" variant="outline">
                    Report an issue
                  </Button>
                </Link>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default Dashboard
