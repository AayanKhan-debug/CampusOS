import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { opportunitiesApi } from '../../services/opportunitiesApi'
import { useAuth } from '../../context/AuthContext'
import { PageHeader } from '../../components/common/PageHeader'
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '../../components/ui/Card'
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
import { formatDate } from '../../lib/utils'
import { toast } from 'sonner'
import {
  Briefcase,
  Search,
  Plus,
  ExternalLink,
  MapPin,
  Building,
  Clock,
  Trash2,
  Globe,
} from 'lucide-react'

const OPPORTUNITY_TYPES = [
  'ALL',
  'INTERNSHIP',
  'HACKATHON',
  'COMPETITION',
  'SCHOLARSHIP',
  'WORKSHOP',
  'CODING_CONTEST',
  'PLACEMENT',
  'OTHER',
]

const opportunitySchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters').max(200),
  organization: z.string().min(2, 'Organization is required').max(200),
  opportunityType: z.enum([
    'INTERNSHIP',
    'HACKATHON',
    'COMPETITION',
    'SCHOLARSHIP',
    'WORKSHOP',
    'CODING_CONTEST',
    'PLACEMENT',
    'OTHER',
  ]),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  eligibility: z.string().optional(),
  applicationUrl: z.string().url('Please enter a valid URL').optional().or(z.literal('')),
  location: z.string().optional(),
  deadline: z.string().optional(),
  remote: z.boolean().default(false),
})

