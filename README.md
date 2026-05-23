# Fuck Friends

[English](#english) | [简体中文](#简体中文)

---

<h2 id="english">Fuck Friends (Fabric Mod)</h2>

A Server-side Fabric mod designed for multiplayer survival with friends. It perfectly balances the convenience of teleportation with the challenge of survival by limiting teleportation usage and punishing excessive deaths.

### ✨ Features
- **Teleportation Limit & Xaero's Minimap Compatibility**: Allows regular players to use the `/tp` command and perfectly integrates with the teleport function of Xaero's Minimap. However, teleportations are strictly limited (default is 6 times per cycle).
- **Death Penalty (Spectator Mode)**: If a player dies too many times within a cycle (default is 2 deaths), they are forced into Spectator Mode until the next reset.
- **Actionbar Countdown**: Players in Spectator Mode will see a real-time countdown timer in their action bar indicating when they will be revived.
- **Global Reset Cycle**: Automatically resets TP and Death counts every 10 minutes (configurable, based on absolute world time to prevent reset glitches upon server restart).
- **Auto-Respawn at Bed**: When the penalty ends, spectator players are automatically revived and teleported back to their personal bed/respawn anchor (or the world spawn if their bed is destroyed).
- **Server-Side Only**: You only need to install this mod on the server! Clients don't need to install anything.

### ⚙️ Configuration
The configuration file is located at `config/fuck_friends.json` and is automatically generated on the first run. You can fully customize the limits and chat messages.

```json
{
  "resetIntervalTicks": 12000, // 10 minutes = 12000 ticks
  "maxTpCount": 6,
  "maxDeathCount": 2,
  "messageReset": "§aTeleport and Death limits have been reset!",
  "messageTpLimitReached": "§cYou have reached your teleport limit!",
  "messageSpectatorMode": "§cYou reached the death limit! You are a spectator now.",
  "actionbarSpectatorTime": "§eTime until respawn: %02d:%02d"
}
```

---

<h2 id="简体中文">Fuck Friends (Fabric 模组)</h2>

专为好基友联机生存打造的 Fabric 服务端模组。在保留小地图传送便利性的同时，通过严格的次数限制和死亡惩罚机制，找回原汁原味的生存挑战！

### ✨ 核心特性
- **TP 限制与 Xaero 小地图完美兼容**：赋予普通玩家使用 `/tp` 指令的权限，并完美接管 Xaero 小地图自带的传送发包。但传送次数受到严格限制（默认每周期 6 次）。
- **死亡惩罚（旁观者模式）**：如果玩家在一个周期内死亡次数过多（默认超过 2 次），将被强制切换为旁观者模式（Spectator），直到下一个重置周期。
- **动态倒计时显示**：处于死亡惩罚（旁观者模式）的玩家，其物品栏上方（Actionbar）会实时显示距离复活的倒计时。
- **全局重置周期**：每 10 分钟（可配置，基于严格的世界绝对时间，重启服务器不会导致计时重置）清空所有人的 TP 和死亡次数。
- **自动床铺复活**：惩罚周期结束后，旁观者玩家会被自动恢复为生存模式，并精准传送到自己睡过的床或重生锚处（如果床被破坏则传回世界中心出生点）。
- **纯服务端模组**：只需要在服务器安装，客户端无需安装任何内容即可生效！

### ⚙️ 配置文件
配置文件位于 `config/fuck_friends.json`，初次启动时会自动生成，且默认包含中文提示。你可以自由修改各项参数和提示文本。

```json
{
  "resetIntervalTicks": 12000, // 10分钟 = 12000 ticks
  "maxTpCount": 6,             // 周期内最大传送次数
  "maxDeathCount": 2,          // 周期内最大死亡次数
  "messageReset": "§a传送和死亡限制已重置!",
  "messageTpLimitReached": "§c你到达了传送上限!",
  "messageSpectatorMode": "§c你到达了死亡上限!",
  "actionbarSpectatorTime": "§e重生时间: %02d:%02d"
}
