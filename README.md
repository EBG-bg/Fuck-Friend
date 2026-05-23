# Fuck Friends

[English](#english) | [简体中文](#简体中文)

---

<h2 id="english">Fuck Friends (Fabric Mod)</h2>

A mod created to prevent (noob) friends from abusing server commands while playing together on a multiplayer server.

### Please Note!!!: This mod was coded ENTIRELY by AI! (Cline + Gemini 3.1)
This mod was created solely to solve the issues I encountered while playing with my friends. It is only guaranteed to work on Fabric 1.21.1 servers. There is no guarantee of code safety or perfect optimization (as long as it works, it works). I'm just sharing this to provide a possible solution for other hosts whose friends constantly abuse server powers. 
Other Minecraft versions can be adapted using AI coding tools as well. 
If you find any bugs, what you should do is open your AI coding tool instead of submitting an issue here (solve AI problems with AI, they say).
Oh, and this README was also written by AI, I just modified it a bit (facepalm).

### ✨ Core Features (OPs ignore these limits)
- **TP Limit & Xaero's Minimap Compatibility**: Grants regular players permission to use the `/tp` command and is compatible with Xaero's Minimap teleportation requests. However, teleportations are strictly limited (default is 6 times per cycle).
- **Death Penalty (Spectator Mode)**: If a player dies too many times within a cycle (defaults to more than 2 deaths, meaning upon the 3rd death), they will be forcibly switched to Spectator Mode until the next reset cycle.
- **Dynamic Countdown Display**: Players serving the death penalty (in Spectator Mode) will see a real-time countdown timer above their hotbar (Actionbar) indicating the time until revival.
- **Global Reset Cycle**: The cycle is set to 10 minutes (configurable) and is based on absolute world time. Restarting the server won't reset the timer itself, but it will refresh everyone's available TP and Death counts.
- **Auto-Respawn at Spawn Point**: Once the penalty cycle ends, spectator players will automatically revert to Survival Mode and be teleported to their personal respawn point (or the world spawn if their respawn point is destroyed).
- **Server-Side Only**: You only need to install this mod on the server! Clients don't need to install anything for it to work.

### ⚙️ Configuration
The configuration file is located at `config/fuck_friends.json` and is automatically generated on the first run with Chinese prompts by default. You can freely modify the parameters and prompt texts.

---

<h2 id="简体中文">Fuck Friends (Fabric 模组)</h2>

一个为了防止和好友在服务器游玩时(萌新)好友滥用权力的模组

### 请注意!!!: 此模组为!纯AI!编写模组 (cline+gemini3.1)
   仅用来解决我在和朋友玩的过程中遇到的问题 仅保证fabric服务端1.21.1版本的可用性不保证安全性等东西(能用就行) 纯分享以提供可能的一种解决方案给同样被朋友不断滥用权力的主机 
   其它版本可同样使用ai编程工具修改 
   如果你发现了任何bug你应该做的是打开你的ai编程工具而不是在这里提交issue(原汤化原食说是)
   readme也是ai写的我改了点说是(捂脸‍)


### ✨ 核心特性(op权限玩家可无视)
- **TP 限制与 Xaero 小地图兼容**：赋予普通玩家使用 `/tp` 指令的权限，并兼容 Xaero 小地图自带的传送发包。但传送次数受到严格限制（默认每周期 6 次）。
- **死亡惩罚（旁观者模式）**：如果玩家在一个周期内死亡次数过多（默认超过 2 次,即第三次死亡时），将被强制切换为旁观者模式（Spectator），直到下一个重置周期。
- **动态倒计时显示**：处于死亡惩罚（旁观者模式）的玩家，其物品栏上方（Actionbar）会实时显示距离复活的倒计时。
- **全局重置周期**：每 10 分钟（可配置）周期时间基于世界时间，重启服务器不会导致计时重置但会重置所有人可用的 TP 和死亡次数。
- **自动床铺复活**：惩罚周期结束后，旁观者玩家会被自动恢复为生存模式，并传送到自己的重生点（如果重生点被破坏则传回世界中心出生点）。
- **纯服务端模组**：只需要在服务器安装，客户端无需安装任何内容即可生效！

### ⚙️ 配置文件
配置文件位于 `config/fuck_friends.json`，初次启动时会自动生成，默认为中文提示。你可以自由修改各项参数和提示文本。
