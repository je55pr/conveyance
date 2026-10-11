"""Shared power and fuel tooltip labels for the Factorio base-power audit."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def generate_all(root=ROOT):
    translations = {
        "tooltip.conveyance.stat.idle_usage": "Idle drain: %s kW",
        "tooltip.conveyance.stat.power_usage_peak": "Power usage: %s kW (peak)",
        "tooltip.conveyance.stat.power_rotation": "Power while rotating: %s kW",
        "tooltip.conveyance.stat.fuel_usage_peak": "Fuel consumption: %s kW (peak)",
        "tooltip.conveyance.stat.fuel_rotation": "Fuel consumption while rotating: %s kW",
        "tooltip.conveyance.assembler": "Produces the selected recipe from its ingredients. Draws standby power when idle.",
        "tooltip.conveyance.inserter": "Moves items using electricity. Draw varies with arm movement; pickup and drop use extra power.",
        "tooltip.conveyance.long_inserter": "Reaches two blocks on each side. Draw varies with arm movement; pickup and drop use extra power.",
        "tooltip.conveyance.burner_inserter": "Moves items using fuel, with no idle consumption. Can take fuel from its input to start itself.",
    }
    for language in ("en_us", "en_gb"):
        target = root / f"assets/conveyance/lang/{language}.json"
        data = json.loads(target.read_text(encoding="utf-8")); data.update(translations)
        target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    generate_all()
