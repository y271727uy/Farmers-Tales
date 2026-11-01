"""Extract official 26.3 leaf particle sprites from the existing local client JAR."""
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources/assets/farmerstales"
PARTICLES = ("tinted_leaves", "pale_oak_leaves", "red_poplar_leaves", "orange_poplar_leaves", "yellow_poplar_leaves")


def generate():
    with zipfile.ZipFile(ROOT / "refer/minecraft-26.3-client.jar") as jar:
        for name in PARTICLES:
            source = json.loads(jar.read(f"assets/minecraft/particles/{name}.json"))
            textures = []
            for reference in source["textures"]:
                texture = reference.removeprefix("minecraft:")
                path = RES / f"textures/particle/{texture}.png"
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(jar.read(f"assets/minecraft/textures/particle/{texture}.png"))
                textures.append(f"farmerstales:{texture}")
            path = RES / f"particles/{name}.json"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps({"textures": textures}, indent=2) + "\n", encoding="utf-8")
    print("Generated 5 leaf particle definitions and 36 official leaf sprites.")


if __name__ == "__main__":
    generate()
