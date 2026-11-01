"""Convert the locally downloaded 26.3 forest assets into Forge 1.20.1 resources.

The old client-extra JAR supplies compatible vanilla worldgen/loot templates.
No downloads; existing language/tag entries are preserved.
"""
import copy
import gzip
import json
import struct
from pathlib import Path
import zipfile

from official_backport_lang import apply_official_chinese
from backport_leaf_particles import generate as generate_leaf_particles

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
MOD = "farmerstales"
NAMES = [f"{prefix}{tree}_{part}" for tree in ("pale_oak", "poplar")
         for prefix, part in (("", "log"), ("stripped_", "log"), ("", "wood"), ("stripped_", "wood"), ("", "planks"), ("", "sapling"))]
NAMES += ["pale_oak_leaves", "red_poplar_leaves", "orange_poplar_leaves", "yellow_poplar_leaves",
          "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss", "closed_eyeblossom", "open_eyeblossom",
          "red_shrub", "shelf_mushroom", "leaf_litter"]


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def remap(value, replacements):
    if isinstance(value, dict):
        return {replacements.get(k, k): remap(v, replacements) for k, v in value.items()}
    if isinstance(value, list):
        return [remap(v, replacements) for v in value]
    return replacements.get(value, value) if isinstance(value, str) else value


def tag(namespace, kind, name, values):
    path = RES / f"data/{namespace}/tags/{kind}/{name}.json"
    current = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    current["values"] = list(dict.fromkeys(current["values"] + values))
    write(path, current)


def state(name, properties=None):
    result = {"Name": f"{MOD}:{name}"}
    if properties:
        result["Properties"] = properties
    return result


def provider(name, properties=None):
    return {"type": "minecraft:simple_state_provider", "state": state(name, properties)}


def feature(name, feature_type, config):
    write(RES / f"data/{MOD}/worldgen/configured_feature/{name}.json", {"type": feature_type, "config": config})


