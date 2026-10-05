import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { resourcesApi } from '../../services/resourcesApi'
import { PageHeader } from '../../components/common/PageHeader'
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { Input } from '../../components/ui/Input'
import { Select } from '../../components/ui/Select'
import { Skeleton } from '../../components/ui/Skeleton'
import { EmptyState } from '../../components/common/EmptyState'
import { formatDate } from '../../lib/utils'
import {
  BookOpen,
  FileText,
  Search,
  ExternalLink,
  Video,
  FileQuestion,
  FlaskConical,
  BookMarked,
} from 'lucide-react'

const RESOURCE_TYPES = [
  'ALL',
  'NOTES',
  'QUESTION_PAPER',
  'ASSIGNMENT',
  'LAB',
  'VIDEO',
  'BOOK',
  'REFERENCE',
  'OTHER',
]

function getResourceIcon(type) {
  switch (type) {
    case 'NOTES':
      return <FileText className="h-5 w-5 text-blue-500" />
    case 'QUESTION_PAPER':
      return <FileQuestion className="h-5 w-5 text-amber-500" />
    case 'ASSIGNMENT':
      return <FileText className="h-5 w-5 text-purple-500" />
    case 'LAB':
      return <FlaskConical className="h-5 w-5 text-emerald-500" />
    case 'VIDEO':
      return <Video className="h-5 w-5 text-red-500" />
    case 'BOOK':
    case 'REFERENCE':
      return <BookMarked className="h-5 w-5 text-indigo-500" />
    default:
      return <BookOpen className="h-5 w-5 text-primary" />
  }
}

