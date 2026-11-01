"""Apply the checked-in subset of Mojang's verified 26.3 Chinese assets."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = Path(__file__).resolve().parent / "data/minecraft-26.3-official-zh.json"


def apply_official_chinese(block_names, biome_names=(), extra_keys=None):
    source = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    for locale, asset in source["locales"].items():
        path = ROOT / f"src/main/resources/assets/farmerstales/lang/{locale}.json"
        current = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        for kind, names in (("block", block_names), ("biome", biome_names)):
            for name in names:
                current[f"{kind}.farmerstales.{name}"] = asset["translations"][f"{kind}.minecraft.{name}"]
        for original, target in (extra_keys or {}).items():
            current[target] = asset["translations"][original]
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(current, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
