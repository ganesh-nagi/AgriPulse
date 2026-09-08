import { createBrowserRouter } from 'react-router-dom'
import { RequireAuth, RequireRole } from '../components/RequireAuth'
import FarmerLayout from '../layouts/FarmerLayout'
import StakeholderLayout from '../layouts/StakeholderLayout'
import AdminPage from '../pages/AdminPage'
import BuyerPage from '../pages/BuyerPage'
import AlertsPage from '../pages/farmer/AlertsPage'
import HomePage from '../pages/farmer/HomePage'
import MarketsPage from '../pages/farmer/MarketsPage'
import OnboardingPage from '../pages/farmer/OnboardingPage'
import ProfilePage from '../pages/farmer/ProfilePage'
import ReportPage from '../pages/farmer/ReportPage'
import VerificationPage from '../pages/farmer/VerificationPage'
import FpoPage from '../pages/FpoPage'
import LoginPage from '../pages/LoginPage'
import NotFoundPage from '../pages/NotFoundPage'
import RegisterPage from '../pages/RegisterPage'
import ResourcesPage from '../pages/ResourcesPage'

/**
 * Route map. Guards below are UX-only; every API enforces auth and roles
 * server-side from the JWT identity.
 */
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: (
          <RequireRole roles={['FARMER']} />
        ),
        children: [
          { path: '/onboarding', element: <OnboardingPage /> },
          { path: '/verify', element: <VerificationPage /> },
          {
            element: <FarmerLayout />,
            children: [
              { path: '/', element: <HomePage /> },
              { path: '/markets', element: <MarketsPage /> },
              { path: '/report', element: <ReportPage /> },
              { path: '/alerts', element: <AlertsPage /> },
              { path: '/profile', element: <ProfilePage /> },
            ],
          },
        ],
      },
      {
        path: '/buyer',
        element: <StakeholderLayout title="Buyer" />,
        children: [
          {
            element: <RequireRole roles={['BUYER']} />,
            children: [{ path: '', element: <BuyerPage /> }],
          },
        ],
      },
      {
        path: '/fpo',
        element: <StakeholderLayout title="FPO" />,
        children: [
          {
            element: <RequireRole roles={['FPO']} />,
            children: [{ path: '', element: <FpoPage /> }],
          },
        ],
      },
      {
        path: '/resources',
        element: <StakeholderLayout title="Storage & transport" />,
        children: [
          {
            element: <RequireRole roles={['STORAGE_OPERATOR', 'TRANSPORTER']} />,
            children: [{ path: '', element: <ResourcesPage /> }],
          },
        ],
      },
      {
        path: '/admin',
        element: <StakeholderLayout title="Admin" />,
        children: [
          {
            element: <RequireRole roles={['ADMIN']} />,
            children: [{ path: '', element: <AdminPage /> }],
          },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])
