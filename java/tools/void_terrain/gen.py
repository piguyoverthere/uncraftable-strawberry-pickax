# Generates the Void's terrain into the built-in void_terrain datapack (src/main/resources/void_terrain), which MCreator never
# regenerates. Run it with `python3 tools/void_terrain/gen.py` from the java folder after changing anything here.
# It reads MCreator's own dimension/void_dim.json for the base settings, so changes made to the VoidDim element carry over.
import json, os, copy
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources')
P = R + '/void_terrain'
def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'w').write(json.dumps(obj, indent=2) + '\n')

w(P + '/pack.mcmeta', {"pack": {"min_format": 101.1, "max_format": 101.1,
   "description": "Strawberry MC Void terrain (overrides MCreator's generated void_dim)"}})

# Noises: wide ones shape the land, cracks and island placement; small ones roughen surfaces
noises = {
  'void_ground':      {"firstOctave": -7, "amplitudes": [1, 1, 0.5]},
  'void_underside':   {"firstOctave": -6, "amplitudes": [1, 0.5]},
  'void_cracks':      {"firstOctave": -7, "amplitudes": [1, 0.5]},
  'void_crack_width': {"firstOctave": -7, "amplitudes": [1]},
  'void_roughness':   {"firstOctave": -4, "amplitudes": [1, 0.5]},
  'void_islands':     {"firstOctave": -6, "amplitudes": [1, 1, 0.5]},
  'void_pillars':     {"firstOctave": -5, "amplitudes": [1]},
  'void_bridges_a':   {"firstOctave": -6, "amplitudes": [1, 0.5]},
  'void_bridges_b':   {"firstOctave": -6, "amplitudes": [1, 0.5]},
  'void_peaks':       {"firstOctave": -5, "amplitudes": [1, 0.5]},
  'void_islands_mid': {"firstOctave": -6, "amplitudes": [1, 1, 0.5]},
  'void_islands_low': {"firstOctave": -6, "amplitudes": [1, 1, 0.5]},
  'void_canyons_a':   {"firstOctave": -6, "amplitudes": [1, 0.5]},
  'void_canyons_b':   {"firstOctave": -6, "amplitudes": [1, 0.5]},
  'void_tunnels_a':   {"firstOctave": -5, "amplitudes": [1]},
  'void_tunnels_b':   {"firstOctave": -5, "amplitudes": [1]},
  'void_spires':      {"firstOctave": -4, "amplitudes": [1, 0.5]},
  'void_crags':       {"firstOctave": -4, "amplitudes": [1, 1, 0.5]},
}
for k, v in noises.items(): w(f'{P}/data/strawberrymc/worldgen/noise/{k}.json', v)

def noise(n, xz=1, y=1): return {"type": "minecraft:noise", "noise": f"strawberrymc:{n}", "xz_scale": xz, "y_scale": y}
def add(a, b): return {"type": "minecraft:add", "argument1": a, "argument2": b}
def mul(a, b): return {"type": "minecraft:mul", "argument1": a, "argument2": b}
def mn(a, b): return {"type": "minecraft:min", "argument1": a, "argument2": b}
def mx(a, b): return {"type": "minecraft:max", "argument1": a, "argument2": b}
def ab(a): return {"type": "minecraft:abs", "argument": a}
def grad(y0, v0, y1, v1): return {"type": "minecraft:y_clamped_gradient", "from_y": y0, "to_y": y1, "from_value": v0, "to_value": v1}
def sq(a): return mul(a, a)
def clamp(a, lo, hi): return {"type": "minecraft:clamp", "input": a, "min": lo, "max": hi}
def df(name): return f"strawberrymc:void/{name}"

