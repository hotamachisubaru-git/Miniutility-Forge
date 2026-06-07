# Miniutility Forge

Minecraft Forge 1.20.1 向けのサーバー用ユーティリティ mod です。

この mod はサーバー側だけに導入します。クライアント側の `mods` フォルダには入れないでください。メニューやゴミ箱などの GUI はバニラのコンテナ画面を使うため、参加者のクライアントに Miniutility を導入する必要はありません。

## 対応環境

- Minecraft: `1.20.1`
- Forge: `47.4.20` 以上、`48` 未満
- Java: `17`
- 任意依存: LuckPerms `5.5` 以上

## 導入

1. `build/libs/miniutility-1.20.1-47.4.20-1.0.0.jar` を Forge サーバーの `mods` フォルダへ配置します。
2. サーバーを起動します。
3. LuckPerms の prefix をチャット表示へ反映したい場合は、LuckPerms もサーバー側へ導入します。

クライアント側へこの mod を入れる必要はありません。`mods.toml` ではサーバー専用依存として定義し、クライアントの mod 一致チェックも無視する設定にしています。

## 機能

- `/menu` から開けるメインメニュー
- 死亡地点の記録とワープ
- エンダーチェストを開く
- ゴミ箱を開く
- どこでも作業台
- 経験値レベルの増減
- サバイバル / クリエイティブの切り替え
- クリーパー爆発によるブロック破壊の切り替え
- ニックネームの保存、表示、色変更
- LuckPerms prefix の表示切り替え
- ニックネーム DB の再読み込み

ニックネーム DB は `config/miniutility/nickname.db` に SQLite 形式で保存されます。

## コマンド

| コマンド | 説明 |
| --- | --- |
| `/menu` | Miniutility メニューを開きます。 |
| `/load` | ニックネーム DB を再読み込みします。 |
| `/prefixtoggle` | 自分の LuckPerms prefix 表示を切り替えます。 |
| `/prefixtoggle on` | 自分の LuckPerms prefix 表示を有効にします。 |
| `/prefixtoggle off` | 自分の LuckPerms prefix 表示を無効にします。 |
| `/miniutility menu` | Miniutility メニューを開きます。 |
| `/miniutility death` | 死亡地点メニューを開きます。 |
| `/miniutility enderchest` | エンダーチェストを開きます。 |
| `/miniutility crafting` | 作業台を開きます。 |
| `/miniutility trash` | ゴミ箱を開きます。閉じると中身は削除されます。 |
| `/miniutility creeper` | クリーパー保護を切り替えます。 |
| `/miniutility exp <amount>` | 経験値レベルを増減します。 |
| `/miniutility gamemode` | サバイバル / クリエイティブを切り替えます。 |
| `/miniutility nickname set <nickname>` | ニックネームを設定します。 |
| `/miniutility nickname colored <nickname>` | `&6` などの色コード付きニックネームを設定します。 |
| `/miniutility nickname color <color>` | 既存ニックネームの色を変更します。 |
| `/miniutility nickname input` | チャット入力でニックネームを設定します。 |
| `/miniutility nickname colorinput` | チャット入力で色付きニックネームを設定します。 |
| `/miniutility nickname remove` | ニックネームをリセットします。 |

ニックネームは 1 から 16 文字で、通常設定では空白を使えません。記号は `_` と `-` のみ使用できます。色付きニックネームでは `&6ほたまち` のような Minecraft のレガシーカラーコードを使えます。

## ビルド

```powershell
./gradlew.bat build --console=plain
```

生成物は `build/libs/` に出力されます。

開発用のサーバー起動は次のコマンドです。

```powershell
./gradlew.bat runServer --console=plain
```

VS Code の Forge 実行構成を生成する場合は次を実行します。

```powershell
./gradlew.bat genVSCodeRuns --console=plain
```

## ブランチ運用

- Forge 向け変更は `forge` ブランチにコミット、プッシュします。
- Paper 向け変更は Paper 版のブランチで扱います。
