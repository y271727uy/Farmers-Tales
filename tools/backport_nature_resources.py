"""Build the small 1.21.5 plant backport using the locally downloaded client JAR."""
import json
from pathlib import Path
import zipfile

from official_backport_lang import apply_official_chinese

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
NAMES = ("short_dry_grass", "tall_dry_grass", "bush")


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main():
    with zipfile.ZipFile(ROOT / "refer/minecraft-26.3-client.jar") as jar:
        for name in NAMES:
            block = f"farmerstales:{name}"
            texture = RES / f"assets/farmerstales/textures/block/{name}.png"
            texture.parent.mkdir(parents=True, exist_ok=True)
            texture.write_bytes(jar.read(f"assets/minecraft/textures/block/{name}.png"))
            write(RES / f"assets/farmerstales/blockstates/{name}.json",
                  {"variants": {"": {"model": f"farmerstales:block/{name}"}}})
            # 1.20.1 has no tinted_cross parent: supply the tint index explicitly.
            model = {"parent": "minecraft:block/cross", "textures": {"cross": f"farmerstales:block/{name}"}}
            if name == "bush":
                model = json.loads(jar.read("assets/minecraft/models/block/cross.json"))
                model["textures"] = {"cross": f"farmerstales:block/{name}", "particle": "#cross"}
                for element in model["elements"]:
                    element.pop("shade_direction_override", None)
                    for face in element["faces"].values():
                        face["tintindex"] = 0
            write(RES / f"assets/farmerstales/models/block/{name}.json", model)
            write(RES / f"assets/farmerstales/models/item/{name}.json", {
                "parent": "minecraft:item/generated", "textures": {"layer0": f"farmerstales:block/{name}"}})
            write(RES / f"data/farmerstales/loot_tables/blocks/{name}.json", {
                "type": "minecraft:block", "pools": [{"rolls": 1, "conditions": [{
                    "condition": "minecraft:any_of", "terms": [
                        {"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}},
                        {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{
                            "enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}]}],
                    "entries": [{"type": "minecraft:item", "name": block}]}]})

        english = json.loads(jar.read("assets/minecraft/lang/en_us.json"))
        path = ROOT / "src/generated/resources/assets/farmerstales/lang/en_us.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data.update({f"block.farmerstales.{name}": english[f"block.minecraft.{name}"] for name in NAMES})
        write(path, data)
        apply_official_chinese(NAMES)

    for feature, names, biomes, tries, rarity in (
        ("dry_grass", NAMES[:2], ["desert", "badlands", "eroded_badlands", "wooded_badlands"], 32, 2),
        ("bush", NAMES[2:], ["birch_forest", "forest", "frozen_river", "old_growth_birch_forest", "plains",
                             "river", "windswept_forest", "windswept_gravelly_hills", "windswept_hills"], 16, 4),
    ):
        states = [{"data": {"Name": f"farmerstales:{name}"}, "weight": 1} for name in names]
        provider = {"type": "minecraft:weighted_state_provider", "entries": states}
        write(RES / f"data/farmerstales/worldgen/configured_feature/patch_{feature}.json", {
            "type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": 7, "y_spread": 3,
                "feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": provider}},
                            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                                "type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]}}]}}})
        write(RES / f"data/farmerstales/worldgen/placed_feature/patch_{feature}.json", {
            "feature": f"farmerstales:patch_{feature}", "placement": [
                {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
        write(RES / f"data/farmerstales/forge/biome_modifier/add_{feature}.json", {
            "type": "forge:add_features", "biomes": [f"minecraft:{biome}" for biome in biomes],
            "features": [f"farmerstales:patch_{feature}"], "step": "vegetal_decoration"})
    path = RES / "data/minecraft/tags/blocks/replaceable_by_trees.json"
    data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    data["values"] = list(dict.fromkeys(data["values"] + [f"farmerstales:{name}" for name in NAMES]))
    write(path, data)
    print("Generated 3 plants, textures, loot and 2 biome features for 1.20.1.")


if __name__ == "__main__":
    main()
