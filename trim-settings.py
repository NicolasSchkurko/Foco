p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
lines = open(p, encoding='utf-8').read().split('\n')

# Lines are 1-based in the grep output. Remove 315..596 (the moved composables + helper).
start = 315  # "@Composable" before LimitsQuickAdjust
# find the end: the closing brace of formatMinuteOfDay
end = None
for i in range(575, len(lines)):
    if lines[i] == '}' and lines[i - 1].strip() == '}':
        end = i + 1
        break
assert end is not None, 'end not found'
print('removing lines', start, 'to', end)
print('first:', repr(lines[start - 1]))
print('last:', repr(lines[end - 1]))

new = lines[:start - 1] + lines[end:]
open(p, 'w', encoding='utf-8').write('\n'.join(new))
print('done, new line count', len(new))
