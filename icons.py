import glob, os, re, zipfile

# material-icons-core aar
cands = glob.glob(r'C:\Users\Luzbelito-WrkLp\.gradle\caches\modules-2\files-2.1\androidx.compose.material\material-icons-core-android\**\*.aar', recursive=True)
print('aars:', len(cands))
names = set()
for a in cands:
    try:
        z = zipfile.ZipFile(a)
        for n in z.namelist():
            if n.endswith('.class') and 'filled/' in n and n.endswith('Kt.class'):
                names.add(n.split('/')[-1][:-8])
    except Exception as e:
        print('err', e)
print('total filled icons:', len(names))
for probe in ['Tune', 'Timer', 'Notifications', 'Settings', 'Share', 'DateRange', 'Alarm', 'Block', 'Speed', 'Flag', 'Edit', 'List', 'Check', 'Add', 'Remove']:
    print(probe, probe in names)
