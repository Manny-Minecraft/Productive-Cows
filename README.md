<div align="center">
  <img src=".idea/icon.png" width="300"/>

  # ProductiveCows
  
  [![NeoForge](https://img.shields.io/badge/NeoForge-1.21.1-orange.svg)](https://neoforged.net/)
	<a href="https://www.curseforge.com/minecraft/mc-mods/productive-cows"><img src="https://img.shields.io/curseforge/dt/1554716?logo=curseforge&label=&suffix=%20&style=flat&color=242629&labelColor=F16436&logoColor=1C1C1C" alt="CurseForge"></a>
  [![Modrinth](https://img.shields.io/badge/Modrinth-Pending-1bd96a.svg)](#)
</div>

ProductiveCows is a modern, data-driven resource generation mod for Minecraft. Breed, milk, and process custom material cows to automate your resource gathering! 

---

## Adding Custom Cows (For Modpack Devs)

ProductiveCows is **100% Data-Driven**. You can add new cows without writing a single line of Java by creating a simple JSON file in your Datapack or via KubeJS!

### Method 1: KubeJS (Recommended)
You can easily register new cows in your `server_scripts`:

```javascript
ProductiveCowsEvents.registerCows(event => {
    event.create('vibranium')
        .name('Vibranium')
        .tier(5)
        .fluid('productivecows:molten_vibranium')
        .color('#800080')
        .baseYield(100)
        .breedChance(0.05)
        .parents('productivecows:diamond', 'productivecows:obsidian')
        .coolingResult('c:ingots/vibranium')
})
```

Ids without a namespace get the `kubejs` namespace: the cow above is `kubejs:vibranium`, and that is the id to use as a parent of other cows. `.coolingResult()` takes an item id, or a tag prefixed with `#` (strings starting with `c:` are always read as tags).

### Method 2: JSON Datapack
Create a file in your datapack at: `data/<your_namespace>/cows/my_custom_cow.json`

```json
{
  "name": "Vibranium",
  "tier": 5,
  "hex_color": "#800080",
  "fluid": "productivecows:molten_vibranium",
  "base_yield": 100,
  "breed_chance": 0.05,
  "parent1": "productivecows:diamond",
  "parent2": "productivecows:obsidian",
  "cooling_result_tag": "c:ingots/vibranium"
}
```

That's it! The mod will automatically register the cow and process the cooling into the correct tag/item.

Invalid definitions (unknown fluid or cooling item, malformed tag, `breed_chance` outside 0 to 1) are skipped with a warning in the log that names the file and the reason.

**About `base_yield`:** it is a percentage of the base production of the Cow Cage, which is 1000 mB of fluid every 20 seconds (400 ticks). `100` (the default) gives 1000 mB, `50` gives 500 mB, `200` gives 2000 mB per cycle. Accepted range: 1 to 800 (values outside it are clamped).

---

## Cooler Multiblock (End Game)

A 3x3 tower, from 3 to 10 blocks tall (`coolerMaxHeight`, up to 64), that cools many fluids at once. Sneak + right-click the Controller to show a hologram of the blocks still missing.

- **Cooler Casing**: the frame. All corners are Casing, as are the bottom and top layers (except for Ports or the Controller).
- **Cooler Controller**: one per tower, with its screen facing out. Right-click opens the GUI; sneak + right-click tells you what is wrong with an unfinished tower. Right-click it with a bucket to pour the fluid in.
- **Cooler Tank**: inner layers only, up to 8. Each Tank adds `coolerTankCapacity` mB (16000) to the tower's single tank, which holds any mix of fluids, stacked as layers in the GUI.
- **Cooler Stamp**: the centre of each inner layer. Each Stamp cools 2 ingots per cycle (`coolerCycleTicks`, 5 s) and uses `coolerEnergyPerStamp` RF/t (60) while working. Each cycle makes every ingot it can, sharing them among the fluids in turn.
- **Cooler Port**: right-click to switch between input (fluids and RF) and output (ingots, pushed into adjacent inventories).

A full 3x10x3 tower has 8 Stamps and cools 16 ingots every 5 seconds, four times a fully upgraded Advanced Cooler.

---

## Issues and Bug Reports

Found a bug or have a suggestion? We'd love to hear it! 
Please open an issue in the **[Issues Tab](../../issues)**. 

**Important:** Please ensure you select and fill out the provided Issue Template. Bug reports that don't follow the template may take much longer to resolve.
