# RPGItems-reloaded 镶嵌 + 等级功能计划

## 目标
- 为 RPGItem **实例**增加等级与镶嵌持久化（PersistentDataContainer）。
- 支持“容器物品 + 多镶嵌物品”组合触发：共享条件、依次处理 marker、按 trigger 依次触发 power（容器→镶嵌1→镶嵌2…）。
- 提供镶嵌 GUI 与管理员等级指令。

## 设计概要
### 1) 数据与配置
- **PDC（统一加 rgi/rpgitem 前缀）**
  - `rgi_item_level`：int，默认 1。
  - `rgi_sockets`：PDC 子容器，按槽位顺序存储镶嵌品 **RPGItem ID**（不保存 ItemStack）。
  - `rgi_instance_cache_key`：String/UUID，用于缓存“实例化后的组合物品”。
- **物品配置新增字段（RPGItem）**
  - 容器：`socketAcceptTags`(Set<String>)、`socketMaxWeight`(int)、`socketInsertLine`(int)。
  - 镶嵌品：`socketTags`(Set<String>)、`socketWeight`(int)、`socketMinLevel`(int)、`socketingDescription`(List<String>)。
  - 约束：
    - `socketAcceptTags` 为空表示 **不接受任何镶嵌**（默认）。
    - `socketTags` 为空表示 **不匹配任何 tag**。
    - 使用特殊 tag `ANY` 表示可匹配任意 tag。
  - 镶嵌品本身不支持自定义与等级（仅配置态）。
  - 等级描述：`leveldescription`（多段规则，见下）。

### 2) 等级描述 LevelDescription
- 规则包含：`level`、`operation`(replaceline/replacewhole)、`line`、`newlines`。
- **选择规则**：按 level 升序，选“最小的 level ≥ 当前等级”的规则；若无匹配则使用原描述。
- **替换规则**：
  - replaceline：从 `line` 起替换一行成 `newlines`（行号 0-based）。
  - replacewhole：忽略 `line`，整段描述替换为 `newlines`。
- 所有替换基于**配置的原 description**，不叠加上一次替换。

### 3) 镶嵌匹配与校验
- **标签匹配**：容器 `socketAcceptTags` 与镶嵌品 `socketTags` 交集非空则通过（含 `ANY` 特判）。
- **重量限制**：总重量 ≤ 容器 `socketMaxWeight`。
- **等级限制**：容器等级 ≥ 镶嵌品 `socketMinLevel`。

### 4) 运行期组合逻辑
- 组合顺序：容器 → 镶嵌1 → 镶嵌2…
- **conditions 共享**：组合后的条件集统一用于所有 power。
- **markers 依次处理**：用于更新物品元数据（如 Unbreakable、AttributeModifier）。
- **powers 依次触发**：按 trigger 依序执行。
- 通过 ThreadLocal 覆盖 `BasePropertyHolder.getItem()`，在触发期间让镶嵌品的 power/condition/marker 以**容器物品**为 `getItem()` 上下文。
- **实例缓存**：镶嵌/等级导致“实例化 RPGItem”时，创建并缓存组合后的 RPGItem，避免每次 update 重新构建。
  - 触发时机：玩家登入、镶嵌 GUI 修改完成、管理员调整等级。
  - 缓存键：PDC `instance_cache_key`（不存在则生成并写入）。
  - 无变化时直接复用缓存对象。

### 5) Lore/Description 动态生成
- 基于**原始 description**应用 LevelDescription。
- 再按 `socketInsertLine` 插入镶嵌品的 `socketingDescription`（按槽位顺序）。
- 仅显示 `socketingDescription`，不显示镶嵌品 power/armor lore。
- 默认关闭 power lore 与 armor lore（showPowerText/showArmourLore 默认 false）。
- 仅当需要时动态计算，避免无谓更新。

### 6) GUI
- 以箱子界面实现镶嵌操作（3 行箱子）：
  - 左侧 3x3 为“物品区”，仅中心槽可放容器物品，周围一圈为填充。
  - 右侧（第 4 列起）为镶嵌槽；最后一列为**单一状态提示物品**（lore 汇总所有状态）。
  - 放入非 RPGItem 或不满足条件的物品会被弹出并提示。
  - 修改后立即写入 PDC，并实时刷新容器物品预览。
  - 取回容器物品时立即关闭 GUI（一次 GUI 只处理一个物品）。

### 7) 指令
- 管理员：`/rpgitem level get/set <item> <level>`（作用于手持实例，必要时校验 <item>），**独立权限节点**。
- 管理员：`/rpgitems socket` 打开镶嵌 GUI，**独立权限节点**。

## 性能与风险
- 合并 powers/conditions/markers 与 lore 动态更新需要控制频率：仅在物品更新或 GUI 变更时重新计算。
- **分帧策略（按玩家）**：避免逐物品分帧；一次处理一个玩家时必须在 **同一 tick** 完整扫描其背包/护甲/主副手。
  - 使用玩家级队列：每 tick 处理上限 N 个玩家（去重、轮转），保证延迟可控且不爆卡。
  - 不做 Bukkit 相关异步（ItemStack/Inventory/Meta/PDC 必须主线程）。
- PlayerRPGInventoryCache 需识别镶嵌品中具有 tick/sneak 触发的 powers。

## 已确认的细节
1) Lore 行号 0-based。
2) GUI：3 行箱子；左侧 3x3 仅中心槽可放；右侧第 4 列起为镶嵌槽；最后一列为**单一状态提示物品**。
3) `socketAcceptTags` 为空=不接受；`socketTags` 为空=不匹配；`ANY` 为通配。
4) 镶嵌品仅存 ID；不保存 ItemStack。
5) Lore 仅显示 `socketingDescription`；默认不显示 power/armor lore。
