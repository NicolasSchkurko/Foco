workspace "Foco" "Personal Android app to track device usage per app — 100% local, no accounts, no servers." {

    model {
        user = person "User" "The phone owner — the only user."

        foco = softwareSystem "Foco" "Tracks usage per app as time sessions; shows a daily dashboard; limits/friction are planned (Phases 3-4)." {
            app = container "Android app" "Kotlin + Jetpack Compose (Material 3), single process, offline; four layered packages." {
                entry = component "Entry" "MainActivity: edge-to-edge window, theme and Compose entry point (App); gates the entry on the usage-access permission." "" "cap:app-entry,cap:code-structure"
                nav = component "Navigation" "FocoNavHost: Navigation Compose routes (dashboard, detail/{packageName}, settings); the usage-access gate stays outside it, in App." "" "cap:app-settings,cap:usage-tracking,cap:code-structure"
                ui = component "UI" "Compose presentation: dashboard, app detail, onboarding, settings, day selector, derived metrics, unlock count, formatting, theme." "" "cap:app-entry,cap:app-settings,cap:usage-tracking,cap:code-structure"
                data = component "Data" "Platform adapters: UsageRepository (UsageStatsManager events + KEYGUARD_HIDDEN unlocks, launcher/home package visibility), UsageAccess (usage-access AppOp and the settings intent that opens it), AppPackageInfo (launcher icon and label reads), the preferences stores (excluded apps, settings, language) and the one-shot settings migration." "" "cap:app-settings,cap:usage-tracking,cap:code-structure"
                storage = component "Local store" "Two preferences DataStores in app-private storage: 'excluded_apps' (key 'excluded_packages': the packages hidden from the ranking) and 'foco_settings' (language and appearance, migrated out of the legacy store on first start)." "" "cap:app-settings,cap:usage-tracking,cap:code-structure"
                domain = component "Domain" "Pure behavior: session fold (buildSessions), package filtering (filterSessions) and the offered-day window (availableDays)." "" "cap:usage-tracking,cap:code-structure"
                model = component "Model" "Pure types: UsageSession, UsageEvent, UsageEventType, DayData (sessions + unlock times), LanguageOption and AppearanceOption (the settings choices)." "" "cap:app-settings,cap:usage-tracking,cap:code-structure"
            }
        }

        platform = softwareSystem "Android platform" "System-provided usage events, the special usage-access permission, launcher icons/labels and the back invocation behind the system back gesture." {
            tags "External"
        }

        checks = softwareSystem "Development checks" "The repository's own verification, never application behavior: the tripwires under app/src/test/java/com/foco/app/structure/ (layer rules, the boundary check and its fixture, the base locale resources, the config shape), the gate in scripts/gates/ and the CI workflow." {
            tags "cap:spec-tooling"
        }

        user -> app "Uses"
        entry -> ui "Hosts the Compose tree"
        entry -> platform "Declared as the launcher activity: usage-access permission, launcher/home queries, predictive-back callback"
        ui -> nav "Routes through"
        ui -> data "Loads the day's sessions, icons and labels, the excluded packages; checks the usage-access permission"
        ui -> domain "Offers the days inside the retention window"
        ui -> model "Renders"
        ui -> storage "Reads and writes the packages hidden from the ranking"
        data -> domain "Folds and filters sessions"
        domain -> model "Produces"
        data -> platform "Queries events, permission state and package visibility"
        ui -> platform "Opens the system usage-access settings screen and dispatches the system back invocation"
        checks -> app "Runs the tripwires over the app's shape, its resources and the tooling configuration"
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
