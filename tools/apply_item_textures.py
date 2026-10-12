"""Use supplied item PNGs after generating placeholder models."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'

def apply(root=ROOT):
    for sprite in sorted((root / 'assets/conveyance/textures/item').glob('*.png')):
        if sprite.stem == 'generator':
            continue  # The creative generator keeps its magenta machine icon.
        model = root / f'assets/conveyance/models/item/{sprite.stem}.json'
        model.parent.mkdir(parents=True, exist_ok=True)
        content = json.dumps({'parent': 'minecraft:item/generated',
            'textures': {'layer0': f'conveyance:item/{sprite.stem}'}}, indent=2) + '\n'
        if not model.exists() or model.read_text(encoding='utf-8') != content:
            model.write_text(content, encoding='utf-8')

if __name__ == '__main__':
    apply()
