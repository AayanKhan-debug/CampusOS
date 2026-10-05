import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useAuth } from '../../context/AuthContext'
import { PageHeader } from '../../components/common/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { Input } from '../../components/ui/Input'
import { Textarea } from '../../components/ui/Textarea'
import { Select } from '../../components/ui/Select'
import { formatDate } from '../../lib/utils'
import { toast } from 'sonner'
import {
  Mail,
  GraduationCap,
  BookOpen,
  Phone,
  Code,
  Heart,
  Save,
  Shield,
  Calendar,
} from 'lucide-react'

const profileSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters').max(100),
  department: z.string().optional(),
  year: z.string().optional(),
  rollNumber: z.string().optional(),
  phone: z.string().optional(),
  bio: z.string().max(1000).optional(),
  skills: z.string().optional(),
  interests: z.string().optional(),
  profilePicture: z.string().optional(),
})

export function Profile() {
  const { user, updateProfile } = useAuth()
  const [isEditing, setIsEditing] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: user?.name || '',
      department: user?.department || '',
      year: user?.year ? String(user.year) : '',
      rollNumber: user?.rollNumber || '',
      phone: user?.phone || '',
      bio: user?.bio || '',
      skills: user?.skills || '',
      interests: user?.interests || '',
      profilePicture: user?.profilePicture || '',
    },
  })

  const onSubmit = async (data) => {
    try {
      const payload = {
        name: data.name.trim(),
        department: data.department ? data.department.trim() : null,
        year: data.year ? parseInt(data.year, 10) : null,
        rollNumber: data.rollNumber ? data.rollNumber.trim() : null,
        phone: data.phone ? data.phone.trim() : null,
        bio: data.bio ? data.bio.trim() : null,
        skills: data.skills ? data.skills.trim() : null,
        interests: data.interests ? data.interests.trim() : null,
        profilePicture: data.profilePicture ? data.profilePicture.trim() : null,
      }
      await updateProfile(payload)
      toast.success('Profile updated successfully!')
      setIsEditing(false)
    } catch (err) {
      toast.error(err.message || 'Failed to update profile')
    }
  }

  const cancelEdit = () => {
    reset({
      name: user?.name || '',
      department: user?.department || '',
      year: user?.year ? String(user.year) : '',
      rollNumber: user?.rollNumber || '',
      phone: user?.phone || '',
      bio: user?.bio || '',
      skills: user?.skills || '',
      interests: user?.interests || '',
      profilePicture: user?.profilePicture || '',
    })
    setIsEditing(false)
  }

  const skillsList = user?.skills
    ? user.skills.split(',').map((s) => s.trim()).filter(Boolean)
    : []

  const interestsList = user?.interests
    ? user.interests.split(',').map((s) => s.trim()).filter(Boolean)
    : []

  return (
    <div className="space-y-6 animate-fade-in max-w-4xl mx-auto">
      <PageHeader
        title="Student Profile"
        description="Manage your campus identity, academic details, and portfolio skills"
      >
        {!isEditing ? (
          <Button onClick={() => setIsEditing(true)}>Edit Profile</Button>
        ) : (
          <Button variant="outline" onClick={cancelEdit}>
            Cancel
          </Button>
        )}
      </PageHeader>

      {/* Profile Overview Card */}
      <Card className="overflow-hidden border-border/70 shadow-xs">
        <div className="h-28 bg-linear-to-r from-primary/20 via-primary/10 to-muted/40 border-b border-border/40" />

        <div className="px-6 pb-6 pt-0 relative">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 -mt-12 mb-4">
            <div className="flex items-end gap-4">
              {user?.profilePicture ? (
                <img
                  src={user.profilePicture}
                  alt={user.name}
                  className="h-20 w-20 rounded-2xl object-cover border-4 border-background shadow-md bg-muted"
                />
              ) : (
                <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-primary text-primary-foreground font-black text-2xl border-4 border-background shadow-md">
                  {user?.name?.substring(0, 2).toUpperCase() || 'ST'}
                </div>
              )}
              <div className="space-y-1 mb-1">
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold text-foreground">{user?.name}</h2>
                  <Badge variant="default" className="text-[10px]">
                    {user?.role}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground flex items-center gap-1.5">
                  <Mail className="h-3.5 w-3.5" /> {user?.email}
                </p>
              </div>
            </div>

            <div className="text-xs text-muted-foreground flex items-center gap-1.5">
              <Calendar className="h-3.5 w-3.5" />
              <span>Joined {formatDate(user?.createdAt)}</span>
            </div>
          </div>

          {!isEditing ? (
            /* VIEW MODE */
            <div className="space-y-6 pt-4 border-t border-border/50">
              {/* Bio */}
              <div>
                <h4 className="text-xs font-semibold uppercase text-muted-foreground tracking-wider mb-1.5">
                  About Me
                </h4>
                <p className="text-sm text-foreground leading-relaxed">
                  {user?.bio || (
                    <span className="italic text-muted-foreground">
                      No bio added yet. Tell other students and club leaders about yourself.
                    </span>
                  )}
                </p>
              </div>

              {/* Academic Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-4 rounded-xl bg-muted/20 border border-border/50">
                <div className="space-y-1">
                  <span className="text-[11px] font-semibold text-muted-foreground flex items-center gap-1">
                    <GraduationCap className="h-3.5 w-3.5 text-primary" /> Department
                  </span>
                  <p className="text-sm font-semibold text-foreground">
                    {user?.department || 'Not specified'}
                  </p>
                </div>

                <div className="space-y-1">
                  <span className="text-[11px] font-semibold text-muted-foreground flex items-center gap-1">
                    <BookOpen className="h-3.5 w-3.5 text-primary" /> Academic Year
                  </span>
                  <p className="text-sm font-semibold text-foreground">
                    {user?.year ? `Year ${user.year}` : 'Not specified'}
                  </p>
                </div>

                <div className="space-y-1">
                  <span className="text-[11px] font-semibold text-muted-foreground flex items-center gap-1">
                    <Shield className="h-3.5 w-3.5 text-primary" /> Roll Number / ID
                  </span>
                  <p className="text-sm font-semibold text-foreground">
                    {user?.rollNumber || 'Not specified'}
                  </p>
                </div>
              </div>

              {/* Skills and Interests */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <h4 className="text-xs font-semibold uppercase text-muted-foreground tracking-wider mb-2 flex items-center gap-1.5">
                    <Code className="h-3.5 w-3.5 text-primary" /> Skills
                  </h4>
                  {skillsList.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {skillsList.map((skill, i) => (
                        <Badge key={i} variant="secondary" className="text-xs">
                          {skill}
                        </Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-muted-foreground italic">No skills listed yet.</p>
                  )}
                </div>

                <div>
                  <h4 className="text-xs font-semibold uppercase text-muted-foreground tracking-wider mb-2 flex items-center gap-1.5">
                    <Heart className="h-3.5 w-3.5 text-rose-500" /> Interests & Hobbies
                  </h4>
                  {interestsList.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5">
                      {interestsList.map((interest, i) => (
                        <Badge key={i} variant="outline" className="text-xs">
                          {interest}
                        </Badge>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-muted-foreground italic">No interests listed yet.</p>
                  )}
                </div>
              </div>

              {user?.phone && (
                <div className="pt-2 text-xs text-muted-foreground flex items-center gap-2">
                  <Phone className="h-3.5 w-3.5 text-primary" />
                  <span>Contact: {user.phone}</span>
                </div>
              )}
            </div>
          ) : (
            /* EDIT FORM MODE */
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 pt-4 border-t border-border/50">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Full Name *</label>
                  <Input {...register('name')} />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Phone Number</label>
                  <Input placeholder="+1 555-0199" {...register('phone')} />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Department</label>
                  <Input placeholder="Computer Science" {...register('department')} />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Academic Year</label>
                  <Select {...register('year')}>
                    <option value="">Select Year</option>
                    <option value="1">1st Year</option>
                    <option value="2">2nd Year</option>
                    <option value="3">3rd Year</option>
                    <option value="4">4th Year</option>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Roll Number / Student ID</label>
                  <Input placeholder="CS-2024-042" {...register('rollNumber')} />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Bio / About</label>
                <Textarea
                  placeholder="Tell campus peers about your passion, projects, or goals..."
                  rows={3}
                  {...register('bio')}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Skills (Comma-separated)</label>
                  <Input
                    placeholder="React, Java, Spring Boot, UI Design"
                    {...register('skills')}
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-foreground">Interests (Comma-separated)</label>
                  <Input
                    placeholder="Robotics, Hackathons, Chess, Open Source"
                    {...register('interests')}
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Profile Picture URL</label>
                <Input
                  placeholder="https://images.unsplash.com/..."
                  {...register('profilePicture')}
                />
              </div>

              <div className="flex justify-end gap-3 pt-3">
                <Button type="button" variant="outline" onClick={cancelEdit} disabled={isSubmitting}>
                  Cancel
                </Button>
                <Button type="submit" disabled={isSubmitting} className="gap-1.5">
                  <Save className="h-4 w-4" />
                  {isSubmitting ? 'Saving...' : 'Save Profile'}
                </Button>
              </div>
            </form>
          )}
        </div>
      </Card>
    </div>
  )
}

export default Profile