def placed(name, configured, count=1, rarity=None):
    placements = [{"type": "minecraft:in_square"}, {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    placements.insert(0, {"type": "minecraft:rarity_filter", "chance": rarity} if rarity else {"type": "minecraft:count", "count": count})
    write(RES / f"data/{MOD}/worldgen/placed_feature/{name}.json", {"feature": f"{MOD}:{configured}", "placement": placements})


def patch(name, names, tries=24):
    entries = [{"data": state(n), "weight": 1} for n in names]
    feature(name, "minecraft:random_patch", {"tries": tries, "xz_spread": 6, "y_spread": 3,
        "feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": {
            "type": "minecraft:weighted_state_provider", "entries": entries}}}, "placement": [{
                "type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]}}]}})
    placed(name, name)


def recipe(name, data, material):
    write(RES / f"data/{MOD}/recipes/{name}.json", data)
    predicate = {"tag": material[1:]} if material.startswith("#") else {"items": [material]}
    write(RES / f"data/{MOD}/advancements/recipes/forest/{name}.json", {
        "parent": "minecraft:recipes/root", "criteria": {
            "has_material": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [predicate]}},
            "has_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"{MOD}:{name}"}}},
        "requirements": [["has_material", "has_recipe"]], "rewards": {"recipes": [f"{MOD}:{name}"]}})


def main():
    generate_leaf_particles()
    # A roomy empty GameTest template prevents tree crowns overlapping other tests.
    def nbt_name(name):
        raw = name.encode("utf-8")
        return struct.pack(">H", len(raw)) + raw
    def nbt_tag(kind, name, payload):
        return bytes([kind]) + nbt_name(name) + payload
    template = b"\x0a\x00\x00" + nbt_tag(3, "DataVersion", struct.pack(">i", 3465))
    template += nbt_tag(9, "size", b"\x03" + struct.pack(">iiii", 3, 32, 24, 32))
    template += nbt_tag(9, "palette", b"\x0a" + struct.pack(">i", 1) + nbt_tag(8, "Name", nbt_name("minecraft:air")) + b"\x00")
    for name in ("blocks", "entities"):
        template += nbt_tag(9, name, b"\x0a" + struct.pack(">i", 0))
    target = RES / f"data/{MOD}/structures/forest_empty.nbt"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(gzip.compress(template + b"\x00", mtime=0))
    with zipfile.ZipFile(ROOT / "refer/minecraft-26.3-client.jar") as new, zipfile.ZipFile(
            ROOT / "build/moddev/artifacts/client-extra-1.20.1-47.4.0.jar") as old:
        old_files = set(old.namelist())
        copied = set()

        def texture(reference):
            if not reference.startswith("#") and ":" not in reference:
                reference = "minecraft:" + reference
            if not reference.startswith("minecraft:"):
                return reference
            relative = reference.removeprefix("minecraft:")
            source = f"assets/minecraft/textures/{relative}.png"
            if source in old_files:
                return reference
            target = RES / f"assets/{MOD}/textures/{relative}.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(new.read(source))
            metadata = source + ".mcmeta"
            if metadata in new.namelist():
                Path(str(target) + ".mcmeta").write_bytes(new.read(metadata))
            return f"{MOD}:{relative}"

        def model(reference, force=False):
            if ":" not in reference:
                reference = "minecraft:" + reference
            if not reference.startswith("minecraft:"):
                return reference
            relative = reference.removeprefix("minecraft:")
            source = f"assets/minecraft/models/{relative}.json"
            if not force and source in old_files:
                return reference
            destination = f"{MOD}:{relative}"
            if destination in copied:
                return destination
            copied.add(destination)
            data = json.loads(new.read(source))
            if "parent" in data:
                data["parent"] = model(data["parent"])
            if "textures" in data:
                data["textures"] = {key: texture(value) for key, value in data["textures"].items()}
            for element in data.get("elements", []):
                element.pop("shade_direction_override", None)
                emission = element.pop("light_emission", 0)
                if emission:
                    element["forge_data"] = {"block_light": emission, "sky_light": emission, "ambient_occlusion": False}
            if relative.startswith("block/") and any(word in relative for word in ("leaves", "sapling", "moss", "eyeblossom", "shrub", "mushroom", "litter")):
                data["render_type"] = "minecraft:cutout"
            write(RES / f"assets/{MOD}/models/{relative}.json", data)
            return destination

        def blockstate_models(value):
            if isinstance(value, dict):
                return {key: model(item, True) if key == "model" else blockstate_models(item) for key, item in value.items()}
            if isinstance(value, list):
                return [blockstate_models(item) for item in value]
            return value

        for name in NAMES:
            if name == "pale_moss_carpet":
                data = {"variants": {"": {"model": model("minecraft:block/pale_moss_carpet", True)}}}
            else:
                data = json.loads(new.read(f"assets/minecraft/blockstates/{name}.json"))
                data = blockstate_models(remap(data, {"segment_amount": "flower_amount"}))
            write(RES / f"assets/{MOD}/blockstates/{name}.json", data)
            item_source = f"assets/minecraft/models/item/{name}.json"
            if item_source in new.namelist():
                model(f"minecraft:item/{name}", True)
            elif name == "shelf_mushroom":
                write(RES / f"assets/{MOD}/models/item/{name}.json", {"parent": model("minecraft:block/shelf_mushroom_stage0", True)})
            else:
                write(RES / f"assets/{MOD}/models/item/{name}.json", {"parent": model(f"minecraft:block/{name}", True)})

            if name.endswith("leaves"):
                tree = "pale_oak" if name.startswith("pale_oak") else "poplar"
                loot = json.loads(old.read("data/minecraft/loot_tables/blocks/oak_leaves.json"))
                loot["pools"] = [pool for pool in loot["pools"] if "minecraft:apple" not in json.dumps(pool)]
                loot = remap(loot, {"minecraft:oak_leaves": f"{MOD}:{name}", "minecraft:oak_sapling": f"{MOD}:{tree}_sapling"})
            elif name == "leaf_litter":
                loot = remap(json.loads(old.read("data/minecraft/loot_tables/blocks/pink_petals.json")), {"minecraft:pink_petals": f"{MOD}:leaf_litter"})
            else:
                pool = {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                        "conditions": [{"condition": "minecraft:survives_explosion"}]}
                if name == "pale_hanging_moss":
                    pool["conditions"].append({"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}})
                if name == "shelf_mushroom":
                    pool["entries"][0]["functions"] = [{"function": "minecraft:set_count", "count": 2, "conditions": [{
                        "condition": "minecraft:block_state_property", "block": f"{MOD}:{name}", "properties": {"age": "1"}}]}]
                loot = {"type": "minecraft:block", "pools": [pool]}
            write(RES / f"data/{MOD}/loot_tables/blocks/{name}.json", loot)

        for tree in ("pale_oak", "poplar"):
            logs = [f"{MOD}:{prefix}{tree}_{part}" for prefix, part in (("", "log"), ("stripped_", "log"), ("", "wood"), ("stripped_", "wood"))]
            for kind in ("blocks", "items"):
                tag(MOD, kind, tree + "_logs", logs)
                tag("minecraft", kind, "logs_that_burn", [f"#{MOD}:{tree}_logs"])
                tag("minecraft", kind, "planks", [f"{MOD}:{tree}_planks"])
                tag("minecraft", kind, "saplings", [f"{MOD}:{tree}_sapling"])
            tag("minecraft", "blocks", "mineable/axe", logs + [f"{MOD}:{tree}_planks"])
            recipe(tree + "_planks", {"type": "minecraft:crafting_shapeless", "category": "building", "group": "planks",
                "ingredients": [{"tag": f"{MOD}:{tree}_logs"}], "result": {"item": f"{MOD}:{tree}_planks", "count": 4}}, f"#{MOD}:{tree}_logs")
            for prefix in ("", "stripped_"):
                log = f"{MOD}:{prefix}{tree}_log"
                recipe(prefix + tree + "_wood", {"type": "minecraft:crafting_shaped", "category": "building",
                    "pattern": ["##", "##"], "key": {"#": {"item": log}},
                    "result": {"item": f"{MOD}:{prefix}{tree}_wood", "count": 3}}, log)

        leaves = [f"{MOD}:{n}" for n in NAMES if n.endswith("leaves")]
        for kind in ("blocks", "items"):
            tag("minecraft", kind, "leaves", leaves)
        tag("minecraft", "blocks", "mineable/hoe", leaves + [f"{MOD}:pale_moss_block", f"{MOD}:pale_moss_carpet"])
        tag("minecraft", "blocks", "dirt", [f"{MOD}:pale_moss_block"])
        tag("minecraft", "blocks", "replaceable_by_trees", [f"{MOD}:{n}" for n in NAMES if any(s in n for s in ("litter", "shrub", "moss_carpet", "eyeblossom"))])
        tag("minecraft", "blocks", "small_flowers", [f"{MOD}:closed_eyeblossom", f"{MOD}:open_eyeblossom"])
        tag("minecraft", "items", "small_flowers", [f"{MOD}:closed_eyeblossom", f"{MOD}:open_eyeblossom"])
        tag(MOD, "items", "stew_mushrooms", ["minecraft:brown_mushroom", "minecraft:red_mushroom", f"{MOD}:shelf_mushroom"])
        recipe("mushroom_stew_from_forest_mushrooms", {"type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"tag": f"{MOD}:stew_mushrooms"}, {"tag": f"{MOD}:stew_mushrooms"}, {"item": "minecraft:bowl"}],
            "result": {"item": "minecraft:mushroom_stew"}}, f"{MOD}:shelf_mushroom")
        recipe("leaf_litter_from_smelting", {"type": "minecraft:smelting", "category": "blocks", "cookingtime": 200,
            "experience": 0.1, "ingredient": {"tag": "minecraft:leaves"}, "result": f"{MOD}:leaf_litter"}, "#minecraft:leaves")
        for name, dye in (("closed_eyeblossom", "gray_dye"), ("open_eyeblossom", "orange_dye")):
            recipe(name + "_dye", {"type": "minecraft:crafting_shapeless", "category": "misc",
                "ingredients": [{"item": f"{MOD}:{name}"}], "result": {"item": f"minecraft:{dye}"}}, f"{MOD}:{name}")
        recipe("pale_moss_carpet", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["##"],
            "key": {"#": {"item": f"{MOD}:pale_moss_block"}}, "result": {"item": f"{MOD}:pale_moss_carpet", "count": 3}}, f"{MOD}:pale_moss_block")

        # Retain all 1.20.1 cave, lake and ore stages; only replace the forest vegetation.
        for name in ("pale_garden", "dappled_forest"):
            biome = json.loads(old.read("data/minecraft/worldgen/biome/forest.json"))
            modern = json.loads(new.read(f"data/minecraft/worldgen/biome/{name}.json"))
            attributes = modern["attributes"]
            effects = biome["effects"]
            for field in ("fog_color", "sky_color", "water_fog_color"):
                effects[field] = int(attributes[f"minecraft:visual/{field}"][1:], 16)
            for field in ("water_color", "grass_color", "foliage_color"):
                effects[field] = int(modern["effects"][field][1:], 16)
            biome["temperature"] = modern["temperature"]
            biome["downfall"] = modern["downfall"]
            shared = ["minecraft:glow_lichen", f"{MOD}:trees_{name}"]
            if name == "pale_garden":
                biome["spawners"]["creature"] = []
                effects.pop("music", None)
                shared += [f"{MOD}:pale_moss_patch", f"{MOD}:patch_eyeblossom"]
            else:
                biome["spawners"]["creature"] = [spawn for spawn in biome["spawners"]["creature"]
                                                if spawn["type"] != "minecraft:wolf"]
                biome["spawners"]["creature"] += [
                    {"type": "minecraft:rabbit", "weight": 4, "minCount": 2, "maxCount": 4},
                    {"type": "minecraft:fox", "weight": 4, "minCount": 2, "maxCount": 4}]
                shared += [f"{MOD}:fallen_poplar", f"{MOD}:patch_red_shrub"]
            shared += ["minecraft:patch_grass_forest", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
                       "minecraft:patch_sugar_cane", "minecraft:patch_pumpkin"]
            biome["features"][9] = shared
            write(RES / f"data/{MOD}/worldgen/biome/{name}.json", biome)

        for name in ("pale_oak", "red_poplar", "orange_poplar", "yellow_poplar"):
            tree = json.loads(old.read("data/minecraft/worldgen/configured_feature/dark_oak.json"))["config"]
            pale = name == "pale_oak"
            tree["trunk_provider"] = provider("pale_oak_log" if pale else "poplar_log", {"axis": "y"})
            tree["foliage_provider"] = provider(name + "_leaves", {"distance": "7", "persistent": "false", "waterlogged": "false"})
            if not pale:
                tree["trunk_placer"] = {"type": f"{MOD}:poplar_trunk_placer", "base_height": 7, "height_rand_a": 4, "height_rand_b": 0}
                tree["foliage_placer"] = {"type": f"{MOD}:poplar_foliage_placer", "radius": 4, "offset": 0, "height": 6}
                tree["minimum_size"] = {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 2}
            # Player-grown trees remain renewable without spawning ground decoration.
            feature(name + "_bonemeal", "minecraft:tree", copy.deepcopy(tree))
            tree["decorators"] = [{"type": f"{MOD}:forest_decoration", "pale": pale}]
            feature(name, "minecraft:tree", tree)
        feature("trees_pale_garden", "minecraft:random_selector", {"features": [], "default": {
            "feature": f"{MOD}:pale_oak", "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                "type": "minecraft:would_survive", "state": state("pale_oak_sapling")}}]}})
        feature("trees_dappled_forest", "minecraft:random_selector", {
            "features": [{"chance": chance, "feature": {"feature": f"{MOD}:{color}_poplar", "placement": []}}
                         for color, chance in (("red", 1 / 3), ("orange", 0.5))],
            "default": {"feature": f"{MOD}:yellow_poplar", "placement": []}})
        placed("trees_pale_garden", "trees_pale_garden", count=8)
        placed("trees_dappled_forest", "trees_dappled_forest", count=6)
        feature("fallen_poplar", f"{MOD}:fallen_poplar", {})
        placed("fallen_poplar", "fallen_poplar", rarity=5)
        patch("patch_eyeblossom", ["closed_eyeblossom"], tries=16)
        patch("patch_red_shrub", ["red_shrub"], tries=24)
        feature("pale_moss_vegetation", "minecraft:simple_block", {"to_place": provider("pale_moss_carpet")})
        moss = json.loads(old.read("data/minecraft/worldgen/configured_feature/moss_patch_bonemeal.json"))["config"]
        moss["ground_state"] = provider("pale_moss_block")
        moss["vegetation_feature"]["feature"] = f"{MOD}:pale_moss_vegetation"
        feature("pale_moss_patch", "minecraft:vegetation_patch", moss)
        placed("pale_moss_patch", "pale_moss_patch", count=2)

        for tag_name in ("is_overworld", "is_forest"):
            tag("minecraft", "worldgen/biome", tag_name, [f"{MOD}:pale_garden", f"{MOD}:dappled_forest"])
        tag(MOD, "worldgen/biome", "spawns_cold_variant_farm_animals", [f"{MOD}:dappled_forest"])

        english = json.loads(new.read("assets/minecraft/lang/en_us.json"))
        path = ROOT / "src/generated/resources/assets/farmerstales/lang/en_us.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        for name in NAMES:
            data[f"block.{MOD}.{name}"] = english[f"block.minecraft.{name}"]
        for name in ("pale_garden", "dappled_forest"):
            data[f"biome.{MOD}.{name}"] = english[f"biome.minecraft.{name}"]
        write(path, data)
        apply_official_chinese(NAMES, ("pale_garden", "dappled_forest"))
    print(f"Generated {len(NAMES)} forest blocks, two biomes and renewable tree/plant resources.")


if __name__ == "__main__":
    main()
