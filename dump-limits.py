p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
t = open(p, encoding='utf-8').read()

start = t.find('@Composable\nprivate fun LimitsQuickAdjust(')
end = t.find('\nprivate fun formatMinuteOfDay', start)
print('start', start, 'end', end)
print(t[start:end])
