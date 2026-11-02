# Generates the Ocean Monument puzzle jigsaws (see the wiki's "Puzzle jigsaws" section) into the built-in
# monument_puzzles datapack (src/main/resources/monument_puzzles), which MCreator never regenerates.
# Run it with `python3 tools/monument_puzzles/gen.py` from the java folder after changing the tree below.
#
# The template pools and the test structure are always rewritten. Each piece's .nbt starts as a labelled placeholder and is
# only written when it doesn't exist yet, so rooms you've built and imported (tools/monument_puzzles/import.py) are kept.
# Pass --reset-placeholders to overwrite every piece with its placeholder again.
#
# How the pieces fit together:
#  - Every puzzle room is ROOM wide, tall and deep and is entered on its north wall and left on its south wall. The doorway's
#    jigsaw is at the bottom middle of a 3x3 opening, and the key's copper grate pipe crosses the same wall PIPE blocks from
#    it: walking along the route, the pipe is always 4 blocks to the left and 7 up. So when two rooms' doorways connect,
#    their pipes line up too.
#  - A sub-variant that only changes what's inside a room (a different hiding place, seagrass bed, decoration) is an
#    overlay: a piece the size of the room's inside, attached by a jigsaw in the room's floor corner. Its structure voids
#    leave the room alone, so an overlay only needs the blocks it changes.
#  - A sub-variant that changes the room's shape (two gaps instead of one, a side room) is a separate room in the pool.
import gzip, json, os, struct, sys

R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources')
P = R + '/monument_puzzles'
DATA_VERSION = 4790  # Minecraft 26.1.2
NS = 'strawberrymc'
RESET = '--reset-placeholders' in sys.argv

ROOM = (16, 12, 16)
INSIDE = (ROOM[0] - 2, ROOM[1] - 2, ROOM[2] - 2)
DOOR_X = 7
PIPE = (4, 7)  # blocks to the left and up from the doorway's jigsaw, walking along the route
SIDE_ROOM = (12, 12, 12)
SIDE_INSIDE = (SIDE_ROOM[0] - 2, SIDE_ROOM[1] - 2, SIDE_ROOM[2] - 2)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(json.dumps(obj, indent=2) + '\n')


# ---- Minimal NBT writer: just enough for structure templates -----------------------------------------------------------
class Int(int): pass
class Byte(int): pass

def _name(s):
    b = s.encode('utf-8')
    return struct.pack('>H', len(b)) + b

def _tag_id(v):
    if isinstance(v, Byte): return 1
    if isinstance(v, bool): return 1
    if isinstance(v, int): return 3
    if isinstance(v, str): return 8
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(v)

def _payload(v):
    t = _tag_id(v)
    if t == 1: return struct.pack('>b', int(v))
    if t == 3: return struct.pack('>i', v)
    if t == 8: return _name(v)
    if t == 9:
        inner = _tag_id(v[0]) if v else 0
        return struct.pack('>bi', inner, len(v)) + b''.join(_payload(x) for x in v)
    return b''.join(struct.pack('>b', _tag_id(x)) + _name(k) + _payload(x) for k, x in v.items()) + b'\0'

def write_nbt(path, root):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.open(path, 'wb') as f:
        f.write(b'\x0a' + _name('') + _payload(root))


