<picture>
  <source media="(prefers-color-scheme: dark)" srcset="src/main/resources/plannhlogo4x.png">
  <img alt="PlanNH" src="src/main/resources/plannhlogo4x.png" width="606">
</picture>

In-game flowchart-based production planner for Minecraft 1.7.10 for the GTNH pack.

## Features

- **Canvas.** Infinite 2D whiteboard to organize complex recipe chain.
- **NEI Recipes.** Recipes can be added using NEI and are shown through the normal recipe widgets.
- **Groups.** Collapsible and customizable groups to keep the recipes organized
- **Sticky notes.** To provide extra context or to just draw a smily face
- **Production balancer.** You can balance chains for optimal throuput by pinning recipe nodes, uses [ojAlgo](https://github.com/optimatika/ojAlgo) to provide a clean solution 
- **Summary.** Shows the overall demands and outputs of a recipe chains, ranging from items, fluids, energy and even magic resources like mana
- **Mod integrations.** GregTech, EnderIO, Thaumcraft, Botania, Forestry, Et Futurum Requiem and many more to come
- **Machine configuration.** Mod-specific configuration options to calculate recipe throughput like voltage, overcloks or speed modifiers
- **Share and import.** Share your charts to other players either via the in-game chat or externally via the clipboard
- **Mermaid export.** Export graphs as Mermaid.js flowcharts for [GuideNH](https://github.com/GTNewHorizons/GuideNH).
- **Client side only.** No server binary required!

## Dependencies

- Minecraft **1.7.10** with Forge
- **Not Enough Items**, **ModularUI**, **GTNHLib**, **Mixin**

## Optional Offscreen Window

Press **F8** to open the planner in-game. [Offscreen](https://github.com/sbancuz/Offscreen) is optional and lets you open the planner in a separate window, which can be moved to another monitor.

To use the separate window in another modpack:

1. Install PlanNH and its dependencies in the client instance.
2. Download [offscreen-0.3-alpha.jar](https://github.com/sbancuz/Offscreen/releases/download/0.3-alpha/offscreen-0.3-alpha.jar) from the [Offscreen releases](https://github.com/sbancuz/Offscreen/releases) and put it in the instance's `mods/` folder. The `offscreen-0.3-alpha-dev.jar` in this repository's `libs/` folder is for development, not normal modpack installation.
3. Restart Minecraft, join a world, and press **F7** to toggle the separate planner window. Both keybinds can be changed in Minecraft's Controls menu.

## Credits

- **[TheYoingLad](https://github.com/TheYoingLad)** for the UI code 
- **[FlyToSpace](https://github.com/FlyToSpace)** for the logo design
- **[OrderedSet86](https://github.com/OrderedSet86)** for the production balancer

## Building

```bash
./gradlew build
```

## License

MIT — see [LICENSE](LICENSE).
