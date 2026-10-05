import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { lostFoundApi } from '../../services/lostFoundApi'
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
  Search,
  Plus,
  MapPin,
  Calendar,
  Phone,
  Tag,
  Trash2,
  HelpCircle,
} from 'lucide-react'

const reportSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters').max(200),
  listingType: z.enum(['LOST', 'FOUND']),
  itemType: z.string().min(2, 'Item type required (e.g. Keys, Laptop, Wallet)'),
  location: z.string().min(2, 'Location is required'),
  dateOccurred: z.string().min(1, 'Date is required'),
  description: z.string().max(5000).optional(),
  contactInfo: z.string().max(200).optional(),
  imageUrl: z.string().optional(),
})

const STATUS_OPTIONS = ['ALL', 'OPEN', 'MATCHED', 'RECOVERED', 'CLOSED']

export function LostFound() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [listingTypeFilter, setListingTypeFilter] = useState('ALL') // 'ALL' | 'LOST' | 'FOUND'
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  // Dialogs
  const [isReportOpen, setIsReportOpen] = useState(false)
  const [statusUpdateItem, setStatusUpdateItem] = useState(null)
  const [isStatusDialogOpen, setIsStatusDialogOpen] = useState(false)
  const [newStatus, setNewStatus] = useState('OPEN')

  // Form for reporting item
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(reportSchema),
    defaultValues: {
      title: '',
      listingType: 'LOST',
      itemType: '',
      location: '',
      dateOccurred: new Date().toISOString().split('T')[0],
      description: '',
      contactInfo: user?.phone || user?.email || '',
      imageUrl: '',
    },
  })

  // Query: Lost & Found Items
  const { data: listingsData, isLoading } = useQuery({
    queryKey: ['lost-found', listingTypeFilter, statusFilter, search, page],
    queryFn: () => {
      const params = { page, size: 9 }
      if (listingTypeFilter !== 'ALL') params.listingType = listingTypeFilter
      if (statusFilter !== 'ALL') params.status = statusFilter
      if (search.trim()) params.search = search.trim()
      return lostFoundApi.getListings(params)
    },
  })

  // Create Mutation
  const createMutation = useMutation({
    mutationFn: (data) => lostFoundApi.createListing(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found'] })
      setIsReportOpen(false)
      reset()
      toast.success('Listing created successfully!')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to submit listing')
    },
  })

  // Status Update Mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }) => lostFoundApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found'] })
      setIsStatusDialogOpen(false)
      toast.success('Listing status updated')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to update status')
    },
  })

  // Delete Mutation
  const deleteMutation = useMutation({
    mutationFn: (id) => lostFoundApi.deleteListing(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lost-found'] })
      toast.success('Listing deleted')
    },
    onError: (err) => {
      toast.error(err.message || 'Failed to delete listing')
    },
  })

  const onSubmitReport = (data) => {
    createMutation.mutate({
      ...data,
      description: data.description?.trim() || null,
      contactInfo: data.contactInfo?.trim() || null,
      imageUrl: data.imageUrl?.trim() || null,
    })
  }

  const handleOpenStatusDialog = (item) => {
    setStatusUpdateItem(item)
    // determine default next logical status
    if (item.status === 'OPEN') setNewStatus('MATCHED')
    else if (item.status === 'MATCHED') setNewStatus('RECOVERED')
    else if (item.status === 'RECOVERED') setNewStatus('CLOSED')
    else setNewStatus('CLOSED')
    setIsStatusDialogOpen(true)
  }

  const listings = listingsData?.content || []

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Lost & Found"
        description="Report lost items or help fellow students recover misplaced belongings"
      >
        <Button onClick={() => setIsReportOpen(true)} className="gap-1.5 shadow-xs">
          <Plus className="h-4 w-4" /> Report Item
        </Button>
      </PageHeader>

      {/* Filter and Search Bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-card p-3.5 rounded-xl border border-border/70 shadow-xs">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search by title, location, or item name..."
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
            value={listingTypeFilter}
            onChange={(e) => {
              setListingTypeFilter(e.target.value)
              setPage(0)
            }}
          >
            <option value="ALL">All Types (Lost & Found)</option>
            <option value="LOST">Lost Items</option>
            <option value="FOUND">Found Items</option>
          </Select>
        </div>

        <div>
          <Select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(0)
            }}
          >
            {STATUS_OPTIONS.map((st) => (
              <option key={st} value={st}>
                {st === 'ALL' ? 'All Statuses' : st}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {/* Listings Grid */}
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
        ) : listings.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {listings.map((item) => {
              const isOwner =
                user &&
                (item.createdBy?.id === user.id || item.createdBy?.email === user.email)
              const isAdmin = user?.role === 'ADMIN'

              return (
                <Card
                  key={item.id}
                  className="flex flex-col justify-between overflow-hidden hover:border-primary/40 transition-all hover:shadow-md"
                >
                  <div>
                    {item.imageUrl && (
                      <div className="h-40 w-full overflow-hidden bg-muted">
                        <img
                          src={item.imageUrl}
                          alt={item.title}
                          className="h-full w-full object-cover"
                        />
                      </div>
                    )}

                    <CardHeader className="p-4 pb-2">
                      <div className="flex items-center justify-between gap-2 mb-2">
                        <Badge
                          variant={item.listingType === 'LOST' ? 'destructive' : 'success'}
                          className="text-[10px] font-bold tracking-wider"
                        >
                          {item.listingType}
                        </Badge>
                        <Badge
                          variant={
                            item.status === 'OPEN'
                              ? 'default'
                              : item.status === 'MATCHED'
                              ? 'warning'
                              : item.status === 'RECOVERED'
                              ? 'success'
                              : 'secondary'
                          }
                          className="text-[10px]"
                        >
                          {item.status}
                        </Badge>
                      </div>

                      <CardTitle className="text-base font-bold line-clamp-1">
                        {item.title}
                      </CardTitle>
                      <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                        <Tag className="h-3 w-3" />
                        <span>Category: {item.itemType}</span>
                      </p>
                    </CardHeader>

                    <CardContent className="p-4 pt-1 space-y-3">
                      {item.description && (
                        <p className="text-xs text-muted-foreground line-clamp-3">
                          {item.description}
                        </p>
                      )}

                      <div className="space-y-1.5 text-xs text-muted-foreground pt-2 border-t border-border/50">
                        <div className="flex items-center gap-2">
                          <MapPin className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                          <span className="truncate">{item.location}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Calendar className="h-3.5 w-3.5 text-foreground/70 shrink-0" />
                          <span>Date: {formatDate(item.dateOccurred)}</span>
                        </div>
                        {item.contactInfo && (
                          <div className="flex items-center gap-2 text-foreground font-medium">
                            <Phone className="h-3.5 w-3.5 text-primary shrink-0" />
                            <span className="truncate">{item.contactInfo}</span>
                          </div>
                        )}
                      </div>
                    </CardContent>
                  </div>

                  <CardFooter className="p-4 pt-0 border-t border-border/40 mt-3 flex items-center justify-between gap-2">
                    <span className="text-[11px] text-muted-foreground">
                      Reported by {item.createdBy?.name || 'Student'}
                    </span>

                    {(isOwner || isAdmin) && (
                      <div className="flex items-center gap-1.5">
                        {item.status !== 'CLOSED' && (
                          <Button
                            size="xs"
                            variant="outline"
                            onClick={() => handleOpenStatusDialog(item)}
                          >
                            Update Status
                          </Button>
                        )}
                        <Button
                          size="xs"
                          variant="ghost"
                          className="text-destructive hover:bg-destructive/10"
                          onClick={() => {
                            if (window.confirm('Delete this listing?')) {
                              deleteMutation.mutate(item.id)
                            }
                          }}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    )}
                  </CardFooter>
                </Card>
              )
            })}
          </div>
        ) : (
          <EmptyState
            icon={HelpCircle}
            title="No items found"
            description="No lost or found listings match your active filters."
            action={
              <Button onClick={() => setIsReportOpen(true)}>Report an item</Button>
            }
          />
        )}

        {/* Pagination */}
        {listingsData?.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <Button
              size="sm"
              variant="outline"
              disabled={listingsData.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {listingsData.pageNumber + 1} of {listingsData.totalPages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={listingsData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </div>

      {/* Report Modal */}
      <Dialog open={isReportOpen} onOpenChange={setIsReportOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Report Lost or Found Item</DialogTitle>
            <DialogDescription>
              Submit an item notice to assist campus recovery.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit(onSubmitReport)} className="space-y-3.5 my-2">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Notice Type *</label>
                <Select {...register('listingType')}>
                  <option value="LOST">Lost (I lost something)</option>
                  <option value="FOUND">Found (I found something)</option>
                </Select>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Item Category *</label>
                <Input
                  placeholder="e.g. Wallet, AirPods, ID"
                  {...register('itemType')}
                />
                {errors.itemType && (
                  <p className="text-[11px] text-destructive">{errors.itemType.message}</p>
                )}
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Title *</label>
              <Input
                placeholder="e.g. Blue Hydro Flask bottle left in Library 2nd floor"
                {...register('title')}
              />
              {errors.title && (
                <p className="text-[11px] text-destructive">{errors.title.message}</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Location *</label>
                <Input
                  placeholder="e.g. Science Complex Lab 3"
                  {...register('location')}
                />
                {errors.location && (
                  <p className="text-[11px] text-destructive">{errors.location.message}</p>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Date Occurred *</label>
                <Input type="date" {...register('dateOccurred')} />
                {errors.dateOccurred && (
                  <p className="text-[11px] text-destructive">{errors.dateOccurred.message}</p>
                )}
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold text-foreground">Description (Optional)</label>
              <Textarea
                placeholder="Provide distinctive details, scratches, brand, stickers..."
                rows={3}
                {...register('description')}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Contact Info</label>
                <Input
                  placeholder="Phone or email to reach you"
                  {...register('contactInfo')}
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-foreground">Image URL (Optional)</label>
                <Input
                  placeholder="https://..."
                  {...register('imageUrl')}
                />
              </div>
            </div>

            <DialogFooter className="pt-3">
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsReportOpen(false)}
                disabled={isSubmitting}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Submitting...' : 'Submit Notice'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Status Update Dialog */}
      <Dialog open={isStatusDialogOpen} onOpenChange={setIsStatusDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Update Listing Status</DialogTitle>
            <DialogDescription>
              Mark the status transition for "{statusUpdateItem?.title}"
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 my-2">
            <label className="text-xs font-semibold text-foreground">Select New Status</label>
            <Select value={newStatus} onChange={(e) => setNewStatus(e.target.value)}>
              {statusUpdateItem?.status === 'OPEN' && (
                <>
                  <option value="MATCHED">MATCHED (Potential owner found)</option>
                  <option value="CLOSED">CLOSED (Resolved or dismissed)</option>
                </>
              )}
              {statusUpdateItem?.status === 'MATCHED' && (
                <>
                  <option value="RECOVERED">RECOVERED (Item returned to owner)</option>
                  <option value="CLOSED">CLOSED (Resolved)</option>
                </>
              )}
              {statusUpdateItem?.status === 'RECOVERED' && (
                <option value="CLOSED">CLOSED (Resolved)</option>
              )}
            </Select>
          </div>

          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setIsStatusDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={() =>
                updateStatusMutation.mutate({
                  id: statusUpdateItem.id,
                  status: newStatus,
                })
              }
              disabled={updateStatusMutation.isPending}
            >
              {updateStatusMutation.isPending ? 'Updating...' : 'Save Status'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

export default LostFound
