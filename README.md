# If you want to download the plugin, ensure the plugin is ended with "-core" !

# RPGItems [![Build Status](https://ci.nyaacat.com/job/RPGItems-reloaded/job/main/badge/icon)](https://ci.nyaacat.com/job/RPGItems-reloaded/job/main/)

The RPGItems2 plugin continued from [TheCreeperOfRedstone/RPG-Items-2](https://github.com/TheCreeperOfRedstone/RPG-Items-2)

**RPGitem starting from 3.6 depends on [NyaaCore](https://github.com/NyaaCat/NyaaCore) to work! See [Installation](https://nyaacat.github.io/RPGItems-wiki/#/en-us/installation) for detail.**

## Project Discussion

Discord Server: [![Discord](https://img.shields.io/discord/486394125206421524.svg?logo=discord&link=https%3A%2F%2Fdiscord.gg%QeVy8Yd)](https://discord.gg/QeVy8Yd)

## Downloads

Development builds can be found at [our Jenkins](https://ci.nyaacat.com/job/RPGItems-reloaded/)

1.7 ~ 1.13.2 builds can be found in the [Releases Page](https://github.com/NyaaCat/RPGItems-reloaded/releases).
Please choose builds with prefixes matching your server version. Some version use code from [NBT API](https://www.spigotmc.org/resources/item-entity-tile-nbt-api.7939/) internally.

## Information

**Ensure you have latest Spigot/Paper and NyaaCore**. Some commands and item file format are updated. RPGItems 3.8 is coming with lots of new feature, and it should fully compatible with 3.7 item file. **Note: item file format update are one-way! Backup your 3.7 item files in case you need them later**

## Resources

[Wiki](https://nyaacat.github.io/RPGItems-wiki/#/) | [Spigot page](https://www.spigotmc.org/resources/rpgitems.17549/) | [Javadoc](https://ci.nyaacat.com/javadocs/)

## `/rpgitem give`

```
/rpgitem give <item> [targets] [amount]
/rpgitem give <group> [targets]
```

* `targets` is **one argument**: a comma separated list of online player names, player UUIDs and/or
  Minecraft target selectors, without spaces. Commas inside a selector's `[...]` belong to the selector.
  Omitted: the item goes to the player running the command (the console gets `Cannot give console items`).
* `amount` is what **each** player receives, a whole number from 1 to 2304. Omitted: 1. Item groups always
  give one of each item.
* Every player is given the item at most once, however many list elements match them.

| command | result |
| --- | --- |
| `/rpgitem give sword` | 1 to yourself (unchanged) |
| `/rpgitem give sword Alice` | 1 to Alice (unchanged) |
| `/rpgitem give sword Alice 5` | 5 to Alice (unchanged) |
| `/rpgitem give sword Alice,Bob 3` | 3 to Alice and 3 to Bob |
| `/rpgitem give sword @a 2` | 2 to every online player |
| `/rpgitem give sword @a[distance=..10]` | 1 to every player within 10 blocks of the sender |
| `/rpgitem give sword @a[tag=vip,limit=3],Alice 1` | 1 to up to three tagged players and to Alice, Alice only once |
| `/rpgitem give starterkit @a` | one of each item of the group `starterkit` to every online player |

The command is all-or-nothing. Targets and amount are resolved and checked before anything is handed out, and
any problem aborts the command with a message ending in `Nothing was given.`: a name that is not online, a
selector that does not parse, a selector matching nobody, a selector matching anything that is not a player
(use `@a` or `@e[type=player]`), an empty list element, an amount that is not a number from 1 to 2304, or a
surplus argument.

Selectors are evaluated by the server (`Bukkit.selectEntities`) from the sender's position, so for a player
`@p` and `@s` are that player and `distance=` is measured from them. From the console `@s` matches nobody and
positions are relative to the world origin; a command block uses its own position.

Permissions:

| permission | default | needed for |
| --- | --- | --- |
| `rpgitem` (or `rpgitem.give.<item>` / `rpgitem.give.group.<group>` with `/rpgitem giveperms` on) | op | the command itself, as before |
| `rpgitem.givetarget.selector` | op | any `@` selector as a target |
| `rpgitem.givetarget.multiple` | op | delivering to more than one player in one command |

A single player name needs no extra permission. Differences from earlier versions: an amount that is not a
valid number used to be silently read as 1, and surplus arguments were ignored; both are now errors. A
non-op who was granted `rpgitem` now also needs `rpgitem.givetarget.selector` to use `@p`/`@s`. Groups can
now be given by the console when a target is named.

Tab completion offers item and group names, then online player names plus `@a @p @r @s` (selectors only if
the sender has `rpgitem.givetarget.selector`) and continues a list after each comma, then `1 16 64`.

## `firingLocation` and the extension API

The `firingLocation` property of `projectile`, `beam`, `aoedamage` and `attract` accepts `SELF` (default),
`TARGET`, and any name an installed extension registered. `SELF` and `TARGET` behave and are stored exactly
as before. `/rpgitem power` commands refuse names that are not registered at that moment; item files keep
whatever name they contain, so an item is not rewritten when its extension is missing. A power whose name
cannot be resolved when it fires simply does not fire and consumes neither cooldown nor durability.

Extensions register names through `think.rpgitems.api.firing`:

```java
FiringLocations.register(plugin, "MY_PLACE", context -> {
    // server thread, once per power activation; context = player, source entity, item, power
    Entity anchorEntity = pick(context);               // capture identity now
    if (anchorEntity == null) return Optional.empty(); // -> the power does not fire
    return Optional.of(() -> anchorEntity.isValid()    // asked again before every shot, also delayed ones
            ? Optional.of(FiringPoint.origin(anchorEntity.getLocation()))
            : Optional.empty());                       // -> this shot and the rest of the burst are skipped
});
```

* `FiringPoint.origin(location)` replaces the firing entity: shots start at the location and follow its
  yaw/pitch. `FiringPoint.cast(location, entity)` replaces the ray-traced point of `TARGET`: shots are fired
  at/around the location.
* With `castOff: true` a power keeps the point of its activation for the whole burst; otherwise the anchor is
  asked before each shot.
* Names are case-insensitive (`[A-Z][A-Z0-9_]*`). `SELF`/`TARGET` cannot be registered, and a name belongs to
  the plugin that registered it; that plugin may register it again (e.g. on `RPGItemsReloadEvent`).
* RPGItems removes a plugin's names when that plugin is disabled. Registration and lookup are thread-safe;
  `bind`/`locate` touch entities and run on the server thread only.
