# AE2CC Bridge Forge

[中文说明](README_zh_CN.md)

A Forge port of [AE2CC Bridge](https://github.com/TheMrMilchmann/AE2CCBridge) — allows [ComputerCraft](https://github.com/cc-tweaked/CC-Tweaked) computers to access [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) ME systems.

## Features

- Read all items/fluids stored in ME system
- Query craftable items
- Schedule crafting jobs from ComputerCraft
- Monitor crafting CPU status and progress
- Passive crafting event monitoring (ae2cc:network_crafting_update)

## Requirements

- Minecraft 1.20.1
- Forge 47.x
- [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) 15.x
- [CC:Tweaked](https://www.curseforge.com/minecraft/mc-mods/cc-tweaked) 1.120.x

## Building

1. Download the following jars and place them in the `libs/` folder:
   - `appliedenergistics2-forge-15.4.10.jar`
   - `cc-tweaked-1.20.1-forge-1.120.2.jar`

2. Build with Gradle:
   ```bash
   ./gradlew build
   ```

3. The output jar will be in `build/libs/`.

## Usage

1. Craft the Adapter block (recipe below)
2. Place it adjacent to an AE2 ME Cable
3. Connect a ComputerCraft computer via wired modem
4. In Lua:
   ```lua
   local ae2 = peripheral.wrap("right")
   local items = ae2.getAvailableObjects()
   local craftables = ae2.getCraftableObjects()
   ae2.scheduleCrafting("item", "minecraft:diamond", 64)
   ```

## Recipe

```
Iron  Redstone  Iron
Redstone  Glass  Redstone
Iron  Redstone  Iron
```

## Lua API

| Function | Description |
|---|---|
| `getAvailableObjects()` | Returns all items/fluids in ME system |
| `getCraftableObjects()` | Returns all craftable items |
| `getCraftingCPUs()` | Returns information about crafting CPUs |
| `getIssuedCraftingJobs()` | Returns pending and active crafting jobs |
| `getAllCraftingRequests()` | Returns all active crafting requests |
| `scheduleCrafting(type, id, amount)` | Schedules a crafting job (`type`: "item" or "fluid") |
| `getStatus()` | Returns adapter connection status |

### Events

| Event | Description |
|---|---|
| `ae2cc:network_crafting_update` | Fired when any crafting activity changes (every 1s) |
| `ae2cc:crafting_started` | Fired when your scheduled crafting job begins |
| `ae2cc:crafting_done` | Fired when your scheduled crafting job completes |
| `ae2cc:crafting_cancelled` | Fired when your scheduled crafting job fails |

**crafting_cancelled error codes:** `CANCELLED`, `CPU_NOT_FOUND`, `INCOMPLETE_PLAN`, `NO_CPU_FOUND`, `NO_SUITABLE_CPU_FOUND`, `CPU_BUSY`, `CPU_OFFLINE`, `CPU_TOO_SMALL`, `MISSING_INGREDIENT`

Example:
```lua
-- Passive monitoring (any crafting in the network)
while true do
    local event, data = os.pullEvent("ae2cc:network_crafting_update")
    if data and #data > 0 then
        for _, job in ipairs(data) do
            print(job.systemID .. " x" .. job.amount)
        end
    end
end

-- Scheduled job events
local jobID = ae2.scheduleCrafting("item", "minecraft:diamond", 64)
while true do
    local event, id, reason = os.pullEvent()
    if event == "ae2cc:crafting_started" and id == jobID then
        print("Crafting started!")
    elseif event == "ae2cc:crafting_done" and id == jobID then
        print("Crafting done!")
        break
    elseif event == "ae2cc:crafting_cancelled" and id == jobID then
        print("Failed: " .. (reason or "unknown"))
        break
    end
end
```

## Acknowledgments

- Original Fabric version: [AE2CC Bridge](https://github.com/TheMrMilchmann/AE2CCBridge) by [TheMrMilchmann](https://github.com/TheMrMilchmann)
- Forge fork: [PowerAE2CC Bridge](https://github.com/GameModsBR/PowerAE2CCBridge) by [GameModsBR](https://github.com/GameModsBR)

## License

MIT License - see [LICENSE](LICENSE) for details.
