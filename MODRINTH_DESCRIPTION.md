## AE2CC Bridge Forge

A Forge port that allows **ComputerCraft** computers to read and interact with **Applied Energistics 2** ME storage systems.

### Features

- Read all items and fluids stored in your ME system
- Query craftable items
- Schedule crafting jobs directly from Lua
- Monitor crafting CPU status and progress in real-time

### Quick Start

1. Craft the Adapter block
2. Place it next to an AE2 ME Cable
3. Connect a ComputerCraft computer via wired modem
4. Run your Lua script

```lua
local ae2 = peripheral.wrap("right")
local items = ae2.getAvailableObjects()
ae2.scheduleCrafting("item", "minecraft:diamond", 64)
```

### Recipe

```
Iron  | Redstone | Iron
Redstone | Glass | Redstone
Iron  | Redstone | Iron
```

### Requirements

- Minecraft 1.20.1
- Forge 47.x
- Applied Energistics 2 (15.x)
- CC:Tweaked (1.120.x)

### Lua API

| Function | Description |
|---|---|
| `getAvailableObjects()` | List all items/fluids in ME system |
| `getCraftableObjects()` | List all craftable items |
| `getCraftingCPUs()` | Get crafting CPU info |
| `getIssuedCraftingJobs()` | Get pending/active jobs |
| `getAllCraftingRequests()` | Get all active crafting requests |
| `scheduleCrafting(type, id, amount)` | Schedule a crafting job |
| `getStatus()` | Check adapter connection status |

### License

MIT License

### Credits

- **Original:** [AE2CC Bridge](https://github.com/TheMrMilchmann/AE2CCBridge) by TheMrMilchmann
- **Forge fork:** [PowerAE2CC Bridge](https://github.com/GameModsBR/PowerAE2CCBridge) by GameModsBR
- **Maintainer:** [Shining](https://github.com/evervane)
- **Developed with:** [OpenCode](https://opencode.ai/) (AI Coding Assistant)