export function Opportunities() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [selectedType, setSelectedType] = useState('ALL')
  const [remoteOnly, setRemoteOnly] = useState(false)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  // Dialog state
  const [isPostOpen, setIsPostOpen] = useState(false)

  const isOrganizer = user?.role === 'ADMIN' || user?.role === 'CLUB_ADMIN'

  // Query Opportunities
  const { data: oppsData, isLoading } = useQuery({
    queryKey: ['opportunities', selectedType, remoteOnly, search, page],
    queryFn: () => {
      const params = { page, size: 9, status: 'OPEN' }
      if (selectedType !== 'ALL') params.opportunityType = selectedType
      if (remoteOnly) params.remote = true
      if (search.trim()) params.search = search.trim()
      return opportunitiesApi.getOpportunities(params)
    },
  })

  // Form
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(opportunitySchema),
    defaultValues: {
      title: '',
      organization: '',
      opportunityType: 'INTERNSHIP',
      description: '',
      eligibility: '',
      applicationUrl: '',
      location: '',
      deadline: '',
      remote: false,
    },
  })

  // Mutations
  const createMutation = useMutation({
    mutationFn: (data) => opportunitiesApi.createOpportunity(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['opportunities'] })
      setIsPostOpen(false)
      reset()
      toast.success('Opportunity posted successfully!')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to post opportunity')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id) => opportunitiesApi.deleteOpportunity(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['opportunities'] })
      toast.success('Opportunity deleted')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to delete opportunity')
    },
  })

  const onSubmitPost = (data) => {
    const payload = {
      ...data,
      applicationUrl: data.applicationUrl ? data.applicationUrl.trim() : null,
      eligibility: data.eligibility ? data.eligibility.trim() : null,
      location: data.location ? data.location.trim() : null,
      deadline: data.deadline ? new Date(data.deadline).toISOString() : null,
    }
    createMutation.mutate(payload)
  }

  const opportunities = oppsData?.content || []

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Student Opportunities"
        description="Internships, hackathons, scholarships, and campus placement drives"
      >
        {isOrganizer && (
          <Button onClick={() => setIsPostOpen(true)} className="gap-1.5 shadow-xs">
            <Plus className="h-4 w-4" /> Post Opportunity
          </Button>
        )}
      </PageHeader>

      {/* Filter and Search */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-card p-3.5 rounded-xl border border-border/70 shadow-xs">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search roles, skills, or company name..."
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
            value={selectedType}
            onChange={(e) => {
              setSelectedType(e.target.value)
              setPage(0)
            }}
          >
            {OPPORTUNITY_TYPES.map((t) => (
              <option key={t} value={t}>
                {t === 'ALL' ? 'All Opportunity Types' : t.replace('_', ' ')}
              </option>
            ))}
          </Select>
        </div>

        <div className="flex items-center px-3 bg-muted/30 rounded-lg border border-border/50">
          <label className="flex items-center gap-2 cursor-pointer text-xs font-semibold text-foreground select-none">
            <input
              type="checkbox"
              checked={remoteOnly}
              onChange={(e) => {
                setRemoteOnly(e.target.checked)
                setPage(0)
              }}
              className="rounded border-border text-primary focus:ring-primary h-4 w-4"
            />
            <span>Remote Only</span>
          </label>
        </div>
      </div>

      {/* Opportunities Grid */}
      <div>
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <Card key={i} className="p-4 space-y-3">
                <Skeleton className="h-6 w-1/3" />
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-6 w-1/2" />
              </Card>
            ))}
          </div>
        ) : opportunities.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {opportunities.map((opp) => {
              const isOwner =
                user &&
                (opp.createdBy?.id === user.id || opp.createdBy?.email === user.email)
              const canManage = isOwner || user?.role === 'ADMIN'

              return (
                <Card
                  key={opp.id}
                  className="flex flex-col justify-between overflow-hidden hover:border-primary/40 transition-all hover:shadow-md"
                >
                  <div>
                    <CardHeader className="p-4 pb-2">
                      <div className="flex items-center justify-between gap-2 mb-2">
                        <Badge variant="outline" className="text-[10px] font-semibold">
                          {opp.opportunityType?.replace('_', ' ')}
                        </Badge>
                        {opp.remote && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                            <Globe className="h-3 w-3" /> Remote
                          </span>
                        )}
                      </div>

                      <CardTitle className="text-base font-bold line-clamp-1">
                        {opp.title}
                      </CardTitle>
                      <p className="text-xs text-muted-foreground flex items-center gap-1.5 mt-0.5 font-medium">
                        <Building className="h-3.5 w-3.5 text-primary" />
                        <span>{opp.organization}</span>
                      </p>
                    </CardHeader>

                    <CardContent className="p-4 pt-1 space-y-3">
                      <p className="text-xs text-muted-foreground line-clamp-3 leading-relaxed">
                        {opp.description}
                      </p>

                      {opp.eligibility && (
                        <div className="p-2.5 rounded-lg bg-muted/30 text-xs space-y-1">
                          <span className="text-[10px] uppercase font-bold text-muted-foreground block">
                            Eligibility
                          </span>
                          <p className="text-foreground line-clamp-2">{opp.eligibility}</p>
                        </div>
                      )}

                      <div className="space-y-1 text-xs text-muted-foreground pt-1 border-t border-border/50">
                        {opp.location && (
                          <div className="flex items-center gap-2">
                            <MapPin className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                            <span className="truncate">{opp.location}</span>
                          </div>
                        )}
                        {opp.deadline && (
                          <div className="flex items-center gap-2">
                            <Clock className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                            <span>Deadline: {formatDate(opp.deadline)}</span>
                          </div>
                        )}
                      </div>
                    </CardContent>
                  </div>

                  <CardFooter className="p-4 pt-0 border-t border-border/40 mt-3 flex items-center justify-between gap-2">
                    {opp.applicationUrl ? (
                      <a
                        href={opp.applicationUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex-1"
                      >
                        <Button size="sm" className="w-full text-xs gap-1.5">
                          <ExternalLink className="h-3.5 w-3.5" /> Apply Now
                        </Button>
                      </a>
                    ) : (
                      <Button size="sm" variant="outline" disabled className="flex-1 text-xs">
                        Contact Organizer
                      </Button>
                    )}

                    {canManage && (
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-destructive hover:bg-destructive/10"
                        onClick={() => {
                          if (window.confirm('Delete this opportunity?')) {
                            deleteMutation.mutate(opp.id)
                          }
                        }}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </CardFooter>
                </Card>
              )
            })}
          </div>
        ) : (
          <EmptyState
            icon={Briefcase}
            title="No opportunities found"
            description="No opportunities match your selected filter or keywords."
          />
        )}

        {/* Pagination */}
        {oppsData?.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <Button
              size="sm"
              variant="outline"
              disabled={oppsData.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {oppsData.pageNumber + 1} of {oppsData.totalPages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={oppsData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </div>

      {/* Post Modal */}
      <Dialog open={isPostOpen} onOpenChange={setIsPostOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Post Student Opportunity</DialogTitle>
            <DialogDescription>
              Share an internship, hackathon, or scholarship listing with the campus community.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit(onSubmitPost)} className="space-y-3.5 my-2">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Organization / Company *</label>
                <Input placeholder="e.g. Google, Campus DevClub" {...register('organization')} />
                {errors.organization && (
                  <p className="text-[11px] text-destructive">{errors.organization.message}</p>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Type *</label>
                <Select {...register('opportunityType')}>
                  <option value="INTERNSHIP">Internship</option>
                  <option value="HACKATHON">Hackathon</option>
                  <option value="COMPETITION">Competition</option>
                  <option value="SCHOLARSHIP">Scholarship</option>
                  <option value="WORKSHOP">Workshop</option>
                  <option value="CODING_CONTEST">Coding Contest</option>
                  <option value="PLACEMENT">Placement</option>
                  <option value="OTHER">Other</option>
                </Select>
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Title *</label>
              <Input
                placeholder="e.g. Summer Software Engineering Intern"
                {...register('title')}
              />
              {errors.title && (
                <p className="text-[11px] text-destructive">{errors.title.message}</p>
              )}
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Description *</label>
              <Textarea
                placeholder="Key responsibilities, perks, duration, and overview..."
                rows={3}
                {...register('description')}
              />
              {errors.description && (
                <p className="text-[11px] text-destructive">{errors.description.message}</p>
              )}
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Eligibility (Optional)</label>
              <Input
                placeholder="e.g. Pre-final & Final year CS/IT students"
                {...register('eligibility')}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Application URL</label>
                <Input
                  type="url"
                  placeholder="https://company.com/apply"
                  {...register('applicationUrl')}
                />
                {errors.applicationUrl && (
                  <p className="text-[11px] text-destructive">{errors.applicationUrl.message}</p>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Deadline</label>
                <Input type="date" {...register('deadline')} />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 items-center">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Location</label>
                <Input placeholder="e.g. San Francisco or On Campus" {...register('location')} />
              </div>

              <div className="flex items-center gap-2 pt-4">
                <input
                  type="checkbox"
                  id="remoteCheckbox"
                  {...register('remote')}
                  className="rounded border-border text-primary focus:ring-primary h-4 w-4"
                />
                <label htmlFor="remoteCheckbox" className="text-xs font-semibold text-foreground cursor-pointer">
                  Remote Position
                </label>
              </div>
            </div>

            <DialogFooter className="pt-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsPostOpen(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Posting...' : 'Post Opportunity'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default Opportunities