# Each piece is a named density function so it can be tweaked on its own later
fns = {}
# Fractured plain: an uneven plane around y=64 (roughly 50-80)
fns['ground'] = add(add(grad(34, 1, 94, -1), mul(0.35, noise('void_ground', 1, 0))), mul(0.08, noise('void_roughness')))
# Cracks: where the crack noise is near zero, positive elsewhere; the width noise makes some cracks wide and some narrow
fns['cracks'] = mul(12, add(ab(noise('void_cracks', 1, 0)), mul(-1, add(0.11, mul(0.06, noise('void_crack_width', 1, 0))))))
# The plain is a slab with a ragged underside around y=-5 and nothing below it, so the cracks open into the void
fns['underside'] = add(grad(-25, -1, 15, 1), mul(0.4, noise('void_underside', 1, 0)))
# Biome climate points (temperature, humidity). Voidstone Peaks is a range, so it takes up a large share of the Void
ABYSSAL = (-0.5, 0)
ECHOING = (0.4, -0.5)
PEAKS_T, PEAKS_H = (-0.1, 1), (-0.15, 1)
# The multi_noise biome source picks the biome whose point is nearest, so the mask is how much nearer Voidstone Peaks is than
# the closest other biome: 1 inside the biome, fading to 0 at its edges, so the mountains only rise there
climate = lambda n: {"type": "minecraft:flat_cache", "argument": {"type": "minecraft:shifted_noise", "noise": f"minecraft:{n}", "xz_scale": 0.25, "y_scale": 0, "shift_x": "minecraft:shift_x", "shift_y": 0, "shift_z": "minecraft:shift_z"}}
T, H = climate('temperature'), climate('vegetation')
dist_point = lambda p: add(sq(add(T, -p[0])), sq(add(H, -p[1])))
outside = lambda v, lo: mx(0, add(lo, mul(-1, v)))  # how far v is below the range (the ranges reach past the noise's top)
dist_peaks = add(sq(outside(T, PEAKS_T[0])), sq(outside(H, PEAKS_H[0])))
fns['peaks_mask'] = {"type": "minecraft:flat_cache", "argument": clamp(mul(10, add(mn(dist_point(ABYSSAL), dist_point(ECHOING)), mul(-1, dist_peaks))), 0, 1)}
# Mountain height (1 = 30 blocks above y=34): sharp ridges from a squared ridged noise, with smaller spikes on top
ridge = lambda n: add(1, mul(-1, ab(noise(n, 1, 0))))
fns['peaks_height'] = {"type": "minecraft:flat_cache", "argument": mul(df('peaks_mask'), add(1, add(mul(6, sq(ridge('void_peaks'))), mul(2.5, sq(ridge('void_spires'))))))}
# Mountains: the height, carved by vertically stretched noise into spires and fins, plus crags and overhangs
fns['mountains'] = add(add(grad(34, 0, 304, -9), add(df('peaks_height'), -1)),
   mul(df('peaks_mask'), add(mul(0.9, noise('void_spires', 1, 0.2)), mul(0.4, noise('void_crags', 1, 1)))))
# A maze through the mountains: winding canyons down to the plain where either canyon noise crosses zero, and tunnels
# through the rock where two 3D noises both do. They only cut the mountains, so the plain under them stays
fns['canyons'] = mul(14, add(mn(ab(noise('void_canyons_a', 1, 0)), ab(noise('void_canyons_b', 1, 0))), -0.035))
fns['tunnels'] = mul(10, add(mx(ab(noise('void_tunnels_a', 1, 0.7)), ab(noise('void_tunnels_b', 1, 0.7))), -0.07))
fns['fractured_plain'] = mn(mn(mx(df('ground'), mn(df('mountains'), mn(df('canyons'), df('tunnels')))), df('underside')), df('cracks'))
# Floating islands: flat-topped, tapering downward into cones where the island noise is weaker. Three layers: the main one
# (tops around y=166), a lower one between it and the plain (around y=112), and one in the void under the plain (around y=-30)
island_layer = lambda n, bottom, top, ceiling: add(mn(grad(bottom, -1.2, top, 0), grad(top, 0, ceiling, -1.2)), add(add(mul(1.4, noise(n, 1, 0)), -0.55), mul(0.1, noise('void_roughness'))))
fns['islands'] = mx(mx(island_layer('void_islands', 130, 166, 180), island_layer('void_islands_mid', 88, 112, 122)), island_layer('void_islands_low', -60, -30, -20))
# Pillars: thin columns rising from the plain up into the island layer (cut off under the plain so they don't hang into the void)
fns['pillars'] = add(add(add(mul(4, noise('void_pillars', 1, 0)), -3.4), add(add(grad(40, 1, 60, 0), grad(160, 0, 175, -1)), grad(-20, -3, 10, 0))),
   # Many more, thicker and craggier pillars in Voidstone Peaks, a forest of them to get lost in
   mul(df('peaks_mask'), add(1.4, mul(0.6, noise('void_crags', 1, 1)))))
