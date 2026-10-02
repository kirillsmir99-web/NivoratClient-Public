from pathlib import Path
from collections import Counter
import io
import json
import re
import zipfile
import sys

sys.stdout.reconfigure(encoding='utf-8')

root = Path(__file__).resolve().parents[1]
artifact = root / 'build/libs/NivoratClient.jar'
checks = {
    'retired_names': re.compile(rb'Kimiko|kimiko|Neural|neural|PulseHUD|kt1xW|RemoteLockService'),
    'process_or_network_apis': re.compile(rb'java/net/(?:Socket|Http|URLConnection)|java/lang/ProcessBuilder|loadLibrary|openConnection|webhook'),
    'irc': re.compile(rb'(?i)(?:^|[^a-z])irc(?:[^a-z]|$)'),
}
results = {name: [] for name in checks}
urls = set()
native_files = []

def inspect(data, prefix=''):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for entry in archive.infolist():
            if entry.is_dir():
                continue
            content = archive.read(entry)
            name = prefix + entry.filename
            if name.endswith(('.dll', '.exe', '.so', '.dylib', '.ps1', '.bat')):
                native_files.append(name)
            for check, pattern in checks.items():
                if pattern.search(content) or pattern.search(name.encode()):
                    results[check].append(name)
            if name.endswith('.class'):
                urls.update(match.decode(errors='replace') for match in re.findall(rb'https?://[a-zA-Z0-9./@_?=&:%#-]+', content))
            if name.endswith('.jar'):
                inspect(content, name + '!/')

inspect(artifact.read_bytes())
report = {'artifact': str(artifact), 'size_bytes': artifact.stat().st_size,
          'matches': results, 'native_or_script_files': native_files, 'class_urls': sorted(urls)}
events_path = root / 'build/profiles/release-audit-events.json'
if events_path.exists():
    events = json.loads(events_path.read_text(encoding='utf-8-sig'))['recording']['events']
    counts = Counter(event['type'] for event in events)
    own_samples = Counter()
    writes = []
    pauses = []
    for event in events:
        values = event['values']
        if event['type'] == 'jdk.ExecutionSample':
            frames = (values.get('stackTrace') or {}).get('frames', [])
            own = next((f['method']['type']['name'] for f in frames
                        if f['method']['type']['name'].startswith(('activity/', 'dev/virion/', 'dev/nivorat/', 'dev/pearl/', 'net/redstone/'))), None)
            if own:
                own_samples[own] += 1
        if event['type'] == 'jdk.FileWrite':
            writes.append({'path': values.get('path'), 'duration': values.get('duration')})
        if event['type'] == 'jdk.GCPhasePause':
            pauses.append(values.get('duration'))
    report['jfr'] = {'events': dict(counts), 'own_execution_samples': own_samples.most_common(20),
                     'file_writes': writes, 'gc_pauses': pauses}
output = root / 'build/release-audit.json'
output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k != 'jfr'}, ensure_ascii=False, indent=2))
if 'jfr' in report:
    print(json.dumps({k:v for k,v in report['jfr'].items() if k != 'file_writes'}, ensure_ascii=False, indent=2))
