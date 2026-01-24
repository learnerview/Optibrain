import { create } from "zustand"
import { persist } from "zustand/middleware"

interface User {
  id: string
  email: string
  name: string
  avatar?: string
  company?: string
  role?: string
}

interface CloudProvider {
  id: string
  name: "AWS" | "Azure" | "GCP"
  accountId: string
  accountName: string
  environment: "prod" | "dev" | "staging" | "custom"
  regions: string[]
  connected: boolean
  lastSync?: string
  permissions: {
    costVisibility: boolean
    optimizationExecution: boolean
    autoRemediation: boolean
    securityCorrelation: boolean
    cicdCostInsights: boolean
    kubernetesInsights: boolean
    serverlessInsights: boolean
    carbonTracking: boolean
  }
}

interface AutonomousDecision {
  id: string
  timestamp: string
  action: string
  resource: string
  savings: number
  confidence: number
  status: "completed" | "pending" | "failed"
}

interface AuthStore {
  user: User | null
  isAuthenticated: boolean
  login: (email: string, password: string) => void
  logout: () => void
  setUser: (user: User) => void
}

interface CloudStore {
  providers: CloudProvider[]
  selectedProvider: string | null
  addProvider: (provider: CloudProvider) => void
  removeProvider: (id: string) => void
  selectProvider: (id: string) => void
  updateProvider: (id: string, updates: Partial<CloudProvider>) => void
  getActiveProviders: () => CloudProvider[]
}

interface OnboardingStore {
  step: number
  selectedProviders: ("AWS" | "Azure" | "GCP")[]
  setStep: (step: number) => void
  addSelectedProvider: (provider: "AWS" | "Azure" | "GCP") => void
  removeSelectedProvider: (provider: "AWS" | "Azure" | "GCP") => void
  resetOnboarding: () => void
}

interface DecisionsStore {
  decisions: AutonomousDecision[]
  addDecision: (decision: AutonomousDecision) => void
  updateDecisionStatus: (id: string, status: "completed" | "pending" | "failed") => void
  getRecentDecisions: (limit: number) => AutonomousDecision[]
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      login: (email, password) => {
        const user: User = {
          id: "1",
          email,
          name: email.split("@")[0],
          company: "Your Company",
          role: "Cloud Intelligence Engineer",
        }
        set({ user, isAuthenticated: true })
      },
      logout: () => {
        set({ user: null, isAuthenticated: false })
      },
      setUser: (user) => {
        set({ user, isAuthenticated: true })
      },
    }),
    {
      name: "auth-store",
    },
  ),
)

export const useCloudStore = create<CloudStore>()(
  persist(
    (set, get) => ({
      providers: [],
      selectedProvider: null,
      addProvider: (provider) => set((state) => ({ providers: [...state.providers, provider] })),
      removeProvider: (id) =>
        set((state) => ({
          providers: state.providers.filter((p) => p.id !== id),
          selectedProvider: state.selectedProvider === id ? null : state.selectedProvider,
        })),
      selectProvider: (id) => set({ selectedProvider: id }),
      updateProvider: (id, updates) =>
        set((state) => ({
          providers: state.providers.map((p) => (p.id === id ? { ...p, ...updates } : p)),
        })),
      getActiveProviders: () => get().providers.filter((p) => p.connected),
    }),
    {
      name: "cloud-store",
    },
  ),
)

export const useOnboardingStore = create<OnboardingStore>()(
  persist(
    (set) => ({
      step: 1,
      selectedProviders: [],
      setStep: (step) => set({ step }),
      addSelectedProvider: (provider) =>
        set((state) => ({
          selectedProviders: [...state.selectedProviders, provider],
        })),
      removeSelectedProvider: (provider) =>
        set((state) => ({
          selectedProviders: state.selectedProviders.filter((p) => p !== provider),
        })),
      resetOnboarding: () => set({ step: 1, selectedProviders: [] }),
    }),
    {
      name: "onboarding-store",
    },
  ),
)

export const useDecisionsStore = create<DecisionsStore>()(
  persist(
    (set, get) => ({
      decisions: [],
      addDecision: (decision) =>
        set((state) => ({
          decisions: [decision, ...state.decisions],
        })),
      updateDecisionStatus: (id, status) =>
        set((state) => ({
          decisions: state.decisions.map((d) => (d.id === id ? { ...d, status } : d)),
        })),
      getRecentDecisions: (limit) => get().decisions.slice(0, limit),
    }),
    {
      name: "decisions-store",
    },
  ),
)
