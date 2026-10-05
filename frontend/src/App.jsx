import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import { ThemeProvider } from './context/ThemeContext'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { PublicRoute } from './routes/PublicRoute'
import { LoadingScreen } from './components/common/LoadingScreen'
import { AppLayout } from './components/layout/AppLayout'

// Lazy-loaded routes for code splitting
const Login = lazy(() => import('./features/auth/Login'))
const Register = lazy(() => import('./features/auth/Register'))
const Dashboard = lazy(() => import('./features/dashboard/Dashboard'))
const Events = lazy(() => import('./features/events/Events'))
const Clubs = lazy(() => import('./features/clubs/Clubs'))
const Announcements = lazy(() => import('./features/announcements/Announcements'))
const Resources = lazy(() => import('./features/resources/Resources'))
const LostFound = lazy(() => import('./features/lostFound/LostFound'))
const Opportunities = lazy(() => import('./features/opportunities/Opportunities'))
const HelpDesk = lazy(() => import('./features/helpDesk/HelpDesk'))
const Profile = lazy(() => import('./features/profile/Profile'))
const NotFound = lazy(() => import('./routes/NotFound'))

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 1000 * 60 * 2, // 2 minutes
    },
  },
})

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <Suspense fallback={<LoadingScreen />}>
              <Routes>
                {/* Public Routes */}
                <Route
                  path="/login"
                  element={
                    <PublicRoute>
                      <Login />
                    </PublicRoute>
                  }
                />
                <Route
                  path="/register"
                  element={
                    <PublicRoute>
                      <Register />
                    </PublicRoute>
                  }
                />

                {/* Protected App Routes */}
                <Route
                  path="/"
                  element={
                    <ProtectedRoute>
                      <AppLayout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route path="dashboard" element={<Dashboard />} />
                  <Route path="events" element={<Events />} />
                  <Route path="clubs" element={<Clubs />} />
                  <Route path="announcements" element={<Announcements />} />
                  <Route path="resources" element={<Resources />} />
                  <Route path="lost-found" element={<LostFound />} />
                  <Route path="opportunities" element={<Opportunities />} />
                  <Route path="help-desk" element={<HelpDesk />} />
                  <Route path="profile" element={<Profile />} />
                </Route>

                {/* 404 Route */}
                <Route path="*" element={<NotFound />} />
              </Routes>
            </Suspense>
          </BrowserRouter>
          <Toaster position="bottom-right" richColors closeButton />
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

export default App