# ---- Placeholder pieces --------------------------------------------------------------------------------------------------
class Piece:
    def __init__(self, size):
        self.size = size
        self.cells = {}  # (x, y, z) -> (name, properties, block entity nbt)

    def set(self, pos, name, props=None, nbt=None):
        x, y, z = pos
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.cells[pos] = (name, props or {}, nbt)

    def fill(self, a, b, name, props=None):
        for x in range(a[0], b[0] + 1):
            for y in range(a[1], b[1] + 1):
                for z in range(a[2], b[2] + 1):
                    self.set((x, y, z), name, props)

    def shell(self, wall, floor, ceiling, inside, inside_props=None):
        sx, sy, sz = self.size
        self.fill((0, 0, 0), (sx - 1, sy - 1, sz - 1), wall)
        self.fill((0, 0, 0), (sx - 1, 0, sz - 1), floor)
        self.fill((0, sy - 1, 0), (sx - 1, sy - 1, sz - 1), ceiling)
        self.fill((1, 1, 1), (sx - 2, sy - 2, sz - 2), inside, inside_props)

    def jigsaw(self, pos, orientation, name='minecraft:empty', target='minecraft:empty', pool='minecraft:empty',
               final_state='minecraft:air', selection_priority=0):
        self.set(pos, 'minecraft:jigsaw', {'orientation': orientation}, {
            'id': 'minecraft:jigsaw', 'name': name, 'target': target, 'pool': pool, 'final_state': final_state,
            'joint': 'aligned', 'placement_priority': Int(0), 'selection_priority': Int(selection_priority)})

    def sign(self, pos, lines, waterlogged=True):
        text = {'messages': (lines + ['', '', '', ''])[:4], 'color': 'black', 'has_glowing_text': Byte(1)}
        self.set(pos, 'minecraft:oak_sign', {'rotation': '0', 'waterlogged': str(waterlogged).lower()},
                 {'id': 'minecraft:sign', 'front_text': text, 'back_text': text, 'is_waxed': Byte(1)})

    def save(self, location):
        path = f'{P}/data/{NS}/structure/{location}.nbt'
        if os.path.exists(path) and not RESET:
            return
        palette, index, blocks = [], {}, []
        for pos in sorted(self.cells):
            name, props, nbt = self.cells[pos]
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = {'Name': name}
                if props:
                    entry['Properties'] = dict(props)
                palette.append(entry)
            block = {'pos': [Int(c) for c in pos], 'state': Int(index[key])}
            if nbt:
                block['nbt'] = nbt
            blocks.append(block)
        write_nbt(path, {'DataVersion': Int(DATA_VERSION), 'size': [Int(c) for c in self.size], 'palette': palette,
                         'blocks': blocks, 'entities': []})


WATER = ('minecraft:water', {'level': '0'})
GRATE = ('minecraft:copper_grate', {'waterlogged': 'true'})


def doorway(piece, z, outward, jigsaw_args):
    # 3x3 opening with its jigsaw at the bottom middle, and the pipe through the wall beside it
    for dx in (-1, 0, 1):
        for y in (1, 2, 3):
            piece.set((DOOR_X + dx, y, z), *WATER)
    piece.jigsaw((DOOR_X, 1, z), f'{outward}_up', final_state='minecraft:water', **jigsaw_args)
    # Left of the route: entering through the north wall heading south, left is east (+x)
    piece.set((DOOR_X + PIPE[0], 1 + PIPE[1], z), *GRATE)


def route_room(label, location, out_pool, marker, slot=None, side_door=None):
    room = Piece(ROOM)
    room.shell('minecraft:prismarine_bricks', 'minecraft:dark_prismarine', 'minecraft:prismarine', *WATER)
    doorway(room, 0, 'north', {'name': f'{NS}:route_in'})
    doorway(room, ROOM[2] - 1, 'south', {'name': f'{NS}:route_out', 'target': f'{NS}:route_in', 'pool': out_pool,
                                         'selection_priority': 10})
    # Placeholder pipe from door to door along the ceiling
    for z in range(1, ROOM[2] - 1):
        room.set((DOOR_X + PIPE[0], 1 + PIPE[1], z), *GRATE)
    room.sign((DOOR_X, 1, 3), ['P2 piston puzzle', label, 'placeholder room'])
    room.set((DOOR_X, 0, 3), marker)  # Tells the variants apart at a glance
    if slot:
        # Overlays attach above this corner of the floor
        room.jigsaw((1, 0, 1), 'up_north', target=f'{NS}:overlay', pool=slot, final_state='minecraft:dark_prismarine')
    if side_door:
        for dz in (-1, 0, 1):
            for y in (1, 2, 3):
                room.set((0, y, DOOR_X + dz), *WATER)
        room.jigsaw((0, 1, DOOR_X), 'west_up', target=f'{NS}:side_room_in', pool=side_door, final_state='minecraft:water',
                    selection_priority=5)
    room.save(location)


