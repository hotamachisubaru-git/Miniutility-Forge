# Forge 1.20.1-47.4.20 移植メモ

このメモは、現在の Miniutility Paper プラグインを Minecraft Forge `1.20.1-47.4.20` 向けの mod として作り直すための作業指針です。

## 前提

- 現在の実装は `io.papermc.paper:paper-api` と Bukkit API に依存した Paper プラグインです。
- Forge 版は Paper/Bukkit 互換ではないため、`plugin.yml` や `JavaPlugin` は使えません。
- Forge 1.20.1 の公式配布ページでは `47.4.20` が Latest として公開されています。
- Forge mod のメタデータは `src/main/resources/META-INF/mods.toml` に置きます。
- Forge の Java エントリポイントは `@Mod("modid")` を付けたクラスです。
- Minecraft 1.20.1 / Forge 47.x 系は Java 17 をターゲットにします。現在の `pom.xml` は Java 25 なので、そのままでは対象がずれます。

参考:

- Forge 1.20.1 downloads: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html
- Forge mod files: https://docs.minecraftforge.net/en/1.20.x/gettingstarted/modfiles/
- Forge events: https://docs.minecraftforge.net/en/1.20.1/concepts/events/
- Forge lifecycle: https://docs.minecraftforge.net/en/1.20.x/concepts/lifecycle/
- Forge configuration: https://docs.minecraftforge.net/en/latest/misc/config/
- Forge screens: https://docs.minecraftforge.net/en/1.20.1/gui/screens/

## 推奨ブランチ

`README.md` の運用に合わせて、Forge 版は `forge` ブランチで作業します。

```powershell
git switch -c forge
```

既に `forge` ブランチが存在する場合:

```powershell
git switch forge
```

## ビルド構成

Forge 版は ForgeGradle を使うのが前提です。現在の Maven `pom.xml` は Paper 版用として残すか、Forge ブランチでは Gradle に置き換えます。

推奨構成:

```text
settings.gradle
build.gradle
gradle.properties
src/main/java/org/hotamachisubaru/miniutility/MiniutilityForge.java
src/main/resources/META-INF/mods.toml
src/main/resources/pack.mcmeta
```

`gradle.properties` の例:

```properties
mod_id=miniutility
mod_name=Miniutility
mod_group_id=org.hotamachisubaru
mod_version=1.20.1-47.4.20-1.0.0
minecraft_version=1.20.1
forge_version=47.4.20
java_version=17
```

`settings.gradle` の例:

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = 'https://maven.minecraftforge.net/' }
    }
}

rootProject.name = 'Miniutility'
```

`build.gradle` の最小例:

```gradle
plugins {
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}

group = mod_group_id
version = mod_version

base {
    archivesName = mod_id
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java_version as int)
}

minecraft {
    mappings channel: 'official', version: minecraft_version

    runs {
        client {
            workingDirectory project.file('run')
            mods {
                "${mod_id}" {
                    source sourceSets.main
                }
            }
        }

        server {
            workingDirectory project.file('run-server')
            args '--nogui'
            mods {
                "${mod_id}" {
                    source sourceSets.main
                }
            }
        }
    }
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"
    implementation 'org.xerial:sqlite-jdbc:3.51.3.0'

    compileOnly 'net.luckperms:api:5.5'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

processResources {
    inputs.properties([
            mod_id: mod_id,
            mod_name: mod_name,
            mod_version: mod_version,
            minecraft_version: minecraft_version,
            forge_version: forge_version
    ])

    filesMatching('META-INF/mods.toml') {
        expand inputs.properties
    }
}
```

SQLite JDBC を mod jar に同梱する場合は、単純な `implementation` だけでは不足することがあります。配布前に ForgeGradle の jar-in-jar または shadow の採用を決めて、実際の `mods` フォルダ環境で起動確認します。

## mods.toml

`plugin.yml` は削除し、Forge 用に `src/main/resources/META-INF/mods.toml` を作ります。

例:

```toml
modLoader="javafml"
loaderVersion="[47,)"
license="All Rights Reserved"
issueTrackerURL="https://github.com/hotamachisubaru/Miniutility/issues"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="hotamachisubaru"
description='''
Miniutility for Forge 1.20.1.
'''
displayTest="MATCH_VERSION"

[[dependencies.${mod_id}]]
modId="forge"
mandatory=true
versionRange="[47.4.20,48)"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.20.2)"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="luckperms"
mandatory=false
versionRange="[5.5,)"
ordering="AFTER"
side="BOTH"
```

`modId` は小文字英数字、`_`、`-` の範囲に寄せ、コード側の `@Mod` と必ず一致させます。ここでは `miniutility` を使います。

## pack.mcmeta

`src/main/resources/pack.mcmeta` の例:

```json
{
  "pack": {
    "description": "Miniutility resources",
    "pack_format": 15
  }
}
```

## エントリポイント

Paper 版:

```java
public final class Miniutility extends JavaPlugin {
    @Override
    public void onEnable() {
        ...
    }
}
```

Forge 版:

```java
package org.hotamachisubaru.miniutility;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(MiniutilityForge.MOD_ID)
public final class MiniutilityForge {
    public static final String MOD_ID = "miniutility";

