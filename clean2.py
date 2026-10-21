import io

p = 'app/src/main/java/com/foco/app/ui/settings/SettingsScreen.kt'
t = io.open(p, encoding='utf-8').read()

# 1) focusLimits + scroll plumbing is gone with the limits item.
t = t.replace('fun SettingsScreen(focusLimits: Boolean = false) {', 'fun SettingsScreen() {', 1)
old = '''    val listState = rememberLazyListState()
    LaunchedEffect(focusLimits) {
        if (focusLimits) {
            // Limits section is the last item (index 4); scroll it into view.
            runCatching { listState.animateScrollToItem(4) }
        }
    }
'''
assert old in t, 'focus block'
t = t.replace(old, '', 1)
t = t.replace('                state = listState,\n', '', 1)

# 2) The limits store / job are gone with the limits item.
t = t.replace('    val limitsStore = remember { AppLimitsStore.forApp(context.applicationContext) }\n', '', 1)
t = t.replace('    var limitJob by remember { mutableStateOf<Job?>(null) }\n', '', 1)

# 3) Imports the file no longer uses.
for imp in [
    'import androidx.compose.foundation.layout.heightIn\n',
    'import androidx.compose.foundation.lazy.rememberLazyListState\n',
    'import androidx.compose.material3.DropdownMenu\n',
    'import androidx.compose.material3.DropdownMenuItem\n',
    'import androidx.compose.ui.semantics.contentDescription\n',
    'import androidx.compose.ui.semantics.semantics\n',
    'import kotlinx.coroutines.Job\n',
    'import kotlinx.coroutines.delay\n',
    'import android.text.format.DateFormat\n',
    'import androidx.compose.foundation.gestures.detectTapGestures\n',
    'import androidx.compose.foundation.layout.sizeIn\n',
    'import androidx.compose.material3.OutlinedButton\n',
    'import com.foco.app.data.AppLimitsStore\n',
    'import com.foco.app.data.UsageMonitorService\n',
    'import com.foco.app.domain.AppLimitRules\n',
    'import com.foco.app.domain.AppLimitWindowRules\n',
    'import com.foco.app.domain.AppWindow\n',
    'import java.util.Calendar\n',
]:
    t = t.replace(imp, '', 1)

io.open(p, 'w', encoding='utf-8').write(t)
print('cleaned')
