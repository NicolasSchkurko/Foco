import io

p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
t = io.open(p, encoding='utf-8').read()
# DropdownRow still uses the dropdown menu.
t = t.replace('import androidx.compose.material3.MaterialTheme\n', 'import androidx.compose.material3.DropdownMenu\nimport androidx.compose.material3.DropdownMenuItem\nimport androidx.compose.material3.MaterialTheme\n', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('done')
