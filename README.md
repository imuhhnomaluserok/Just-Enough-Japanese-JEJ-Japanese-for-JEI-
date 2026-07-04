# JEJ - Just Enough Japanese Config

Forge 1.20.1 用MOD。JEIなど「ForgeConfigSpec」を使っているMODの設定を、**完全日本語のGUI**で
一覧・編集できます。おまけでイースターエッグの **Chaosモード** 付き。

## 主な機能

- **設定ハブ画面**: `ConfigTracker` に登録されている全MODのconfig(COMMON/CLIENT/SERVER)を一覧表示し、
  選ぶとそのMODの設定項目を直接編集できます(JEJ自身のModリスト「Config」ボタン、または
  キーバインド「JEJ設定ハブを開く」から開けます。デフォルトは未割り当てなので、
  コントロール設定から好きなキーに割り当ててください)。
- **完全日本語UI**: `en_us.json` にも `ja_jp.json` と同じ日本語文言を入れているので、
  ゲームの言語設定が英語のままでもJEJの画面は日本語で表示されます。
- **Chaosモード**: JEJ自身の設定(`config/jej-client.toml`)で `chaosMode = true` にすると、
  - Null値を入れようとした時のエラー、
  - 保存確認ダイアログ、

  などの文言が賑やかな言い回しに変わります。機能自体は通常モードと同じで、あくまで表示テキストのみが変わります。

## ビルド方法

1. `gradle.properties` の `forge_version` を、実際に使いたい 1.20.1 系Forgeのビルド番号に合わせて調整してください(現状 `47.3.0` を指定していますが、最新の安定版に変えてもらって問題ありません)。
2. プロジェクトルートで:
   ```
   ./gradlew build
   ```
   (Gradle Wrapperのjarが同梱されていないため、初回は `gradle wrapper` を実行するか、
   お手元のGradle 8系で直接ビルドしてください)
3. `build/libs/jej-1.0.0.jar` ができます。

## 実装メモ・既知の注意点(重要)

このMODの「他MODの設定を横断的に読み書きする」部分は、Forgeの内部API
(`ConfigTracker`, `ModConfig#getSpec()/getConfigData()`, `ForgeConfigSpec.ValueSpec`)
に直接アクセスしています。1.20.1 / Forge 47.x を前提に実装していますが、
これらは「公開されているが内部実装よりのAPI」なので、Forgeのマイナーバージョンによっては
メソッド名や挙動が微妙に異なる場合があります。

具体的には以下のファイルが該当します:

- `net.jej.util.ConfigWalker` — configツリーの読み書きの中核
- `net.jej.gui.ConfigHubScreen#populateList` — 全MOD一覧の取得

もしビルド時にこの周辺でエラーが出た場合は、お使いのForgeバージョンの
`ConfigTracker` / `ModConfig` / `ForgeConfigSpec.ValueSpec` のメソッドシグネチャを
確認し、該当箇所を調整してください。

また、以下は意図的に簡略化しています(必要に応じて拡張してください):

- リスト系(`List`/`ListValue`)の値編集には非対応です(表示上は文字列として扱われ、
  保存時にバリデーションで弾かれる可能性があります)。
- 数値の範囲(Range)チェックは `ValueSpec#test()` にそのまま委ねているため、
  画面上にスライダーやmin/max表示はありません。
- スクロールではなく「ページ送り」方式で項目を表示しています(1ページ6項目)。

## ファイル構成

```
JEJ/
├── build.gradle / gradle.properties / settings.gradle
├── src/main/resources/
│   ├── META-INF/mods.toml
│   └── assets/jej/lang/{ja_jp,en_us}.json
└── src/main/java/net/jej/
    ├── JEJMod.java                 … MODエントリーポイント
    ├── config/JEJConfig.java       … JEJ自身の設定(chaosMode等)
    ├── util/ChaosText.java         … 通常/Chaos文言の切り替え
    ├── util/ConfigWalker.java      … 他MODのForgeConfigSpecの読み書き
    └── gui/
        ├── ConfigHubScreen.java    … MOD一覧画面
        ├── ConfigEditScreen.java   … 設定編集画面
        └── SaveConfirmScreen.java … 保存確認ダイアログ
```