def overlay(label, what, location, size, corner_state, marker='minecraft:lime_wool', waterlogged=True):
    # Everything not listed is left out of the template, so it behaves like structure void
    piece = Piece(size)
    piece.jigsaw((0, 0, 0), 'down_north', name=f'{NS}:overlay', final_state=corner_state)
    middle = (size[0] // 2, 0, size[2] // 2)
    piece.set(middle, marker)
    piece.sign((middle[0], 1, middle[2]), [label, what, 'overlay placeholder'], waterlogged)
    piece.save(location)


def side_room(label, location, slot=None):
    # Kept dry behind a blockade, with a lava pit under the parkour
    room = Piece(SIDE_ROOM)
    sx, sy, sz = SIDE_ROOM
    room.shell('minecraft:prismarine_bricks', 'minecraft:dark_prismarine', 'minecraft:prismarine', 'minecraft:air')
    room.fill((1, 1, 1), (sx - 2, 2, sz - 2), 'minecraft:lava', {'level': '0'})
    room.fill((sx - 4, 1, 3), (sx - 2, 3, 7), 'minecraft:dark_prismarine')  # landing inside the door
    room.fill((1, 1, 3), (3, 3, 7), 'minecraft:dark_prismarine')  # chest platform
    room.set((2, 4, 5), 'minecraft:chest', {'facing': 'east', 'waterlogged': 'false'})
    door_z = 5
    for dz in (-1, 0, 1):
        for y in (4, 5, 6):
            room.set((sx - 1, y, door_z + dz), 'minecraft:stone_bricks')  # blockade
    room.jigsaw((sx - 1, 4, door_z), 'east_up', name=f'{NS}:side_room_in', final_state='minecraft:stone_bricks')
    room.sign((sx - 3, 4, door_z), ['P2 side room', label, 'placeholder'], False)
    if slot:
        room.jigsaw((1, 0, 1), 'up_north', target=f'{NS}:overlay', pool=slot, final_state='minecraft:dark_prismarine')
    room.save(location)


# ---- Pools ---------------------------------------------------------------------------------------------------------------
def single(location, liquid=None):
    element = {'element_type': 'minecraft:single_pool_element', 'location': f'{NS}:{location}',
               'processors': 'minecraft:empty', 'projection': 'rigid'}
    if liquid:
        element['override_liquid_settings'] = liquid
    return element

def pool(name, entries):
    w(f'{P}/data/{NS}/worldgen/template_pool/{name}.json', {
        'fallback': 'minecraft:empty',
        'elements': [{'weight': weight, 'element': element} for element, weight in entries]})

EMPTY = {'element_type': 'minecraft:empty_pool_element'}
M = 'monument'
AFTER_P2 = f'{NS}:{M}/after_p2'

w(P + '/pack.mcmeta', {'pack': {'min_format': 101.1, 'max_format': 101.1,
                                'description': 'Strawberry MC Ocean Monument puzzle jigsaws'}})

# P2 rooms. Variant 3 has three shapes (3, 3,1 and 3,2), so each gets a third of variant 3's share
p2_rooms = [
    ('Variant 1', 'v1', 'white_concrete', f'{NS}:{M}/p2/v1/hiding', None, 3),
    ('Variant 2', 'v2', 'yellow_concrete', f'{NS}:{M}/p2/v2/bed', None, 3),
    ('Variant 3', 'v3', 'blue_concrete', None, None, 1),
    ('Variant 3,1', 'v3_1', 'cyan_concrete', f'{NS}:{M}/p2/v3_1/barrel', None, 1),
    ('Variant 3,2', 'v3_2', 'purple_concrete', None, f'{NS}:{M}/p2/v3_2/side_room', 1),
]
for label, name, marker, slot, side, _ in p2_rooms:
    route_room(label, f'{M}/p2/{name}', AFTER_P2, 'minecraft:' + marker, slot, side)
pool(f'{M}/p2', [(single(f'{M}/p2/{name}'), weight) for _, name, _, _, _, weight in p2_rooms])

# Overlays inside the rooms
overlays = {
    'p2/v1/hiding': [('Variant 1', '1', 'crouch slab chest'), ('Variant 1,1', '1_1', 'trapdoor chest'),
                     ('Variant 1,2', '1_2', 'chest left behind kelp'), ('Variant 1,3', '1_3', 'chest right behind kelp'),
                     ('Variant 1,4', '1_4', 'wall hole, barrels, button')],
    'p2/v2/bed': [('Variant 2,1,1', '2_1_1', 'seagrass in gravel'), ('Variant 2,1,1,1', '2_1_1_1', '+ suspicious gravel'),
                  ('Variant 2,1,2', '2_1_2', 'seagrass in sand'), ('Variant 2,1,2,1', '2_1_2_1', '+ suspicious sand'),
                  ('Variant 2,1,3', '2_1_3', 'loot chests under bed')],
    'p2/v3_1/barrel': [('Variant 3,1,1', '3_1_1', 'barrel behind kelp'), ('Variant 3,1,2', '3_1_2', 'barrel behind seagrass')],
}
for slot, entries in overlays.items():
    for label, name, what in entries:
        overlay(label, what, f'{M}/{slot}/{name}', INSIDE, 'minecraft:water')
    elements = [(single(f'{M}/{slot}/{name}'), 1) for _, name, _ in entries]
    if slot == 'p2/v2/bed':
        elements.append((EMPTY, 1))  # Plain variant 2, without a seagrass bed
    pool(f'{M}/{slot}', elements)

# Variant 3,2's side room, and the decorations that some of its jump layouts take
side_rooms = [
    ('Variant 3,2,1', '3_2_1', ['Kelp on the neo', 'Seagrass on the neo', 'Sea lantern checkerboard']),
    ('Variant 3,2,2', '3_2_2', ['Kelp on the platform', 'Seagrass on the platform', 'Sea lantern checkerboard']),
    ('Variant 3,2,3', '3_2_3', []),
    ('Variant 3,2,4', '3_2_4', ['Conduit on quartz', 'Stone brick fountain', 'Glass aquarium tank', 'Quartz column walkway']),
]
for label, name, decorations in side_rooms:
    slot = f'{NS}:{M}/p2/v3_2/{name}/decor' if decorations else None
    side_room(label, f'{M}/p2/v3_2/{name}', slot)
    for i, what in enumerate(decorations, 1):
        overlay(f'{label},{i}', what, f'{M}/p2/v3_2/{name}/{name}_{i}', SIDE_INSIDE, 'minecraft:lava', 'minecraft:orange_wool', False)
    if decorations:
        pool(f'{M}/p2/v3_2/{name}/decor',
             [(single(f'{M}/p2/v3_2/{name}/{name}_{i}', 'ignore_waterlogging'), 1) for i in range(1, len(decorations) + 1)])
pool(f'{M}/p2/v3_2/side_room', [(single(f'{M}/p2/v3_2/{name}', 'ignore_waterlogging'), 1) for _, name, _ in side_rooms])

# What follows P2. Only the end cap for now; point it at P3's pool once that exists
cap = Piece((ROOM[0], ROOM[1], 6))
cap.shell('minecraft:prismarine_bricks', 'minecraft:dark_prismarine', 'minecraft:prismarine', *WATER)
doorway(cap, 0, 'north', {'name': f'{NS}:route_in'})
cap.fill((1, 1, 4), (ROOM[0] - 2, ROOM[1] - 2, 4), 'minecraft:glass')
cap.sign((DOOR_X, 1, 2), ['End of P2', 'P3 goes here'])
cap.save(f'{M}/test/end_cap')
pool(f'{M}/after_p2', [(single(f'{M}/test/end_cap'), 1)])

# Test structure: a short corridor leading into a random P2 room. It isn't in any structure set, so it never generates on
# its own; place it with /place structure strawberrymc:monument_puzzles/p2_test
entry = Piece((ROOM[0], ROOM[1], 6))
entry.shell('minecraft:prismarine_bricks', 'minecraft:dark_prismarine', 'minecraft:prismarine', *WATER)
doorway(entry, 5, 'south', {'name': f'{NS}:route_out', 'target': f'{NS}:route_in', 'pool': f'{NS}:{M}/p2'})
entry.fill((1, 1, 1), (ROOM[0] - 2, ROOM[1] - 2, 1), 'minecraft:glass')
entry.sign((DOOR_X, 1, 3), ['P2 test', 'start corridor'])
entry.save(f'{M}/test/p2_entry')
pool(f'{M}/test/p2_start', [(single(f'{M}/test/p2_entry'), 1)])

w(f'{P}/data/{NS}/worldgen/structure/monument_puzzles/p2_test.json', {
    'type': 'minecraft:jigsaw',
    'biomes': '#minecraft:is_overworld',
    'step': 'underground_structures',
    'spawn_overrides': {},
    'terrain_adaptation': 'none',
    'start_pool': f'{NS}:{M}/test/p2_start',
    'size': 6,
    'start_height': {'absolute': 0},
    'use_expansion_hack': False,
    'max_distance_from_center': 64,
    'liquid_settings': 'apply_waterlogging'})
