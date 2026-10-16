workspace "Foco" "Personal Android app to track device usage per app — 100% local, no accounts, no servers." {

    model {
        user = person "User" "The phone owner — the only user."

        foco = softwareSystem "Foco" "Tracks usage per app as time sessions; shows a daily dashboard; limits/friction are planned (Phases 3-4)." {
            app = container "Android app" "Kotlin + Jetpack Compose (Material 3), single process, offline; four layered packages." {
                entry = component "Entry" "MainActivity: edge-to-edge window, theme and Compose entry point (App)."
                nav = component "Navigation" "FocoNavHost: Navigation Compose routes (dashboard, detail/{packageName}); the usage-access gate stays outside it, in App." "" "cap:usage-tracking,cap:code-structure"
                ui = component "UI" "Compose presentation: dashboard, app detail, onboarding, day selector, derived metrics, unlock count, formatting, theme." "" "cap:usage-tracking,cap:code-structure"
                data = component "Data" "Platform adapters: UsageRepository (UsageStatsManager events + KEYGUARD_HIDDEN unlocks), UsageAccess (usage-access AppOp), availableDays (retention window)." "" "cap:usage-tracking,cap:code-structure"
                storage = component "Local store" "Preferences DataStore 'excluded_apps' in app-private storage: the packages the user hid from the ranking." "" "cap:usage-tracking,cap:code-structure"
                domain = component "Domain" "Pure behavior: session fold (buildSessions) and package filtering (filterSessions)." "" "cap:usage-tracking,cap:code-structure"
                model = component "Model" "Pure types: UsageSession, UsageEvent, UsageEventType, DayData (sessions + unlock times)." "" "cap:usage-tracking,cap:code-structure"
            }
        }

        platform = softwareSystem "Android platform" "System-provided usage events, the special usage-access permission, launcher icons/labels and the back invocation behind the system back gesture." {
            tags "External"
        }

        user -> app "Uses"
        entry -> ui "Hosts the Compose tree"
        ui -> nav "Routes through"
        ui -> data "Loads the day's sessions and checks the usage-access permission"
        ui -> model "Renders"
        data -> domain "Folds and filters sessions"
        domain -> model "Produces"
        data -> storage "Persists the excluded packages"
        data -> platform "Queries events, permission state and package visibility"
        ui -> platform "Resolves launcher icons and labels"
        nav -> platform "Relies on the system back invocation"
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
