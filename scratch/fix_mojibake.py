import os
import re

def fix_string(s):
    current = s
    for _ in range(4):
        try:
            if 'Ã' not in current:
                break
            reverted = current.encode('cp1252').decode('utf-8')
            current = reverted
        except Exception:
            break
    return current

def process_match(match):
    original = match.group(0)
    return fix_string(original)

directory = r'f:\ltw\cinema\src\main\java\com\cinema'
changed_files = 0

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = re.sub(r'"([^"\\]*(\\.[^"\\]*)*)"', process_match, content)
            
            if new_content != content:
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                changed_files += 1
                print(f"Fixed: {path}")

print(f'Total fixed {changed_files} files.')
