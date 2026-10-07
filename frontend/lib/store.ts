import { create } from "zustand"
import { persist } from "zustand/middleware"
import * as api from "@/lib/api"

interface User {
  id: string
  email: string
  authorities: string[]
}

/**
 * The account OptiBrain is pointed at.
 *
 * <p>Azure and GCP were removed: the backend only implements AWS, so offering them in the
 * onboarding UI would produce an account that can never connect.
 */
interface CloudAccount {
  id: string
  /** AWS account id, or the literal "sandbox" for LocalStack. */
  accountId: string
  accountName: string
  /** Mirrors the backend's cloud.mode. */
  executionTarget: "SANDBOX" | "AWS"
  regions: string[]
  connected: boolean
  lastChecked?: string
}

interface AuthStore {
  user: User | null
  /** True only after the backend has verified the credentials. */
  isAuthenticated: boolean
  /** null until the backend has been asked, so the UI can distinguish "unknown" from "no". */
  loginError: string | null
  login: (email: string, password: string) => Promise<boolean>
  logout: () => Promise<void>
}

interface CloudStore {
  accounts: CloudAccount[]
  selectedAccount: string | null
  upsertAccount: (account: CloudAccount) => void
  removeAccount: (id: string) => void
  selectAccount: (id: string) => void
  setConnection: (id: string, connected: boolean) => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      loginError: null,

      /**
       * Signs in against the backend.
       *
       * <p>This previously invented a user object and set `isAuthenticated: true` for any
       * email and any password, without contacting the server. Any dashboard protected
       * only by this flag was therefore reachable by typing anything.
       */
      login: async (username, password) => {
        set({ loginError: null })
        try {
          const result = await api.login(username, password)
          if (typeof window !== "undefined") {
            window.localStorage.setItem("optibrain_token", result.token)
          }
          set({
            user: {
              id: result.username,
              email: result.username,
              authorities: result.authorities ?? [],
            },
            isAuthenticated: true,
          })
          return true
        } catch (cause) {
          set({
            user: null,
            isAuthenticated: false,
            loginError:
              cause instanceof api.ApiError
                ? cause.message
                : "Could not reach the OptiBrain backend.",
          })
          return false
        }
      },

      logout: async () => {
        try {
          await api.logout()
        } catch {
          // The local session is cleared regardless: a failed logout request must not
          // leave the UI showing a signed-in shell.
        }
        if (typeof window !== "undefined") {
          window.localStorage.removeItem("optibrain_token")
          window.localStorage.removeItem("auth-store")
          window.localStorage.removeItem("cloud-store")
        }
        set({ user: null, isAuthenticated: false })
      },
    }),
    { name: "auth-store" },
  ),
)

export const useCloudStore = create<CloudStore>()(
  persist(
    (set) => ({
      accounts: [],
      selectedAccount: null,
      upsertAccount: (account) =>
        set((state) => {
          const existing = state.accounts.find((a) => a.id === account.id)
          const accounts = existing
            ? state.accounts.map((a) => (a.id === account.id ? account : a))
            : [...state.accounts, account]
          return {
            accounts,
            selectedAccount: state.selectedAccount ?? account.id,
          }
        }),
      removeAccount: (id) =>
        set((state) => ({
          accounts: state.accounts.filter((a) => a.id !== id),
          selectedAccount:
            state.selectedAccount === id ? null : state.selectedAccount,
        })),
      selectAccount: (id) => set({ selectedAccount: id }),
      setConnection: (id, connected) =>
        set((state) => ({
          accounts: state.accounts.map((a) =>
            a.id === id
              ? { ...a, connected, lastChecked: new Date().toISOString() }
              : a
          ),
        })),
    }),
    { name: "cloud-store" },
  ),
)