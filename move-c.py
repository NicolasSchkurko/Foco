import io

# --- AppAppSettingsScreen: use the new quick adjust + time range + alert toggle ---
p = 'app/src/main/java/com/foco/app/ui/detail/AppAppSettingsScreen.kt'
t = io.open(p, encoding='utf-8').read()

old_editor = '''                AppLimitEditor(
                    label = label,
                    minutes = minutes,
                    window = window,
                    alertEnabled = alertOn,
                    notificationsEnabled = notificationsEnabled,
                    onPick = { picked ->
                        scope.launch {
                            if (picked == null) {
                                limitsStore.clearLimit(packageName)
                            } else {
                                limitsStore.setLimit(packageName, picked)
                            }
                            syncMonitor()
                        }
                    },
                    onWindowChange = { start, end ->
                        scope.launch {
                            limitsStore.setWindow(packageName, start, end)
                            syncMonitor()
                        }
                    },
                    onWindowClear = {
                        scope.launch {
                            limitsStore.clearWindow(packageName)
                            syncMonitor()
                        }
                    },
                    onAlertChange = { enabled ->
                        if (enabled) requestNotificationPermission()
                        scope.launch {
                            limitsStore.setAlertEnabled(packageName, enabled)
                            syncMonitor()
                        }
                    },
                )'''

new_editor = '''                LimitsQuickAdjust(
                    store = limitsStore,
                    packageName = packageName,
                    onLimitChange = { pkg, picked ->
                        scope.launch {
                            if (picked == null) {
                                limitsStore.clearLimit(pkg)
                            } else {
                                limitsStore.setLimit(pkg, picked)
                            }
                            syncMonitor()
                        }
                    },
                    onWindowChange = { pkg, start, end ->
                        scope.launch {
                            limitsStore.setWindow(pkg, start, end)
                            syncMonitor()
                        }
                    },
                    onWindowClear = { pkg ->
                        scope.launch {
                            limitsStore.clearWindow(pkg)
                            syncMonitor()
                        }
                    },
                )

                LimitAlertsToggle(
                    label = label,
                    minutes = minutes,
                    alertEnabled = alertOn,
                    notificationsEnabled = notificationsEnabled,
                    onAlertChange = { enabled ->
                        if (enabled) requestNotificationPermission()
                        scope.launch {
                            limitsStore.setAlertEnabled(packageName, enabled)
                            syncMonitor()
                        }
                    },
                )'''

assert old_editor in t, 'editor not found'
t = t.replace(old_editor, new_editor, 1)

# import the shared component
t = t.replace('import com.foco.app.ui.common.ScreenHeader\n', 'import com.foco.app.ui.common.LimitAlertsToggle\nimport com.foco.app.ui.common.LimitsQuickAdjust\nimport com.foco.app.ui.common.ScreenHeader\n', 1)

io.open(p, 'w', encoding='utf-8').write(t)
print('app settings: new UI')