export function Resources() {
  const [selectedDepartment, setSelectedDepartment] = useState('')
  const [selectedCourse, setSelectedCourse] = useState('')
  const [selectedSubject, setSelectedSubject] = useState('')
  const [resourceType, setResourceType] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)

  // 1. Fetch Departments
  const { data: departmentsData } = useQuery({
    queryKey: ['departments'],
    queryFn: () => resourcesApi.getDepartments({ page: 0, size: 50 }),
  })

  // 2. Fetch Courses based on Department
  const { data: coursesData } = useQuery({
    queryKey: ['courses', selectedDepartment],
    queryFn: () => {
      const params = { page: 0, size: 50 }
      if (selectedDepartment) params.departmentId = selectedDepartment
      return resourcesApi.getCourses(params)
    },
  })

  // 3. Fetch Subjects based on Course
  const { data: subjectsData } = useQuery({
    queryKey: ['subjects', selectedCourse],
    queryFn: () => {
      const params = { page: 0, size: 50 }
      if (selectedCourse) params.courseId = selectedCourse
      return resourcesApi.getSubjects(params)
    },
  })

  // 4. Fetch Resources
  const { data: resourcesData, isLoading: loadingResources } = useQuery({
    queryKey: [
      'resources',
      selectedDepartment,
      selectedCourse,
      selectedSubject,
      resourceType,
      search,
      page,
    ],
    queryFn: () => {
      const params = { page, size: 12 }
      if (selectedSubject) params.subjectId = selectedSubject
      if (resourceType !== 'ALL') params.resourceType = resourceType
      if (search.trim()) params.search = search.trim()
      return resourcesApi.getResources(params)
    },
  })

  const departments = departmentsData?.content || []
  const courses = coursesData?.content || []
  const subjects = subjectsData?.content || []
  const resources = resourcesData?.content || []

  const resetFilters = () => {
    setSelectedDepartment('')
    setSelectedCourse('')
    setSelectedSubject('')
    setResourceType('ALL')
    setSearch('')
    setPage(0)
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <PageHeader
        title="Student Resource Hub"
        description="Course materials, past question papers, lab manuals, and lecture notes"
      />

      {/* Drill-down and Filter Panel */}
      <div className="bg-card p-4 rounded-xl border border-border/70 shadow-xs space-y-3">
        {/* Search & Resource Type */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div className="relative md:col-span-2">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search resources by title or keywords..."
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
              value={resourceType}
              onChange={(e) => {
                setResourceType(e.target.value)
                setPage(0)
              }}
            >
              {RESOURCE_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t === 'ALL' ? 'All Resource Types' : t.replace('_', ' ')}
                </option>
              ))}
            </Select>
          </div>
        </div>

        {/* Academic Hierarchy Dropdowns */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-border/50">
          <div>
            <label className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider block mb-1">
              Department
            </label>
            <Select
              value={selectedDepartment}
              onChange={(e) => {
                setSelectedDepartment(e.target.value)
                setSelectedCourse('')
                setSelectedSubject('')
                setPage(0)
              }}
            >
              <option value="">All Departments</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.code})
                </option>
              ))}
            </Select>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider block mb-1">
              Course / Degree
            </label>
            <Select
              value={selectedCourse}
              onChange={(e) => {
                setSelectedCourse(e.target.value)
                setSelectedSubject('')
                setPage(0)
              }}
            >
              <option value="">All Courses</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
            </Select>
          </div>

          <div>
            <label className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider block mb-1">
              Subject
            </label>
            <Select
              value={selectedSubject}
              onChange={(e) => {
                setSelectedSubject(e.target.value)
                setPage(0)
              }}
            >
              <option value="">All Subjects</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.code})
                </option>
              ))}
            </Select>
          </div>
        </div>
      </div>

      {/* Resources Grid */}
      <div>
        {loadingResources ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[...Array(6)].map((_, i) => (
              <Card key={i} className="p-4 space-y-3">
                <Skeleton className="h-5 w-2/3" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-8 w-1/3" />
              </Card>
            ))}
          </div>
        ) : resources.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {resources.map((res) => (
              <Card
                key={res.id}
                className="flex flex-col justify-between hover:border-primary/40 transition-all hover:shadow-xs"
              >
                <CardHeader className="p-4 pb-2">
                  <div className="flex items-start justify-between gap-2">
                    <div className="p-2 rounded-lg bg-muted/50 shrink-0">
                      {getResourceIcon(res.resourceType)}
                    </div>
                    <Badge variant="outline" className="text-[10px]">
                      {res.resourceType?.replace('_', ' ') || 'RESOURCE'}
                    </Badge>
                  </div>
                  <CardTitle className="text-sm font-bold line-clamp-2 mt-2">
                    {res.title}
                  </CardTitle>
                </CardHeader>

                <CardContent className="p-4 pt-1 space-y-2.5">
                  {res.description && (
                    <p className="text-xs text-muted-foreground line-clamp-2">
                      {res.description}
                    </p>
                  )}

                  <div className="space-y-1 text-[11px] text-muted-foreground pt-1 border-t border-border/40">
                    {res.subjectName && (
                      <p className="font-medium text-foreground truncate">
                        {res.subjectName}
                      </p>
                    )}
                    <div className="flex items-center justify-between text-muted-foreground">
                      <span>{res.courseName || res.departmentName || 'Academic'}</span>
                      <span>{formatDate(res.createdAt)}</span>
                    </div>
                  </div>
                </CardContent>

                <div className="p-4 pt-0">
                  {res.url ? (
                    <a
                      href={res.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full inline-block"
                    >
                      <Button size="sm" variant="outline" className="w-full text-xs gap-1.5">
                        <ExternalLink className="h-3.5 w-3.5" />
                        Access Resource
                      </Button>
                    </a>
                  ) : (
                    <Button size="sm" variant="outline" disabled className="w-full text-xs">
                      No link attached
                    </Button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={BookOpen}
            title="No academic resources found"
            description="No resources match the selected department, subject, or search keyword."
            action={
              <Button variant="outline" onClick={resetFilters}>
                Clear All Filters
              </Button>
            }
          />
        )}

        {/* Pagination */}
        {resourcesData?.totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 pt-6">
            <Button
              size="sm"
              variant="outline"
              disabled={resourcesData.first}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-muted-foreground">
              Page {resourcesData.pageNumber + 1} of {resourcesData.totalPages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={resourcesData.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}

export default Resources
