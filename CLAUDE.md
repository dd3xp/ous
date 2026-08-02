# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 模型更新

私有资产目录里面的json文件需要改一下纹理路径

## 自定义方块模型:两个运行时 geometry loader

### 思想

Blockbench 导出的模型不做任何预处理产物。bake 时由自定义 NeoForge geometry loader 运行时加工,同一份 model JSON 同时喂渲染、VoxelShape、物品显示,**无任何预生成文件**。

两个 loader 共用一套骨架([`CustomUnbakedGeometry`](neoforge/src/main/java/com/cahcap/neoforge/client/model/CustomUnbakedGeometry.java) 持有 raw JSON + 一个 [`BakeStrategy`](neoforge/src/main/java/com/cahcap/neoforge/client/model/BakeStrategy.java)),区别只在策略:

| loader key | 策略 | 适用 |
|---|---|---|
| `ous:cleanup` | [`SingleBlockStrategy`](neoforge/src/main/java/com/cahcap/neoforge/client/model/SingleBlockStrategy.java) | 单格模型,只剥 zero-rotation 后交 vanilla 烘焙 |
| `ous:split` | [`MultiblockStrategy`](neoforge/src/main/java/com/cahcap/neoforge/client/model/MultiblockStrategy.java) | 跨格多方块(元素坐标跨 `[−16, 32]`),额外做 per-cell 切片/镜像 + blockstate 分派 |

### 数据流

```
common/.../models/block/<name>.json   ← Blockbench 导出,根部带 "loader": "ous:split" 或 "ous:cleanup"
             │
             ├─► 客户端:CustomModelLoaders → CustomUnbakedGeometry.bake() → BakeStrategy
             │      两者都先走 ElementProcessing.stripZeroRotation
             │      cleanup: 直接 bakeSubset,再贴回 ctx.getTransforms()
             │      split:   clipToCell + mirror,每 (mirror,pos) 一份子集分别 bakeSubset,
             │               结果塞进 BakedSplitModel 的 [mirror][position][side] quad 表
             │
             └─► 服务端/两端:CustomVoxelShapes.loadFromModel(path)
                    读同一 JSON → 按 element 中心点归格 → 合成 per-cell VoxelShape
```

### 关键类

全部位于 `neoforge/src/main/java/com/cahcap/neoforge/client/model/`(没有 `split/` 子包,那是旧结构):

| 类 | 作用 |
|---|---|
| [`CustomModelLoaders`](neoforge/src/main/java/com/cahcap/neoforge/client/model/CustomModelLoaders.java) | 注册入口,注册 `ous:cleanup` 与 `ous:split` 两个 key |
| [`CustomUnbakedGeometry`](neoforge/src/main/java/com/cahcap/neoforge/client/model/CustomUnbakedGeometry.java) | 存 raw JSON + strategy,把 bake 委托给 strategy |
| [`BakeStrategy`](neoforge/src/main/java/com/cahcap/neoforge/client/model/BakeStrategy.java) | 策略接口 |
| [`SingleBlockStrategy`](neoforge/src/main/java/com/cahcap/neoforge/client/model/SingleBlockStrategy.java) | cleanup 策略 |
| [`MultiblockStrategy`](neoforge/src/main/java/com/cahcap/neoforge/client/model/MultiblockStrategy.java) | split 策略 |
| [`ElementProcessing`](neoforge/src/main/java/com/cahcap/neoforge/client/model/ElementProcessing.java) | 共用工具:`stripZeroRotation` / `bakeSubset` / `clipToCell` / mirror |
| [`BakedSplitModel`](neoforge/src/main/java/com/cahcap/neoforge/client/model/BakedSplitModel.java) | `getQuads(state, side, rand)` 按 state 查表,item 渲染时返回全模型 |
| [`CustomVoxelShapes.loadFromModel`](common/src/main/java/com/cahcap/common/util/CustomVoxelShapes.java) | (common) 读同一 model JSON 产出 VoxelShape;支持 `excludeGroups` 跳过 Blockbench 组 |

Loader 在 [`OusNeoForgeClient.registerGeometryLoaders`](neoforge/src/main/java/com/cahcap/neoforge/OusNeoForgeClient.java) 里调 `CustomModelLoaders.register(event)` 注册。

### 哪些模型走哪条路

`ous:split`(6 个方块): `cauldron`、`herb_cabinet`、`herb_vault`、`kiln`、`obelisk`、`workbench`

`ous:cleanup`(5 个模型文件,4 个方块): `herb_basket_floor`、`herb_basket_wall`、`herb_pot`、`incense_burner`、`shelf`

判断当前状态用 `grep -l '"loader"' common/src/main/resources/assets/ous/models/block/*.json`,不要靠记忆。

### 陷阱:bakeSubset 返回的模型变换是恒等的

`ElementProcessing.bakeSubset` 合成的模型只搬了 `elements` 和 `textures`,虽然声明了 `parent: minecraft:block/block` 但**没有任何地方解析父链**。所以它返回的 `BakedModel`,其 `ItemTransforms` 是**全恒等**(旋转 0、缩放 1、位移 0),不是 vanilla 默认值。

**任何调用 `bakeSubset` 的新代码,都必须自己把 `ctx.getTransforms()` 贴回去。** 否则物品在物品栏/手上/地上/头上全部变成满格大小、不倾斜。

贴回去时**必须同时覆盖两个方法**:

```java
@Override public ItemTransforms getTransforms() { return transforms; }

@Override
public BakedModel applyTransform(ItemDisplayContext ctx, PoseStack pose, boolean leftHand) {
    transforms.getTransform(ctx).apply(leftHand, pose);
    return this;
}
```

