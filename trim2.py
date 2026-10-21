p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
raw = open(p, encoding='utf-8').read()
lines = raw.split('\n')

# 1-based: keep 1..314, drop 315..595 (the moved composables + helper), keep the trailing empty line.
assert lines[314] == '@Composable', repr(lines[314])
assert lines[315] == 'private fun LimitsQuickAdjust(', repr(lines[315])
assert lines[594] == '    }', repr(lines[594])
assert lines[595] == '', repr(lines[595])

new = lines[:314] + lines[595:]
out = '\n'.join(new)
open(p, 'w', encoding='utf-8').write(out)
print('new line count:', len(new))
print('LimitsQuickAdjust left:', out.count('LimitsQuickAdjust'))
print('formatMinuteOfDay left:', out.count('formatMinuteOfDay'))
print('TimeRangePicker left:', out.count('TimeRangePicker'))
