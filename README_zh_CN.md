# AE2CC Bridge Forge

AE2CC Bridge 的 Forge 移植版本 —— 允许 ComputerCraft 电脑访问 Applied Energistics 2 ME 存储系统。

## 功能

- 读取 ME 系统中存储的所有物品与流体
- 查询可合成物品列表
- 从 ComputerCraft 安排合成任务
- 监控合成 CPU 状态与进度
- 被动合成事件监控（ae2cc:network_crafting_update）

## 前置需求

- Minecraft 1.20.1
- Forge 47.x
- [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) 15.x
- [CC:Tweaked](https://www.curseforge.com/minecraft/mc-mods/cc-tweaked) 1.120.x

## 编译

1. 下载以下 jar 文件并放入 `libs/` 目录：
   - `appliedenergistics2-forge-15.4.10.jar`
   - `cc-tweaked-1.20.1-forge-1.120.2.jar`

2. 使用 Gradle 编译：
   ```bash
   ./gradlew build
   ```

3. 输出文件位于 `build/libs/`。

## 使用方法

1. 合成适配器方块（配方见下）
2. 将方块放置在 AE2 ME 线缆旁边
3. 通过有线调制解调器连接 ComputerCraft 电脑
4. 在 Lua 中使用：
   ```lua
   local ae2 = peripheral.wrap("right")
   local items = ae2.getAvailableObjects()
   local craftables = ae2.getCraftableObjects()
   ae2.scheduleCrafting("item", "minecraft:diamond", 64)
   ```

## 合成配方

```
铁锭  红石  铁锭
红石  玻璃  红石
铁锭  红石  铁锭
```

## Lua API

| 函数 | 说明 |
|---|---|
| `getAvailableObjects()` | 获取 ME 系统中所有物品与流体 |
| `getCraftableObjects()` | 获取可合成物品列表 |
| `getCraftingCPUs()` | 获取合成 CPU 信息 |
| `getIssuedCraftingJobs()` | 获取已发出的合成任务 |
| `getAllCraftingRequests()` | 获取所有进行中的合成请求 |
| `scheduleCrafting(type, id, amount)` | 安排合成任务（type 为 item 或 fluid） |
| `getStatus()` | 检查适配器连接状态 |

### 事件

| 事件 | 说明 |
|---|---|
| `ae2cc:network_crafting_update` | 任何合成活动变化（每秒检测） |
| `ae2cc:crafting_started` | 你安排的合成任务开始 |
| `ae2cc:crafting_done` | 你安排的合成任务完成 |
| `ae2cc:crafting_cancelled` | 你安排的合成任务失败 |

**crafting_cancelled 错误码：** `CANCELLED`、`CPU_NOT_FOUND`、`INCOMPLETE_PLAN`、`NO_CPU_FOUND`、`NO_SUITABLE_CPU_FOUND`、`CPU_BUSY`、`CPU_OFFLINE`、`CPU_TOO_SMALL`、`MISSING_INGREDIENT`

示例：
```lua
-- 被动监听（监控网络中所有合成活动）
while true do
    local event, data = os.pullEvent("ae2cc:network_crafting_update")
    if data and #data > 0 then
        for _, job in ipairs(data) do
            print(job.systemID .. " x" .. job.amount)
        end
    end
end

-- 定时任务事件
local jobID = ae2.scheduleCrafting("item", "minecraft:diamond", 64)
while true do
    local event, id, reason = os.pullEvent()
    if event == "ae2cc:crafting_started" and id == jobID then
        print("合成已开始！")
    elseif event == "ae2cc:crafting_done" and id == jobID then
        print("合成已完成！")
        break
    elseif event == "ae2cc:crafting_cancelled" and id == jobID then
        print("合成失败：" .. (reason or "未知原因"))
        break
    end
end
```

## 致谢

- 原版 Fabric 版本：[AE2CC Bridge](https://github.com/TheMrMilchmann/AE2CCBridge) by [TheMrMilchmann](https://github.com/TheMrMilchmann)
- Forge 分支：[PowerAE2CC Bridge](https://github.com/GameModsBR/PowerAE2CCBridge) by [GameModsBR](https://github.com/GameModsBR)

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件。
