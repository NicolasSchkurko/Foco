workspace "Foco" "Personal Android app to track device usage per app — 100% local, no accounts, no servers." {

    model {
        user = person "User" "The phone owner — the only user."

        foco = softwareSystem "Foco" "Tracks usage per app as time sessions; shows a daily dashboard; limits/friction are planned (Phases 3-4)." {
            app = container "Android app" "Kotlin + Jetpack Compose (Material 3), single process, offline; four layered packages." {
                entry = component "Entry" "MainActivity + App routing (onboarding to dashboard)."
                ui = component "UI" "Compose presentation: dashboard, onboarding, derived metrics, formatting, theme."
                data = component "Data" "Platform adapters: UsageRepository (UsageStatsManager events) and UsageAccess (usage-access AppOp)."
                domain = component "Domain" "Pure behavior: session fold (buildSessions) and package filtering (filterSessions)."
                model = component "Model" "Pure types: UsageSession, UsageEvent, UsageEventType."
            }
        }

        platform = softwareSystem "Android platform" "System-provided usage events and the special usage-access permission." {
            tags "External"
        }

        user -> app "Uses"
        entry -> ui "Routes to"
        entry -> data "Checks the usage-access permission"
        ui -> data "Loads the day's sessions"
        ui -> model "Renders"
        data -> domain "Folds and filters sessions"
        domain -> model "Produces"
        data -> platform "Queries events and permission state"
    }

    views {
        systemContext foco "SystemContext" {
            include *
            autolayout
        }
        container foco "Containers" {
            include *
            autolayout
        }
        component app "Components" {
            include *
            autolayout
        }
        theme default
    }
}
