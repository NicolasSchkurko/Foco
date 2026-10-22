import io, re

p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = io.open(p, encoding='utf-8').read()

# The clamp only runs when the window itself changes; otherwise the selection is kept.
old = '''    val effectiveIndex = reclampDayIndex(selectedIndex, selectedWindow, days, today)
    SideEffect {
        if (selectedIndex != effectiveIndex) selectedIndex = effectiveIndex
        selectedWindow = days
        days.getOrNull(effectiveIndex)?.toString()?.let { savedSelectedDate = it }
    }'''

new = '''    // The selection is valid as it stands while the window has not changed; when the window
    // changes (the store grew, today rolled over) it is reclamped into the new one.
    val windowChanged = selectedWindow != days
    val effectiveIndex =
        if (windowChanged) {
            reclampDayIndex(selectedIndex, selectedWindow, days, today)
        } else {
            selectedIndex.coerceIn(0, days.lastIndex.coerceAtLeast(0))
        }
    SideEffect {
        if (windowChanged) {
            val clamped = reclampDayIndex(selectedIndex, selectedWindow, days, today)
            if (selectedIndex != clamped) selectedIndex = clamped
            selectedWindow = days
        }
        days.getOrNull(effectiveIndex)?.toString()?.let { savedSelectedDate = it }
    }'''
assert old in t, 'clamp block not found'
t = t.replace(old, new, 1)

# Remove the diagnostics.
lines = t.split('\n')
out = []
for line in lines:
    if 'println("DIAG' in line:
        cleaned = re.sub(r'\s*println\("DIAG[^"]*"\)\s*;', '', line)
        cleaned = re.sub(r'\s*println\("DIAG[^"]*"\)', '', cleaned)
        if cleaned.strip() == '':
            continue
        out.append(cleaned)
    else:
        out.append(line)
t = '\n'.join(out)
assert 'DIAG' not in t, 'app diag left'
io.open(p, 'w', encoding='utf-8').write(t)
print('clamp fixed')

# The test: remove the diagnostics.
p2 = 'app/src/androidTest/java/com/foco/app/ui/AppDetailLimitsTest.kt'
t2 = io.open(p2, encoding='utf-8').read()
lines2 = t2.split('\n')
out2 = []
for line in lines2:
    if 'println("DIAG' in line:
        continue
    out2.append(line)
t2 = '\n'.join(out2)
assert 'DIAG' not in t2, 'test diag left'
io.open(p2, 'w', encoding='utf-8').write(t2)
print('test diag removed')
