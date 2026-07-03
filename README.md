# Persona

A client side Minecraft mod that gives you a local identity. Change your displayed
name, copy another player's skin, and pick any official cape, all from an in game menu
opened with Right Shift.

> Local only. Everything Persona does affects your own client render only. Other players
> on a server never see your Persona name, skin or cape, because the server is
> authoritative for that data. This is by design.

## Features

* In game menu opened with Right Shift (remappable in Options then Controls then Misc).
* Two modes, switched with the buttons at the top of the menu:
  * Custom: set the name, skin and cape independently.
  * Impersonate: type one username and take their whole identity (name, skin and cape).
* Name replacement on the nametag above your head, in the tab list and in chat.
* Skin fetched from the Mojang API by username, with the correct slim or classic model.
* Searchable list of every official cape. Bundled real textures render, capes without a
  bundled texture are shown as unavailable.
* Live rotatable 3D preview of your appearance. Drag to rotate.
* Settings persist to config/persona.json.

## Tech

| | |
|---|---|
| Loader | Fabric with Fabric API |
| Minecraft | 1.21.11 |
| Mappings | Yarn |
| Java | 21 or newer |
| Build | Gradle wrapper with Fabric Loom |

The newest launcher build at the time of writing is 26.2, but Mojang ships no mappings
for it yet (no Yarn, no official Mojang mappings), so it cannot be modded. 1.21.11 is the
newest stable version with full modding support. Bump gradle.properties once mappings for
a newer version land.

## Build and run

```bash
./gradlew build          # produces build/libs/persona-<version>.jar
./gradlew runClient      # launches a dev client with the mod
```

Drop the built jar from build/libs into your .minecraft/mods folder next to Fabric API to
use it in a normal install.

## Cape textures

Real 64x32 cape textures live in src/main/resources/assets/persona/textures/capes. They
are fetched from textures.minecraft.net by tools/fetch_capes.py. To add more capes, add
their id and texture hash to that script (hashes are on each cape's minecraft.wiki
article) and run it again:

```bash
python3 tools/fetch_capes.py
```

Any cape id in CapeRegistry.java without a matching PNG is shown as unavailable rather
than omitted.

## How it works

All overrides are local render hooks applied with mixins (Yarn names):

| Feature | Target |
|---|---|
| Nametag | EntityRenderer#getDisplayName |
| Tab list | PlayerListHud#getPlayerName |
| Chat | ChatHud#addMessage |
| Skin and cape | AbstractClientPlayerEntity#getSkin |

The preview and impersonate skin/cape come from the vanilla PlayerSkinProvider, which
downloads the skin and parses the cape from the player's profile.

## License

MIT
