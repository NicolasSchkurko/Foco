import io, re

p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
t = io.open(p, encoding='utf-8').read()

# 1) Remove the leftover formatMinuteOfDay helper (now in ui/common).
i = t.find('private fun formatMinuteOfDay(')
assert i >= 0
j = t.find('\n}\n', i) + len('\n}\n')
t = t[:i] + t[j:]

# 2) The focus/scroll plumbing is gone with the limits item.
t = t.replace('fun SettingsScreen(focusLimits: Boolean = false) {', 'fun SettingsScreen() {', 1)
old_state = '''    val listState = rememberLazyListState()
    LaunchedEffect(focusLimits) {
        if (focusLimits) {
            // Limits section is the last item (index 4); scroll it into view.
            runCatching { listState.animateScrollToItem(4) }
        }
    }
'''
assert old_state in t, 'focus block not found'
t = t.replace(old_state, '', 1)
t = t.replace('                state = listState,\n', '', 1)

# 3) The limits store / job are gone with the limits item.
t = t.replace('    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }\n', '', 1)
t = t.replace('    var limitJob by remember { mutableStateOf<Job?>(null) }\n', '', 1)

# 4) Drop the imports the file no longer uses.
for imp in [
    'import kotlinx.coroutines.Job\n',
    'import kotlinx.coroutines.delay\n',
    'import android.text.format.DateFormat\n',
    'import androidx.compose.foundation.gestures.detectTapGestures\n',
    'import com.foco.app.data.AppLimitsStore\n',
    'import com.foco.app.data.UsageMonitorService\n',
    'import com.foco.app.domain.AppLimitRules\n',
    'import com.foco.app.domain.AppLimitWindowRules\n',
    'import com.foco.app.domain.AppWindow\n',
    'import java.util.Calendar\n',
    'import androidx.compose.foundation.lazy.rememberLazyListState\n',
]:
    t = t.replace(imp, '', 1)

io.open(p, 'w', encoding='utf-8').write(t)
print('settings cleaned')
