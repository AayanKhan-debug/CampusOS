import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { clubsApi } from '../../services/clubsApi'
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
  Users,
  Search,
  UserPlus,
} from 'lucide-react'

const CLUB_CATEGORIES = [
  'ALL',
  'TECHNICAL',
  'CULTURAL',
  'SPORTS',
  'ACADEMIC',
  'ARTS',
  'SOCIAL',
  'OTHER',
]

export function Clubs() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('ALL')
  const [page, setPage] = useState(0)

  // Dialog state
  const [selectedClub, setSelectedClub] = useState(null)
  const [isJoinDialogOpen, setIsJoinDialogOpen] = useState(false)
  const [isMembersDialogOpen, setIsMembersDialogOpen] = useState(false)

  // Clubs query
  const { data: clubsData, isLoading: loadingClubs } = useQuery({
    queryKey: ['clubs', category, search, page],
    queryFn: () => {
      const params = { page, size: 9, status: 'ACTIVE' }
      if (category !== 'ALL') params.category = category
      if (search.trim()) params.search = search.trim()
      return clubsApi.getClubs(params)
    },
  })

  // Members query for selected club
  const { data: membersData, isLoading: loadingMembers } = useQuery({
    queryKey: ['club-members', selectedClub?.id],
    queryFn: () => clubsApi.getClubMembers(selectedClub.id),
    enabled: !!selectedClub && isMembersDialogOpen,
  })

  // Join club mutation
  const joinMutation = useMutation({
    mutationFn: (clubId) => clubsApi.joinClub(clubId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clubs'] })
      setIsJoinDialogOpen(false)
      toast.success(`Membership request submitted for ${selectedClub?.name}!`)
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to submit join request')
    },
  })

  const handleOpenJoin = (club) => {
    setSelectedClub(club)
    setIsJoinDialogOpen(true)
  }

  const handleOpenMembers = (club) => {
    setSelectedClub(club)
    setIsMembersDialogOpen(true)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Student Clubs & Chapters"
        description="Connect with student-led communities, hack clubs, and campus societies"
      />

      {/* Search and Category Filter */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3 bg-card p-3.5 rounded-xl border border-border/70 shadow-xs">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search clubs by name or keywords..."
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
            {CLUB_CATEGORIES.map((cat) => (
              <option key={cat} value={cat}>
                {cat === 'ALL' ? 'All Categories' : cat}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {/* Clubs Grid */}
      <div>
        {loadingClubs ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <Card key={i} className="p-4 space-y-4">
                <Skeleton className="h-6 w-1/2" />
                <Skeleton className="h-16 w-full" />
                <Skeleton className="h-8 w-full" />
              </Card>
            ))}
          </div>
        ) : clubsData?.content?.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {clubsData.content.map((club) => (
              <Card
                key={club.id}
                className="flex flex-col justify-between overflow-hidden hover:border-primary/40 transition-all hover:shadow-md"
              >
                <div>
                  <CardHeader className="p-4 pb-2">
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <Badge variant="outline" className="text-[10px] font-semibold">
                        {club.category || 'General'}
                      </Badge>
                      <Badge
                        variant={club.status === 'ACTIVE' ? 'success' : 'secondary'}
                        className="text-[10px]"
                      >
                        {club.status}
                      </Badge>
                    </div>

                    <div className="flex items-center gap-3">
                      {club.logo ? (
                        <img
                          src={club.logo}
                          alt={club.name}
                          className="h-10 w-10 rounded-lg object-cover border border-border"
                        />
                      ) : (
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary font-bold text-sm">
                          {club.name.substring(0, 2).toUpperCase()}
                        </div>
                      )}
                      <div>
                        <CardTitle className="text-base font-bold line-clamp-1">
                          {club.name}
                        </CardTitle>
                        <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                          <Users className="h-3 w-3" />
                          <span>{club.memberCount || 0} active members</span>
                        </p>
                      </div>
                    </div>
                  </CardHeader>

                  <CardContent className="p-4 pt-1 space-y-3">
                    <p className="text-xs text-muted-foreground line-clamp-3">
                      {club.description}
                    </p>

                    {club.createdBy && (
                      <div className="pt-2 border-t border-border/50 text-[11px] text-muted-foreground flex items-center justify-between">
                        <span>Lead: {club.createdBy.name}</span>
                        <span>Since {formatDate(club.createdAt)}</span>
                      </div>
                    )}
                  </CardContent>
                </div>

                <CardFooter className="p-4 pt-0 border-t border-border/40 mt-3 flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 text-xs"
                    onClick={() => handleOpenMembers(club)}
                  >
                    View Members
                  </Button>
                  <Button
                    size="sm"
                    className="flex-1 text-xs gap-1"
                    onClick={() => handleOpenJoin(club)}
                  >
                    <UserPlus className="h-3.5 w-3.5" />
                    Join Club
                  </Button>
                </CardFooter>
              </Card>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={Users}
            title="No clubs found"
            description="No student clubs match your filter criteria or search keyword."
          />
        )}

        {/* Pagination */}
        {clubsData?.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <Button
              size="sm"
              variant="outline"
              disabled={clubsData.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {clubsData.pageNumber + 1} of {clubsData.totalPages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={clubsData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </div>

      {/* Join Club Dialog */}
      <Dialog open={isJoinDialogOpen} onOpenChange={setIsJoinDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Join {selectedClub?.name}</DialogTitle>
            <DialogDescription>
              Submit a membership request to join this student organization. Club leaders will review your application.
            </DialogDescription>
          </DialogHeader>

          <div className="p-3 bg-muted/40 rounded-lg text-xs space-y-1 text-muted-foreground">
            <p className="font-semibold text-foreground">Club Category: {selectedClub?.category}</p>
            <p>{selectedClub?.description}</p>
          </div>

          <DialogFooter className="mt-4">
            <Button
              variant="outline"
              onClick={() => setIsJoinDialogOpen(false)}
              disabled={joinMutation.isPending}
            >
              Cancel
            </Button>
            <Button
              onClick={() => joinMutation.mutate(selectedClub?.id)}
              disabled={joinMutation.isPending}
            >
              {joinMutation.isPending ? 'Submitting...' : 'Confirm Request'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View Members Dialog */}
      <Dialog open={isMembersDialogOpen} onOpenChange={setIsMembersDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{selectedClub?.name} Members</DialogTitle>
            <DialogDescription>
              Active and approved members of this club
            </DialogDescription>
          </DialogHeader>

          <div className="max-h-72 overflow-y-auto space-y-2 pr-1">
            {loadingMembers ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : membersData?.content?.length > 0 ? (
              membersData.content.map((member) => (
                <div
                  key={member.id}
                  className="flex items-center justify-between p-2.5 rounded-lg border border-border/60 bg-muted/20 text-xs"
                >
                  <div>
                    <p className="font-semibold text-foreground">
                      {member.user?.name || member.userName}
                    </p>
                    <p className="text-[11px] text-muted-foreground">
                      {member.user?.department || 'Student'}
                    </p>
                  </div>
                  <Badge variant="outline" className="text-[10px]">
                    {member.status}
                  </Badge>
                </div>
              ))
            ) : (
              <p className="text-center py-6 text-xs text-muted-foreground">
                No approved members listed.
              </p>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsMembersDialogOpen(false)}>
              Close
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default Clubs