    public MiniutilityForge() {
        MinecraftForge.EVENT_BUS.register(new ForgeEventHandlers());
    }
}
```

Forge の lifecycle event は並列実行されるものがあるため、ワールドやサーバーに触る初期化は `FMLCommonSetupEvent#enqueueWork` またはサーバー開始後イベントへ逃がします。

## API 置換表

| Paper / Bukkit | Forge 1.20.1 の候補 |
| --- | --- |
| `JavaPlugin#onEnable` | `@Mod` コンストラクタ、`FMLCommonSetupEvent`、`ServerStartingEvent` |
| `plugin.yml` commands | `RegisterCommandsEvent` + Brigadier |
| `Bukkit.getOnlinePlayers()` | `MinecraftServer#getPlayerList().getPlayers()` |
| `org.bukkit.entity.Player` | `net.minecraft.server.level.ServerPlayer` |
| `org.bukkit.Location` | `ServerLevel` + `BlockPos` または `GlobalPos` |
| `PlayerDeathEvent` | `LivingDeathEvent` で `ServerPlayer` を判定 |
| `EntityExplodeEvent` | `ExplosionEvent.Detonate` または爆発関連イベント |
| `AsyncChatEvent` | `ServerChatEvent` |
| `InventoryClickEvent` | Forge の `Menu` / `Screen` / `AbstractContainerMenu` |
| `Bukkit.createInventory` | `MenuType`、`AbstractContainerMenu`、`SimpleMenuProvider` |
| `ItemStack` / `Material` | `net.minecraft.world.item.ItemStack` / `Items` / `Blocks` |
| `GameMode` | `ServerPlayer#setGameMode(GameType)` |
| `Player#teleportAsync` | `ServerPlayer#teleportTo(ServerLevel, x, y, z, yaw, pitch)` |
| `Player#getEnderChest()` | `PlayerEnderChestContainer` を開く処理 |
| `JavaPlugin#getConfig()` | `ForgeConfigSpec` |
| `JavaPlugin#getDataFolder()` | `FMLPaths.CONFIGDIR`、`FMLPaths.GAMEDIR`、または server world data |
| Adventure `Component` | Minecraft native `net.minecraft.network.chat.Component` |

## 機能別の移植方針

### `/menu`

- `RegisterCommandsEvent` で `/menu`、`/load`、`/prefixtoggle` を登録します。
- Forge は Brigadier ベースなので、引数、補完、権限判定をコマンドツリーに移します。
- `/menu` はサーバー側で `ServerPlayer` だけに許可します。

### GUI

現在の GUI は Bukkit Inventory を仮想メニューとして使っています。Forge では `AbstractContainerMenu` と client 側 `Screen` の組み合わせに作り直します。

短期移植案:

- まず `/menu` をテキストコマンド群に分解して、機能を先に動かす。
- その後に Forge の Menu / Screen を実装して GUI を戻す。

GUI を一気に移植する場合:

- `DeferredRegister<MenuType<?>>` を追加します。
- server 側に `MiniutilityMenu` を作ります。
- client 側に `MiniutilityScreen` を作ります。
- `FMLClientSetupEvent` で `MenuScreens.register` を呼びます。
- ボタンクリックは client から server へ packet を送って処理します。

