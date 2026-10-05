import React from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { useAuth } from '../../context/AuthContext'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../../components/ui/Card'
import { toast } from 'sonner'
import { Sparkles } from 'lucide-react'

const loginSchema = z.object({
  email: z.string().email('Please enter a valid campus email address'),
  password: z.string().min(1, 'Password is required'),
})

export function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/dashboard'

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = async (data) => {
    try {
      await login(data)
      navigate(from, { replace: true })
    } catch (err) {
      toast.error(err.message || 'Invalid email or password')
    }
  }

  const fillDemoAccount = (email, password) => {
    setValue('email', email, { shouldValidate: true })
    setValue('password', password, { shouldValidate: true })
  }

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-background px-4 py-12">
      <div className="w-full max-w-md space-y-6">
        {/* Brand header */}
        <div className="text-center space-y-2">
          <div className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-primary text-primary-foreground font-black text-xl shadow-md">
            C
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Welcome to CampusOS</h1>
          <p className="text-sm text-muted-foreground">The unified student operating system for your campus</p>
        </div>

        <Card className="border-border/80 shadow-lg">
          <CardHeader className="space-y-1">
            <CardTitle className="text-xl">Sign in</CardTitle>
            <CardDescription>Enter your student credentials to access your dashboard</CardDescription>
          </CardHeader>
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent className="space-y-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-foreground">Email Address</label>
                <Input
                  type="email"
                  placeholder="student1@campus.edu"
                  {...register('email')}
                />
                {errors.email && (
                  <p className="text-xs text-destructive">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-foreground">Password</label>
                </div>
                <Input
                  type="password"
                  placeholder="••••••••"
                  {...register('password')}
                />
                {errors.password && (
                  <p className="text-xs text-destructive">{errors.password.message}</p>
                )}
              </div>

              {/* Demo accounts quick-fill buttons */}
              <div className="rounded-lg border border-border/60 bg-muted/30 p-3 space-y-2">
                <div className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
                  <Sparkles className="h-3.5 w-3.5 text-primary" />
                  <span>Demo quick fill:</span>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => fillDemoAccount('student1@campus.edu', 'password123')}
                    className="flex-1 rounded-md border border-border bg-background py-1 text-xs font-medium text-foreground hover:bg-accent transition-colors"
                  >
                    Student
                  </button>
                  <button
                    type="button"
                    onClick={() => fillDemoAccount('leader@campus.edu', 'password123')}
                    className="flex-1 rounded-md border border-border bg-background py-1 text-xs font-medium text-foreground hover:bg-accent transition-colors"
                  >
                    Club Admin
                  </button>
                  <button
                    type="button"
                    onClick={() => fillDemoAccount('admin@campus.edu', 'admin123')}
                    className="flex-1 rounded-md border border-border bg-background py-1 text-xs font-medium text-foreground hover:bg-accent transition-colors"
                  >
                    Admin
                  </button>
                </div>
              </div>
            </CardContent>

            <CardFooter className="flex flex-col space-y-4">
              <Button type="submit" className="w-full" isLoading={isSubmitting}>
                Sign in
              </Button>
              <div className="text-center text-xs text-muted-foreground">
                Don't have an account?{' '}
                <Link to="/register" className="font-semibold text-primary hover:underline">
                  Sign up
                </Link>
              </div>
            </CardFooter>
          </form>
        </Card>
      </div>
    </div>
  )
}

export default Login
