# VaultShop

A full buy/sell item shop for Spigot 1.21.x + Vault. Every obtainable item can be sold or bought,
prices are balanced (raw materials < refined materials < crafted blocks, with a labor premium on
compacted blocks), and there's a searchable, paginated GUI plus a drop-in `/sell` menu.

## ⚠️ Important: this needs to be compiled once

I wrote all the source, but I could not compile it into a `.jar` myself — my sandbox can only
reach npm/pip/cargo/GitHub, not Spigot's or Vault's Maven repositories. **You need one extra
step** to turn this into the file that goes in your `/plugins` folder. Pick whichever is easier:

### Option A — GitHub Actions (no installs, ~2 minutes)
1. Create a new **public** repo on GitHub and upload this entire folder to it (drag-and-drop
   works fine on github.com — no git command line needed).
2. GitHub will automatically run the included workflow (`.github/workflows/build.yml`).
3. Go to the repo's **Actions** tab → click the latest run → download the `VaultShop-jar`
   artifact → unzip it. That's your `vaultshop.jar`.

### Option B — Replit (also no installs)
1. Go to replit.com, create a new **Java** repl.
2. Delete its default files, upload this whole folder instead.
3. In the Replit shell, run: `mvn package`
4. Download `target/vaultshop.jar` from the file browser.

Either way takes a couple of minutes and doesn't touch your own computer.

## Installing on Aternos
1. In your Aternos server settings, the software must be **Spigot** (or Paper) — not Vanilla/Forge.
2. Install **Vault** the same way (Aternos's plugin browser has it, or upload it the same way).
3. You also need an actual economy plugin behind Vault (e.g. EssentialsX) if you don't have money/`/balance` already working.
4. Upload `vaultshop.jar` into the `plugins` folder via Aternos's File Manager.
5. Restart the server. Look for `VaultShop enabled - hooked into Vault, ### sellable items loaded.` in the console.

## Commands
- `/shop` — opens the browsing/search GUI. **Left-click** an item to sell 1, **shift-click** to
  sell every one of that item you're carrying, **right-click** to buy 1. Hovering any item shows
  its buy/sell price and how many you own, in the tooltip.
- `/shop search <term>` — jump straight to a filtered view (there's also a compass button inside the GUI).
- `/sell` — opens an empty menu; drop items in, close the inventory, and everything sellable
  inside gets sold at once. Anything it doesn't recognize as sellable (or that you didn't finish
  placing) is handed straight back to you — nothing is ever silently deleted.
- `/sell hand` — instantly sells whatever's in your hand.
- `/shop reload` *(admin)* — reloads config.yml and prices.yml.
- `/shop setprice <material> <buy|sell> <amount>` *(admin)* — overrides one item's price,
  e.g. `/shop setprice DIAMOND sell 35`.

Permissions: `vaultshop.use` (default: everyone), `vaultshop.admin` (default: op).

## How pricing works
- `plugins/VaultShop/prices.yml` is generated the first time the plugin starts, with a price for
  every single sellable item in the game — nothing needs to be added by hand.
- Buy price = sell price × `buy-markup-multiplier` (default 1.5x, set in config.yml), so buying
  is always more expensive than selling — no buy-then-resell money loop.
- Raw resources are cheap, refined/smelted items cost more, and compacted storage blocks cost
  *more* than 9× their ingot price to reflect the crafting labor — e.g. Iron Ingot sells for $5,
  but an Iron Block sells for $50, not the "naive" $45.
- You can hand-edit any price directly in `prices.yml`, or use `/shop setprice`.
- A few item types are intentionally excluded from the shop for safety: written books (to avoid
  destroying player-authored text), bundles and loaded shulker boxes (to avoid destroying
  whatever's stored inside them), and creative-only/technical blocks like command blocks, barriers
  and spawners.

## Honesty about testing
I wrote and carefully reviewed every file, paid close attention to the classic shop-plugin bugs
(item duplication, item loss on server-close-while-menu-open, async chat-thread safety), and the
logic is internally consistent. But I have not been able to run this against a live Minecraft
server or a real Bukkit/Vault classpath from this sandbox. **Please test it on a spare/test world
before trusting it with your real server's economy.** If `mvn package` fails because Spigot bumped
the API version, just change the `<version>` under `spigot-api` in `pom.xml` to whatever version
your build environment resolves (a slightly different Spigot API patch version will still work
fine at runtime; `api-version: '1.21'` in plugin.yml covers the whole 1.21.x line).
