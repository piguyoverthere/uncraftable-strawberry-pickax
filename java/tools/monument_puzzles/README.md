# Ocean Monument puzzle jigsaws

The puzzles from the wiki's "Puzzle jigsaws" section, built as jigsaw pieces in the `monument_puzzles` built-in datapack
(`src/main/resources/monument_puzzles`), which MCreator never touches. Only **P2 (piston puzzle)** is set up so far.

- `gen.py` writes the template pools, the `p2_test` structure, and a labelled placeholder for every piece that doesn't exist yet.
- `import.py` copies pieces you've built and saved in game into the pack.

## Building a piece

1. In a creative world (in the dev client), place a **structure block** in **Load** mode, enter the piece's name
   (for example `strawberrymc:monument/p2/v1`) and press Load to get the placeholder.
2. Build the room over it. Keep these the same, or pieces won't line up:
   - the piece's size (see the table below);
   - the **doorway jigsaws** and their 3x3 openings in the middle of the north (in) and south (out) walls;
   - the **pipe** through each doorway wall: walking along the route, it's 4 blocks to the left of the doorway's jigsaw and 7 up;
   - the **slot jigsaw** in the floor corner (1, 0, 1) and the **side door** jigsaw, where a room has them.
3. Switch the structure block to **Save** with the same name and size and press Save. Turn on "Include entities" for the
   drowned, skeletons and guardians, and give them `PersistenceRequired:1b` so they don't despawn.
4. From the `java` folder, copy what you saved into the pack:

   ```
   python3 tools/monument_puzzles/import.py run/saves/<your world>
   ```

While you're in that world, the copy saved in the world is used instead of the pack's copy, so you can keep loading
and saving without importing each time.

**Overlays** (hiding places, seagrass beds, decorations) only need the blocks they change. Fill everything else with
**structure void**, which leaves the room's own blocks alone; air would erase them. Each overlay's own jigsaw sits in its
bottom corner, and its "Turns into" block replaces that one corner block.

## Testing

```
/place structure strawberrymc:monument_puzzles/p2_test ~ ~ ~
```

This builds a short corridor, a random P2 room with its random sub-variants, and an end cap. It's never generated on
its own. Each placeholder room has a coloured concrete marker under its sign: white v1, yellow v2, blue v3, cyan v3,1,
purple v3,2.

## P2 pieces

All names start with `strawberrymc:monument/p2/`. Sizes are width x height x depth.

| Piece | Size | What it is | Picked from |
|---|---|---|---|
| `v1`, `v2`, `v3`, `v3_1`, `v3_2` | 16x12x16 | The rooms (3-gap, 2-gap, 3+2 gaps, two 2-gaps, side room) | `p2`: v1 and v2 a third each, v3 / v3,1 / v3,2 a ninth each |
| `v1/hiding/1` … `1_4` | 14x10x14 overlay | Hidden chest: crouch slab, trapdoor, left kelp, right kelp, wall hole + button | v1's slot |
| `v2/bed/2_1_1` … `2_1_3` | 14x10x14 overlay | Seagrass bed: gravel, + suspicious, sand, + suspicious, loot chests | v2's slot, or nothing (plain v2) |
| `v3_1/barrel/3_1_1`, `3_1_2` | 14x10x14 overlay | The barrel behind kelp or seagrass | v3,1's slot |
| `v3_2/3_2_1` … `3_2_4` | 12x12x12, dry | The pickaxe side room: 3-jump + neo, two 3-jumps, 4-jump, two neos | v3,2's side door |
| `v3_2/3_2_1/3_2_1_1` … `_3` | 10x10x10 overlay | Kelp, seagrass or sea-lantern neo | 3,2,1's slot |
| `v3_2/3_2_2/3_2_2_1` … `_3` | 10x10x10 overlay | Kelp, seagrass or sea-lantern platform | 3,2,2's slot |
| `v3_2/3_2_4/3_2_4_1` … `_4` | 10x10x10 overlay | Conduit, fountain, aquarium tank, quartz column | 3,2,4's slot |

The side rooms use `ignore_waterlogging`, so blocks in them stay dry behind the blockade in their doorway.

## Adding P3 and later puzzles

Give the new puzzle's rooms the same doorway jigsaws (`strawberrymc:route_in` on the way in; `strawberrymc:route_out`
targeting `strawberrymc:route_in` on the way out), then point the `strawberrymc:monument/after_p2` pool at the new
puzzle's pool in `gen.py`, in place of the end cap.
