# Copies the monument puzzle pieces you've saved with structure blocks into the monument_puzzles datapack.
# Structure blocks save into the world, so run this from the java folder with the world's folder, for example:
#   python3 tools/monument_puzzles/import.py run/saves/MyTestWorld
# Only pieces under strawberrymc:monument/ are copied. Pass --dry-run to just list what would change.
import filecmp, os, shutil, sys

args = [a for a in sys.argv[1:] if not a.startswith('--')]
if len(args) != 1:
    sys.exit(__doc__ if __doc__ else 'usage: import.py <world folder> [--dry-run]')
DRY = '--dry-run' in sys.argv
source = os.path.join(args[0], 'generated', 'strawberrymc', 'structure', 'monument')
target = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources', 'monument_puzzles',
                      'data', 'strawberrymc', 'structure', 'monument')
if not os.path.isdir(source):
    sys.exit(f'No saved monument pieces in {source}')

copied = 0
for folder, _, files in os.walk(source):
    for file in files:
        if not file.endswith('.nbt'):
            continue
        relative = os.path.relpath(os.path.join(folder, file), source)
        destination = os.path.join(target, relative)
        if os.path.exists(destination) and filecmp.cmp(os.path.join(folder, file), destination, shallow=False):
            continue
        print(('would copy ' if DRY else 'copied ') + 'monument/' + relative[:-4])
        if not DRY:
            os.makedirs(os.path.dirname(destination), exist_ok=True)
            shutil.copyfile(os.path.join(folder, file), destination)
        copied += 1
print(f'{copied} piece(s) {"to update" if DRY else "updated"}')
