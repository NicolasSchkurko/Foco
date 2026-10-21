p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()

# 1) Header action: the gear becomes the limits icon (opens the app's limit settings).
old_action = '''                actions = {
                    IconButton(onClick = onConfigure) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.detail_limits_configure),
                        )
                    }
                },'''
new_action = '''                actions = {
                    IconButton(onClick = onConfigure) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.detail_limits_configure),
                        )
                    }
                },'''
assert old_action in t, 'action not found'
t = t.replace(old_action, new_action, 1)

# 2) Import the limits icon.
if 'import androidx.compose.material.icons.filled.Notifications' not in t:
    t = t.replace('import androidx.compose.material.icons.filled.Settings\n', 'import androidx.compose.material.icons.filled.Notifications\n', 1)

open(p, 'w', encoding='utf-8').write(t)
print('detail header updated')