### 死亡地点にワープ

- `LivingDeathEvent` を購読します。
- `event.getEntity()` が `ServerPlayer` のとき、`GlobalPos` または `ResourceKey<Level>` + `BlockPos` を保存します。
- ワープ時は保存先の `ServerLevel` を取得し、`ServerPlayer#teleportTo` で移動します。
- 溶岩、水中、奈落付近の安全化は Bukkit の `Location#getBlock()` 相当を Forge の `BlockPos` と `Level#getBlockState` で判定します。

### 経験値制御

- Paper 版はチャット入力待ちで増減値を受け取っています。
- Forge 版ではまず `/miniutility exp <levels>` などの Brigadier コマンドへ移すのが簡単です。
- GUI 実装後に入力画面または packet 経由のフォームへ戻します。

### ゲームモード切り替え

- `ServerPlayer#gameMode.getGameModeForPlayer()` で現在値を見ます。
- `ServerPlayer#setGameMode(GameType.CREATIVE)` / `GameType.SURVIVAL` へ置き換えます。
- 権限は `CommandSourceStack#hasPermission` または LuckPerms 連携で判定します。

### クリーパーのブロック破壊防止

- `CreeperProtectionService` の状態管理はほぼ流用できます。
- 爆発イベントで entity が `Creeper` の場合、ブロック破壊対象を消すかイベントをキャンセルします。
- Forge の爆発イベントはキャンセル可否を確認してから処理します。キャンセル不可のイベントで `setCanceled` するとクラッシュ原因になります。

### エンダーチェスト

- Forge では Bukkit の `player.getEnderChest()` はありません。
- `ServerPlayer#getEnderChestInventory()` と menu provider を使って開く実装に置き換えます。
- Vanilla の Ender Chest menu 相当を再利用できるか確認します。

### ゴミ箱

- Bukkit Inventory の一時コンテナはそのまま使えません。
- Forge では `AbstractContainerMenu` 上に一時 `Container` を持たせます。
- 「捨てる」ボタン押下時に server 側で中身を破棄します。
- キャンセル時に戻すため、`TrashBoxSessionStore` 相当は `UUID -> NonNullList<ItemStack>` で保持します。

### ニックネーム変更

- データベース、バリデーション、`UUID -> nickname` の map は大部分を流用できます。
- `org.bukkit.entity.Player` 依存を `ServerPlayer` に変更します。
- 表示名は `ServerPlayer#setCustomName` だけでは tab list / chat 表示に十分でない可能性があるため、まず chat 表示の差し替えを優先します。
- チャット整形は `ServerChatEvent` で処理します。
- 色コードは Adventure ではなく Minecraft native `Component` へ変換する utility を作ります。

### LuckPerms

- LuckPerms API 自体は Forge 版でも使えますが、Bukkit adapter は使えません。
- 現在の `LuckPermsUtil.safePrefix(Player)` は `api.getPlayerAdapter(Player.class)` 依存なので、Forge 版では `UserManager` から UUID で user を取得する実装に変えます。
- LuckPerms が存在しない場合は今と同じく空 prefix で続行します。

### SQLite

- `NicknameDatabase` の SQL 文と保存形式は流用できます。
- `JavaPlugin#getDataFolder()` と `getConfig()` は使えないため、DB パスを Forge config または `FMLPaths.GAMEDIR` 配下から解決します。
- 既存 Paper 版の `nickname.db` を移行したい場合は、Forge 版起動時に旧パスからコピーする処理を別途用意します。

### UpdateChecker

- Paper scheduler や plugin lifecycle に依存している場合は、Forge の server tick / executor / lifecycle に置き換えます。
- Forge には update checker 用の仕組みもあるため、`mods.toml` の `updateJSONURL` を使うか、既存 checker を流用するかを決めます。

## パッケージ分割案

Paper と Forge を同一リポジトリで維持する場合、共通ロジックとプラットフォーム依存を分けると保守しやすくなります。

