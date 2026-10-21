import io

p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
t = io.open(p, encoding='utf-8').read()

# 1) Remove the limits item from the global settings list (the quick adjust moves to the app's tab).
start = t.find('                item {\n                    Spacer(Modifier.height(24.dp))\n                    SectionTitle(stringResource(R.string.detail_limits_section))')
assert start >= 0, 'limits item not found'
end = t.find('            }\n        }\n    }\n}', start)
assert end > start, 'item end not found'
# The item block ends right before the LazyColumn's closing brace; find the last '}' of the item.
item_end = t.rfind('                }', start, end)
t = t[:start] + t[item_end + len('                }'):]

# 2) Remove the now-shared LimitsQuickAdjust block and its private helpers.
block_start = t.find('@Composable\nprivate fun LimitsQuickAdjust(')
assert block_start >= 0, 'quick adjust block not found'
block_end = t.find('\nprivate fun formatMinuteOfDay', block_start)
fmt_start = block_end
fmt_end = t.find('\n}\n', fmt_start) + len('\n}\n')
t = t[:block_start] + t[fmt_end:]

io.open(p, 'w', encoding='utf-8').write(t)
print('settings: limits item + local quick adjust removed')
