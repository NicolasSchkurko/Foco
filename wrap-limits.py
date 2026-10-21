p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = open(p, encoding='utf-8').read()

start = t.find('                        // Preview: limit / window / alert for this app')
end = t.find('                        Spacer(Modifier.height(16.dp))\n\n                        HourChart', start)
assert start >= 0 and end >= 0, 'block not found'

block = t[start:end]  # ends right before the Spacer before HourChart
# The block currently starts with the comment and ends with the preview.forEach closing brace + newline.
# Wrap: prepend a clickable Column open, append close.
wrapped = ('                        Column(\n'
           '                            modifier = Modifier\n'
           '                                .fillMaxWidth()\n'
           '                                .clickable { onConfigure() }\n'
           '                                .padding(vertical = 4.dp),\n'
           '                        ) {\n'
           + block +
           '                        }\n')

t = t[:start] + wrapped + t[end:]
open(p, 'w', encoding='utf-8').write(t)
print('wrapped limits block')
print('---')
# verify
i = t.find('Preview: limit / window / alert')
print(t[i-60:i+80])
