<div align="center">

# Persona

**Become anyone. Locally.**

A client side Fabric mod that lets you change your Minecraft **name**, **skin** and **cape**,
or fully **impersonate** another player, all from a clean in game menu.
Everything is a local illusion: only you see it.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?style=for-the-badge)
![Loader](https://img.shields.io/badge/Loader-Fabric-1976d2?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21%2B-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

![Latest release](https://img.shields.io/github/v/release/Miro-sh/Persona?style=flat-square)
![Downloads](https://img.shields.io/github/downloads/Miro-sh/Persona/total?style=flat-square)
![Stars](https://img.shields.io/github/stars/Miro-sh/Persona?style=flat-square)

</div>

---

## What it does

Open the menu with **Right Shift** and pick one of two modes.

### Custom
Set each piece of your look on its own:

* **Name** shown on your nametag, in the tab list and in chat.
* **Skin** copied from any player by username (correct slim or classic model).
* **Cape** chosen from a searchable list of every official Minecraft cape.

### Impersonate
Type one username and take their whole identity at once: **name, skin and their real cape**.

On the right, a **live 3D preview** shows the result. Drag it to spin the model a full turn and
admire the cape.

> **Local only.** Persona changes your own client render only. Other players on a server never see
> your Persona name, skin or cape, because the server owns that data. This is by design.

<!-- Add a screenshot of the menu here, for example:
![Persona menu](docs/menu.png)
-->

## Features

* Two modes: Custom and Impersonate.
* Name override on nametag, tab list and chat.
* Skin fetched from the Mojang API by username.
* 36 bundled real official cape textures, with a search box.
* Live rotatable 3D preview.
* Clean custom themed interface.
* Settings saved to `config/persona.json`.
* Remappable open key (default Right Shift).

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft **1.21.11**.
2. Download **[Fabric API](https://modrinth.com/mod/fabric-api)** for 1.21.11.
3. Download `persona-x.y.z.jar` from the [**Releases**](https://github.com/Miro-sh/Persona/releases) page.
4. Drop both jars into your `.minecraft/mods` folder.
5. Launch the game and press **Right Shift**.

## Build from source

Requires JDK 21 or newer.

```bash
git clone https://github.com/Miro-sh/Persona.git
cd Persona
./gradlew build          # jar lands in build/libs/persona-<version>.jar
./gradlew runClient      # launch a dev client with the mod
```

## Adding more capes

Real 64x32 cape textures live in `src/main/resources/assets/persona/textures/capes`. They are
fetched from `textures.minecraft.net` by `tools/fetch_capes.py`. To add a cape, put its id and
texture hash (found on each cape's [minecraft.wiki](https://minecraft.wiki/w/Cape) page) into that
script and run it again:

```bash
python3 tools/fetch_capes.py
```

Any cape listed in `CapeRegistry.java` without a matching PNG appears as unavailable.

## How it works

All overrides are local render hooks applied with mixins:

| Feature | Hook |
|---|---|
| Nametag | `EntityRenderer#getDisplayName` |
| Tab list | `PlayerListHud#getPlayerName` |
| Chat | `ChatHud#addMessage` |
| Skin and cape | `AbstractClientPlayerEntity#getSkin` |

Skins and impersonated capes come from the vanilla `PlayerSkinProvider`, which downloads the skin
and reads the cape from the player's Mojang profile.

## License

[MIT](LICENSE)