# Bridges: thin winding tubes where two 3D noises both cross zero, kept to the island layers
fns['bridges'] = add(add(0.1, mul(-1, mx(ab(noise('void_bridges_a', 1, 0.5)), ab(noise('void_bridges_b', 1, 0.5))))), mul(0.2, mx(mx(mn(grad(140, -1, 155, 0), grad(172, 0, 185, -1)), mn(grad(95, -1, 104, 0), grad(116, 0, 125, -1))), mn(grad(-55, -1, -45, 0), grad(-34, 0, -24, -1)))))
fns['final'] = {"type": "minecraft:interpolated", "argument":
   mx(mx(df('fractured_plain'), df('islands')), mx(df('pillars'), df('bridges')))}
for k, v in fns.items(): w(f'{P}/data/strawberrymc/worldgen/density_function/void/{k}.json', v)

dim = json.load(open(R + '/data/strawberrymc/dimension/void_dim.json'))
settings = dim['generator']['settings']
zero = 0
router = settings['noise_router']
for k in ('barrier', 'fluid_level_floodedness', 'fluid_level_spread', 'lava', 'continents', 'erosion', 'depth', 'ridges', 'vein_toggle', 'vein_ridged', 'vein_gap'):
    router[k] = zero
router['preliminary_surface_level'] = 64
router['final_density'] = df('final')
settings['aquifers_enabled'] = False
# No sea: Void Tears only appear as pools (worldgen/placed_feature/void_tears_pool.json)
settings['sea_level'] = -64
settings['ore_veins_enabled'] = False
settings['noise'] = {"min_y": -64, "height": 384, "size_horizontal": 1, "size_vertical": 1}
# Same surface for all three biomes for now: voidstone everywhere (the default block). No bedrock floor, the void is open below
biomes = ['strawberrymc:voidstone_peaks', 'strawberrymc:abyssal_plains', 'strawberrymc:echoing_deep']
settings['surface_rule'] = {"type": "minecraft:sequence", "sequence": [
  {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": biomes},
   "then_run": {"type": "minecraft:condition",
     "if_true": {"type": "minecraft:stone_depth", "surface_type": "floor", "add_surface_depth": False, "secondary_depth_range": 0, "offset": 0},
     "then_run": {"type": "minecraft:block", "result_state": {"Name": "strawberrymc:voidstone"}}}}]}
w(f'{P}/data/strawberrymc/worldgen/noise_settings/void_dim.json', settings)

def point(t, h): return {"temperature": t, "humidity": h, "continentalness": 0, "erosion": 0, "weirdness": 0, "depth": 0, "offset": 0}
dim['generator']['settings'] = 'strawberrymc:void_dim'
dim['generator']['biome_source'] = {"type": "minecraft:multi_noise", "biomes": [
  {"biome": 'strawberrymc:abyssal_plains', "parameters": point(*ABYSSAL)},
  {"biome": 'strawberrymc:echoing_deep', "parameters": point(*ECHOING)},
  {"biome": 'strawberrymc:voidstone_peaks', "parameters": point(list(PEAKS_T), list(PEAKS_H))}]}
w(f'{P}/data/strawberrymc/dimension/void_dim.json', dim)

# The two new biomes start as copies of Voidstone Peaks. Existing ones are left alone, so they can be customized
peaks = json.load(open(R + '/data/strawberrymc/worldgen/biome/voidstone_peaks.json'))
for b in ('abyssal_plains', 'echoing_deep'):
    if not os.path.exists(f'{R}/data/strawberrymc/worldgen/biome/{b}.json'):
        w(f'{R}/data/strawberrymc/worldgen/biome/{b}.json', peaks)
print('written')