只覆盖 `getTransforms()` 无效 —— 物品渲染器走的是 `applyTransform()`,而 `BakedModelWrapper` 会把它转发给被包装的原模型,恒等变换又被塞回来。

历史: 该缺陷自 490a5bf 引入 `bakeSubset` 就存在,但当时唯一调用方 `MultiblockStrategy` 随后会覆盖回来,从未暴露;4f75814 新增的 `SingleBlockStrategy` 直接返回结果,缺陷才显现,cc8ea6e 修复。

### Blockstate JSON 形式

6 个走 split loader 的多方块 blockstate 退化成 4-5 条 variant:

```json
{
  "variants": {
    "formed=false": { "model": "ous:block/lumistone" },
    "facing=north,formed=true": { "model": "ous:block/cauldron" },
    "facing=south,formed=true": { "model": "ous:block/cauldron", "y": 180 },
    "facing=east,formed=true":  { "model": "ous:block/cauldron", "y": 90 },
    "facing=west,formed=true":  { "model": "ous:block/cauldron", "y": 270 }
  }
}
```

Position / mirrored / lit 等属性由 `BakedSplitModel.getQuads` 从 state 读,不进 blockstate 文件。`workbench` 无 `formed` 属性,只有 4 条 facing variant。

### 新增一个自定义模型方块要做的事

1. Blockbench 建模,导出到 `common/src/main/resources/assets/ous/models/block/<name>.json`
2. 修纹理路径(加 `ous:block/` 命名空间前缀)
3. **根部加 loader 标记**(与 `format_version` 同级):跨格多方块用 `"loader": "ous:split"`,单格用 `"loader": "ous:cleanup"`
4. 写 blockstate JSON(多方块模板见上;单格按普通方块写)
5. 写 item model JSON:`{ "parent": "ous:block/<name>" }`
6. Block 类 `private static final CustomVoxelShapes SHAPES = CustomVoxelShapes.loadFromModel("/assets/ous/models/block/<name>.json");`
7. 如果模型带 "配饰" 组(例如 incense 香条、rope)需排除出碰撞,给 `loadFromModel` 传第二参数 `Set.of("GroupName")`

**没有 Gradle task 要跑。**

### 陷阱:改碰撞箱必须重启客户端

`SHAPES` 是 `private static final`,在类加载时初始化一次。F3+T 只重载资源,**不重载类**。所以:

- 改**外观** → F3+T 就够
- 改**碰撞箱** → 必须**重启客户端**

调模型时如果"看起来变了但走上去手感没变",就是这个原因。

### display 段的注意事项

- 原版 1.21.1 只认 9 个 `ItemDisplayContext`: `none` / `gui` / `ground` / `fixed` / `head` / `firstperson_lefthand` / `firstperson_righthand` / `thirdperson_lefthand` / `thirdperson_righthand`。其余 key **静默忽略,不报错不警告**。
- 模型里的 `on_shelf` 是 Blockbench 导出的,原版无此 context,代码里也没注册对应的自定义 `ItemDisplayContext`,**目前不生效**。有意保留供以后使用,不要删。

### 什么还需要 runData

- loot 表、recipe、block/item/biome tag、worldgen (biome modifier) —— 都还在 NeoForge datagen 里,跑 `./gradlew :neoforge:runData`
- datagen 输出到 `neoforge/src/main/generated/resources`,该目录**已 gitignore**,但参与打包(`sourceSets.main.resources.srcDir`)。换 mod id 或大改内容后要重跑。
- 模型相关的四件事(剥 rotation、split、voxelshape、blockstate)**全部运行时完成**,无需 datagen 也无需 buildSrc task

### AO 光照格子历史

Blockbench 导出的 elements 含 `rotation: {angle: 0, ...}` 会让 Minecraft 走不同 bake 路径,产生 AO 不连续。两个 loader 都会在读入 JSON 后自动剥离这种 zero-angle rotation(见 `ElementProcessing.stripZeroRotation`),源 JSON 不需要手工清理。

## 联动 mod 依赖 (Jade / WTHIT / JEI)

项目为这三个 mod 写了兼容插件(`common/.../compat/jade`、`compat/wthit`、`neoforge/.../compat/jei`),所以它们必须出现在构建脚本里。版本号集中在 `gradle.properties` 的 `jade_version` / `wthit_version` / `jei_version`,三个模块引用同一变量。

每个 mod 都**成对声明**:`modCompileOnly` 拿 `-api` 包供编译,`modLocalRuntime` 拿完整包供开发环境运行时加载。

**不要改成"把发布版 jar 丢进 `neoforge/run/mods`"。** 那样绕过 Loom remap 拿不到 refmap,Jade 会在标题界面崩溃(`InvalidMixinException: @Shadow field this$0 was not located ... No refMap loaded` —— 它 `@Shadow` 了内部类的合成字段,而开发环境的 Minecraft jar 里没有这个字段名)。其余 mod 当时侥幸没崩,但那只是碰巧。

`neoforge/run/mods` 只放项目代码不引用的整合包 mod。该目录已 gitignore。

注意 WTHIT 按平台分发不同变体:根 `build.gradle` 给所有模块注入 `wthit-api:fabric-`,`neoforge/build.gradle` 用 `wthit-api:neo-`,版本号必须一致。

JEI 的插件写在 neoforge 模块**仅仅因为** JEI 的 API 只在 `neoforge/build.gradle` 里声明 —— `@JeiPlugin` 和 `IModPlugin` 其实都在 `jei-*-common-api` 包里,想搬到 common 只需给 common 加上依赖。
