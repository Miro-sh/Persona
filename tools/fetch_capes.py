#!/usr/bin/env python3
"""
Fetch official Minecraft cape textures (64x32 PNG) from textures.minecraft.net
into the mod's assets folder.

Each cape id here must match an id in CapeRegistry.java. Any cape whose id is
NOT present below (or whose download fails) is left out and shown as
"indisponible" in-game. To enable more capes, add "<id>": "<texture_hash>"
lines and re-run:  python3 tools/fetch_capes.py
"""
import os
import sys
import urllib.request

OUT_DIR = os.path.join(os.path.dirname(__file__), "..",
                       "src", "main", "resources", "assets", "persona", "textures", "capes")
BASE = "http://textures.minecraft.net/texture/"

# id -> texture hash (sourced from minecraft.wiki cape articles)
HASHES = {
    "migrator":         "2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0ba7defd635ca7a933",
    "vanilla":          "f9a76537647989f9a0b6d001e320dac591c359e9e61a31f4ce11c88f207f0ad4",
    "cobalt":           "ca35c56efe71ed290385f4ab5346a1826b546a54d519e6a3ff01efa01acce81",
    "cherry_blossom":   "afd553b39358a24edfe3b8a9a939fa5fa4faa4d9a9c3d6af8eafb377fa05c2bb",
    "minecon_2011":     "953cac8b779fe41383e675ee2b86071a71658f2180f56fbce8aa315ea70e2ed6",
    "minecon_2012":     "a2e8d97ec79100e90a75d369d1b3ba81273c4f82bc1b737e934eed4a854be1b6",
    "minecon_2013":     "153b1a0dfcbae953cdeb6f2c2bf6bf79943239b1372780da44bcbb29273131da",
    "minecon_2015":     "b0cc08840700447322d953a02b965f1d65a13a603bf64b17c803c21446fe1635",
    "minecon_2016":     "e7dfea16dc83c97df01a12fabbd1216359c0cd0ea42f9999b6e97c584963e980",
    "mojang_studios":   "9e507afc56359978a3eb3e32367042b853cddd0995d17d0da995662913fb00f7",
    "mojang":           "5786fe99be377dfb6858859f926c4dbc995751e91cee373468c5fbf4865e7151",
    "classic_mojang":   "8f120319222a9f4a104e2f5cb97b2cda93199a2ee9e1585cb8d09d6f687cb761",
    "anniversary_15th": "cd9d82ab17fd92022dbd4a86cde4c382a7540e117fae7b9a2853658505a80625",
    "founders":         "99aba02ef05ec6aa4d42db8ee43796d6cd50e4b2954ab29f0caeb85f96bf52a1",
    "followers":        "569b7f2a1d00d26f30efe3f9ab9ac817b1e6d35f4f3cfb0324ef2d328223d350",
    "purple_heart":     "cb40a92e32b57fd732a00fc325e7afb00a7ca74936ad50d8e860152e482cfbde",
    "menace":           "dbc21e222528e30dc88445314f7be6ff12d3aeebc3c192054fba7e3b3f8c77b1",
    "home":             "1de21419009db483900da6298a1e6cbf9f1bc1523a0dcdc16263fab150693edd",
    "cheapshot":        "ca29f5dd9e94fb1748203b92e36b66fda80750c87ebc18d6eafdb0e28cc1d05f",
    "prismarine":       "d8f8d13a1adf9636a16c31d47f3ecc9bb8d8533108aa5ad2a01b13b1a0c55eac",
    "pan":              "28de4a81688ad18b49e735a273e086c18f1e3966956123ccb574034c06f5d336",
    "crafter":          "479eacefa3cdd7aca94207f36c0dd449653ddf259daf40544a5866baf05eee22",
    "minecraft_experience": "7658c5025c77cfac7574aab3af94a46a8886e3b7722a895255fbf22ab8652434",
    "moonlight_trail":  "fe8a02dfe9e390e44ff33d69feef9d3943f76d3901015bbd50f0b67722d288bd",
    "mcc_15th_year":    "56c35628fe1c4d59dd52561a3d03bfa4e1a76d397c8b9c476c2f77cb6aebb1df",
    "mojang_office":    "5c29410057e32abec02d870ecb52ec25fb45ea81e785a7854ae8429d7236ca26",
    "copper":           "5e6f3193e74cd16cdd6637d9bae5484e3a37ff2a14c2d157c659a07810b1bdca",
    "zombie_horse":     "a3f6e4f14801f3ea55e3d95b9b4ef3b5e8802d947f669de93d6ec4b9354a436b",
    "builder":          "2c579968c64c1719740fd8c2a451461879b238002574fce48f7d1a7c36a1c7d4",
    "snowman":          "23ec737f18bfe4b547c95935fc297dd767bb84ee55bfd855144d279ac9bfd9fe",
    "turtle":           "5048ea61566353397247d2b7d946034de926b997d5e66c86483dfb1e031aee95",
    "scrolls_champion": "3efadf6510961830f9fcc077f19b4daf286d502b5f5aafbd807c7bbffcaca245",
    "translator":       "1bf91499701404e21bd46b0191d63239a4ef76ebde88d27e4d430ac211df681e",
    "millionth_customer": "70efffaf86fe5bc089608d3cb297d3e276b9eb7a8f9f2fe6659c23a2d8b18edf",
    "valentine":        "e578ef995fabcf0a94768f9651ac3aaba30c59ef85d2438e9b3e0cc1d810652b",
    "birthday":         "2056f2eebd759cce93460907186ef44e9192954ae12b227d817eb4b55627a7fc",
}

PNG_MAGIC = b"\x89PNG\r\n\x1a\n"


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    ok, fail = 0, 0
    for cape_id, h in sorted(HASHES.items()):
        url = BASE + h
        try:
            with urllib.request.urlopen(url, timeout=20) as resp:
                data = resp.read()
            if not data.startswith(PNG_MAGIC):
                print(f"  SKIP  {cape_id:18} (not a PNG)")
                fail += 1
                continue
            path = os.path.join(OUT_DIR, cape_id + ".png")
            with open(path, "wb") as f:
                f.write(data)
            print(f"  OK    {cape_id:18} {len(data):6} bytes")
            ok += 1
        except Exception as e:
            print(f"  FAIL  {cape_id:18} {e}")
            fail += 1
    print(f"\nDone: {ok} capes fetched, {fail} skipped -> {os.path.relpath(OUT_DIR)}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