```text
org.hotamachisubaru.miniutility.common
  NicknameValidator
  CreeperProtectionService
  nickname persistence interfaces

org.hotamachisubaru.miniutility.forge
  MiniutilityForge
  ForgeCommands
  ForgeEventHandlers
  ForgeConfig
  ForgeNicknameManager
  ForgeMenus
```

最初から完全共通化しすぎるより、次の順で進めます。

1. Forge MDK 構成で空 mod を起動できるようにする。
2. コマンドだけを先に移植する。
3. 死亡地点、経験値、ゲームモード、クリーパー保護を移植する。
4. ニックネーム DB とチャット表示を移植する。
5. GUI、エンダーチェスト、ゴミ箱、作業台を移植する。
6. LuckPerms 連携と update checker を仕上げる。

## 置き換え対象ファイル

Forge ブランチで削除または置換するもの:

- `pom.xml`
- `dependency-reduced-pom.xml`
- `src/main/resources/plugin.yml`
- `src/main/java/org/hotamachisubaru/miniutility/Miniutility.java`
- `src/main/java/org/hotamachisubaru/miniutility/bootstrap/*`
- `src/main/java/org/hotamachisubaru/miniutility/registry/*`
- Bukkit/Paper API に直接依存する Listener / GUI / Command class

流用しやすいもの:

- `Nickname/NicknameValidator.java`
- `creeper/CreeperProtectionService.java`
- `GUI/TrashBoxSessionStore.java` の考え方
- `death/DeathLocationStore.java` の考え方
- `Nickname/NicknameDatabase.java` の SQL と schema

## 検証手順

開発環境:

```powershell
./gradlew genIntellijRuns
./gradlew runServer
```

ビルド:

```powershell
./gradlew clean build
```

サーバー検証:

1. `build/libs/*.jar` を Forge `1.20.1-47.4.20` サーバーの `mods` に入れる。
2. Java 17 でサーバーを起動する。
3. `mods.toml` が見つからない警告が出ていないことを確認する。
4. `/menu` または代替コマンドが登録されていることを確認する。
5. LuckPerms なしで起動できることを確認する。
6. LuckPerms ありで prefix 取得ができることを確認する。
7. ニックネーム DB が作成、読み込み、保存できることを確認する。
8. クリーパー爆発でブロックが壊れないことを確認する。
9. 死亡地点保存とワープを Overworld / Nether / End で確認する。

## 注意点

- Paper と Forge はイベント、コマンド、GUI、設定、プレイヤー表現が別物です。単純な import 置換では移植できません。
- Forge は client-only class と server/common class の分離が重要です。`net.minecraft.client.*` を common/server 側から直接参照しないようにします。
- GUI は client/server packet を伴うため、最後に移植するのが安全です。
- Forge のイベントはすべてキャンセルできるわけではありません。キャンセル処理の前に cancelable か確認します。
- 既存の Paper 版は Java 25 前提ですが、Forge 1.20.1 版は Java 17 に落とします。


## 現在の状況（2026-06-07 時点）

### 完了済み

- [x] 移植ドキュメントの作成（このファイル）
- [x] Forge 1.20.1-47.4.20 の公式情報確認
- [x] ビルド構成（build.gradle / settings.gradle / gradle.properties）の設計
- [x] mods.toml の設計
- [x] エントリポイントの設計
- [x] API 置換表の作成
- [x] 機能別の移植方針の策定
- [x] パッケージ分割案の検討

### 進行中

- [ ] orge ブランチの作成
- [ ] ForgeGradle 環境のセットアップ
- [ ] 空 mod の起動確認

### 未着手

- [ ] コマンドの移植（/menu, /load, /prefixtoggle）
- [ ] 死亡地点・経験値・ゲームモード・クリーパー保護の移植
- [ ] ニックネーム DB とチャット表示の移植
- [ ] GUI、エンダーチェスト、ゴミ箱、作業台の移植
- [ ] LuckPerms 連携と update checker の実装

### 現在の課題

- 現在のプロジェクト構造は Paper/Spigot プラグイン向け（pom.xml + org.bukkit 依存）
- Forge 版は Gradle ベースの別構成が必要
- Paper と Forge でイベント・コマンド・GUI・設定・プレイヤー表現が異なるため、単純な import 置換では移植不可
- GUI は client/server packet を伴うため、最後に移植するのが安全
