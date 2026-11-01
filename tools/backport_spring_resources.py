"""Build the next three Spring to Life plants from verified local Mojang assets."""
import copy
import hashlib
import json
import shutil
import zipfile
from pathlib import Path
from backport_forest_resources import ROOT, RES, write, remap, tag, feature, placed, provider, recipe
from official_backport_lang import apply_official_chinese

NAMES = ("firefly_bush", "wildflowers", "cactus_flower")


def main():
    with zipfile.ZipFile(ROOT / "refer/minecraft-26.3-client.jar") as jar, zipfile.ZipFile(
            ROOT / "build/moddev/artifacts/client-extra-1.20.1-47.4.0.jar") as old:
        old_files = set(old.namelist())
        copied = set()

        def texture(ref):
            if ref.startswith("#"):
                return ref
            relative = ref.removeprefix("minecraft:")
            source = f"assets/minecraft/textures/{relative}.png"
            if source in old_files:
                return "minecraft:" + relative
            path = RES / f"assets/farmerstales/textures/{relative}.png"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(jar.read(source))
            return "farmerstales:" + relative

        def model(ref):
            relative = ref.removeprefix("minecraft:")
            source = f"assets/minecraft/models/{relative}.json"
            if source in old_files:
                return "minecraft:" + relative
            if relative not in copied:
                copied.add(relative)
                data = json.loads(jar.read(source))
                if "parent" in data:
                    data["parent"] = model(data["parent"])
                data["textures"] = {k: texture(v) for k, v in data.get("textures", {}).items()}
                for element in data.get("elements", []):
                    element.pop("shade_direction_override", None)
                    emission = element.pop("light_emission", 0)
                    if emission:
                        element["forge_data"] = {"block_light": emission, "sky_light": emission, "ambient_occlusion": False}
                if relative.startswith("block/"):
                    data["render_type"] = "minecraft:cutout"
                write(RES / f"assets/farmerstales/models/{relative}.json", data)
            return "farmerstales:" + relative

        def state_models(data):
            if isinstance(data, list):
                return [state_models(v) for v in data]
            if isinstance(data, dict):
                return {k: model(v) if k == "model" else state_models(v) for k, v in data.items()}
            return data

        for name in NAMES:
            write(RES / f"assets/farmerstales/blockstates/{name}.json",
                  state_models(json.loads(jar.read(f"assets/minecraft/blockstates/{name}.json"))))
            model(f"minecraft:item/{name}")
            if name == "wildflowers":
                loot = remap(json.loads(old.read("data/minecraft/loot_tables/blocks/pink_petals.json")),
                             {"minecraft:pink_petals": "farmerstales:wildflowers"})
            else:
                loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "conditions": [{
                    "condition": "minecraft:survives_explosion"}], "entries": [{
                        "type": "minecraft:item", "name": f"farmerstales:{name}"}]}]}
            write(RES / f"data/farmerstales/loot_tables/blocks/{name}.json", loot)
        texture("minecraft:particle/firefly")
        write(RES / "assets/farmerstales/particles/firefly.json", {"textures": ["farmerstales:firefly"]})
        english = json.loads(jar.read("assets/minecraft/lang/en_us.json"))
        path = ROOT / "src/generated/resources/assets/farmerstales/lang/en_us.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data.update({f"block.farmerstales.{name}": english[f"block.minecraft.{name}"] for name in NAMES})
        data["subtitles.farmerstales.block.firefly_bush.idle"] = english["subtitles.block.firefly_bush.idle"]
        write(path, data)
        apply_official_chinese(NAMES, extra_keys={"subtitles.block.firefly_bush.idle": "subtitles.farmerstales.block.firefly_bush.idle"})

    # Sounds were downloaded separately from Mojang's asset CDN and SHA-1 verified.
    manifest = json.loads((ROOT / "refer/minecraft-26.3-lang/asset-index.json").read_text(encoding="utf-8-sig"))
    official_sounds = json.loads((ROOT / "refer/minecraft-26.3-lang/sounds.json").read_text(encoding="utf-8"))
    sound = copy.deepcopy(official_sounds["block.firefly_bush.idle"])
    for entry in sound["sounds"]:
        asset = entry["name"]
        source = ROOT / f"refer/minecraft-26.3-firefly-sounds/{asset.split('/')[-1]}.ogg"
        expected = manifest["objects"][f"minecraft/sounds/{asset}.ogg"]["hash"]
        assert hashlib.sha1(source.read_bytes()).hexdigest() == expected, source
        path = RES / f"assets/farmerstales/sounds/{asset}.ogg"
        path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, path)
        entry["name"] = "farmerstales:" + asset
    sound["subtitle"] = "subtitles.farmerstales.block.firefly_bush.idle"
    path = RES / "assets/farmerstales/sounds.json"
    sounds = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    sounds["block.firefly_bush.idle"] = sound
    write(path, sounds)

    def air():
        return {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]}

    near_water = {"type": "minecraft:any_of", "predicates": [
        {"type": "minecraft:matching_fluids", "fluids": ["minecraft:water", "minecraft:flowing_water"], "offset": offset}
        for offset in ([1, -1, 0], [-1, -1, 0], [0, -1, 1], [0, -1, -1])]}
    feature("patch_firefly_bush", "minecraft:random_patch", {"tries": 32, "xz_spread": 7, "y_spread": 3,
        "feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": provider("firefly_bush")}},
                    "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                        "type": "minecraft:all_of", "predicates": [air(), near_water]}}]}})
    placed("patch_firefly_bush", "patch_firefly_bush", count=4)
    write(RES / "data/farmerstales/forge/biome_modifier/add_firefly_bush.json", {
        "type": "forge:add_features", "biomes": ["minecraft:" + n for n in (
            "river", "frozen_river", "swamp", "mangrove_swamp", "badlands", "wooded_badlands", "eroded_badlands")]
            + ["farmerstales:pale_garden"], "features": ["farmerstales:patch_firefly_bush"], "step": "vegetal_decoration"})

    states = [{"data": {"Name": "farmerstales:wildflowers", "Properties": {"flower_amount": str(amount), "facing": facing}}, "weight": 1}
              for amount in range(1, 5) for facing in ("north", "south", "west", "east")]
    feature("patch_wildflowers", "minecraft:flower", {"tries": 48, "xz_spread": 6, "y_spread": 2,
        "feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": {
            "type": "minecraft:weighted_state_provider", "entries": states}}}, "placement": [{
                "type": "minecraft:block_predicate_filter", "predicate": air()}]}})
    placed("patch_wildflowers", "patch_wildflowers", count=2)
    write(RES / "data/farmerstales/forge/biome_modifier/add_wildflowers.json", {
        "type": "forge:add_features", "biomes": ["minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:meadow"],
        "features": ["farmerstales:patch_wildflowers"], "step": "vegetal_decoration"})

    feature("cactus_flower", "minecraft:simple_block", {"to_place": provider("cactus_flower")})
    write(RES / "data/farmerstales/worldgen/placed_feature/cactus_flower.json", {"feature": "farmerstales:cactus_flower",
        "placement": [{"type": "minecraft:count", "count": 64}, {"type": "minecraft:in_square"},
                      {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"},
                      {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                          air(), {"type": "minecraft:matching_blocks", "blocks": ["minecraft:cactus"], "offset": [0, -1, 0]}]}}]})
    write(RES / "data/farmerstales/forge/biome_modifier/add_cactus_flowers.json", {
        "type": "forge:add_features", "biomes": ["minecraft:desert", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"],
        "features": ["farmerstales:cactus_flower"], "step": "vegetal_decoration"})

    tag("minecraft", "blocks", "replaceable_by_trees", [f"farmerstales:{n}" for n in NAMES])
    for kind in ("blocks", "items"):
        tag("minecraft", kind, "flowers", ["farmerstales:wildflowers", "farmerstales:cactus_flower"])
    for name, dye in (("wildflowers", "yellow_dye"), ("cactus_flower", "pink_dye")):
        recipe(name + "_dye", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [
            {"item": f"farmerstales:{name}"}], "result": {"item": f"minecraft:{dye}"}}, f"farmerstales:{name}")
    print("Generated 3 plants, official models/particles/sounds, worldgen, recipes and Chinese translations.")


if __name__ == "__main__":
    main()
