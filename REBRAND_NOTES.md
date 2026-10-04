# GGDonutShop → AstraDonutShop Rebrand

This repository now contains both:

- `GGDonutShop.jar` — the original plugin jar (kept for reference/history).
- `AstraDonutShop.jar` — the fully rebranded, ready-to-use plugin jar.
- `tools/classpatch.py` / `tools/rebrand_jar.py` — the scripts used to produce
  `AstraDonutShop.jar` from `GGDonutShop.jar`. Run `python3 tools/rebrand_jar.py`
  from the repo root to regenerate it at any time.

## What changed

**Identity**
- Plugin name: `GGDonutShop` → `AstraDonutShop`
- Author: `NotDaKuxD` → `DevSolentz`
- Brand: → `AstraLab`
- Main class: `net.notdakuxd.nxdonutshop.NxDonutShop` → `net.astralab.astradonutshop.AstraDonutShop`
- Java package: `net.notdakuxd.nxdonutshop` → `net.astralab.astradonutshop`
- Internal helper class `NxSpriteResolver` → `AstraSpriteResolver`
- Maven metadata (`META-INF/maven/...`, `MANIFEST.MF`): `net.notdakuxd:GGDonutShop` → `net.astralab:AstraDonutShop`

**Dependencies (`plugin.yml`)**
- Hard dependency on `GGPlugins` **removed**. Codebase inspection (full
  bytecode disassembly of every class) confirmed `GGPlugins` was never
  actually referenced or hooked into by any code path — it was a
  plugin.yml-only declaration — so no functional replacement was needed.
- `Vault` is now the **only** hard dependency (`depend: [Vault]`).
- `Essentials` / `EssentialsX` remain as unrelated, pre-existing soft
  dependencies.

**Permissions**
- `ggdonutshop.admin` and the legacy `nxdonutshop.admin` fallback (both were
  checked as alternatives for the same admin actions — reload & price
  randomizer) were unified into a single `astradonutshop.admin` node, now
  also declared in `plugin.yml`.

**Commands**
- `/shop` and `/sell` kept (they were never GG-namespaced).
- Alias `nxshop` → `astrashop` (the `donutshop` alias was kept as-is).

**User-facing text**
- Console startup banner, disable message, reload/randomizer admin
  messages: `GG DonutShop` → `AstraDonutShop`, author line now reads
  `DevSolentz`.

**Configuration files**
- `config.yml`, `shop.yml`, `enchantment.yml` header comments updated to
  AstraDonutShop / AstraLab branding. All functional keys/values
  (prices, materials, multipliers, etc.) are untouched.

## How the jar was patched safely

Since only compiled `.class` files were available (no source, no `pom`
build inputs like the Paper API jar), the rebrand was done as a targeted,
verifiable **constant-pool** rewrite rather than a decompile/recompile:

- Every `.class` file's constant pool was parsed, and only `CONSTANT_Utf8`
  entries (used for class/member names, type descriptors, signatures, and
  string literals) were rewritten, using an explicit, exact-match
  replacement list — never a blind `"gg" → "astra"` substitution (which
  would have corrupted unrelated words like `EGG`, `LEGGINGS`, `NUGGET`,
  `toggle`, `trigger`, etc.).
- All other constant-pool entries and every byte of every method's actual
  bytecode (`Code` attributes) were left **byte-for-byte identical** —
  verified programmatically for all 26 classes before shipping.
- This is safe because the JVM class file format's constant pool is a flat,
  self-length-prefixed list; nothing outside it references constant-pool
  entries by byte offset, only by index, so changing a Utf8 entry's length
  cannot corrupt anything else in the file.

No `ClassNotFoundException` / `NoClassDefFoundError` risk is introduced:
package and class renames were applied consistently to every reference
across all 26 class files (verified via constant-pool text search — zero
remaining occurrences of the old package/class names anywhere in the jar).
