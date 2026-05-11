# Web上でAPKをビルドする方法（GitHub Actions）

このプロジェクトには、GitHub上でAPKを自動ビルドする設定を同梱しています。
PCにAndroid StudioやAndroid SDKを入れなくても、GitHubのWeb画面だけで `debug APK` を作成できます。

## できること

- GitHub上でAndroidアプリをビルド
- `app-debug.apk` をArtifactsとしてダウンロード
- Android端末へAPKを転送してインストール

## 手順

### 1. GitHubで新しいリポジトリを作成

GitHubにログインして、新しいリポジトリを作成します。

例：

```text
PitchVideoPlayer
```

公開したくない場合は `Private` を選んでください。

---

### 2. このプロジェクト一式をアップロード

ZIPを解凍して、中身をGitHubリポジトリにアップロードします。

アップロード後、GitHub上で以下のファイルが見えていればOKです。

```text
.github/workflows/android-debug-apk.yml
app/build.gradle.kts
settings.gradle.kts
build.gradle.kts
```

---

### 3. Actionsを開く

GitHubリポジトリ画面の上部にある `Actions` を開きます。

初回はActionsを有効化する確認が出る場合があります。

---

### 4. 手動でビルド実行

左側から以下を選びます。

```text
Build Android Debug APK
```

右側の `Run workflow` を押します。

---

### 5. APKをダウンロード

ビルドが成功したら、実行結果画面の下に `Artifacts` が表示されます。

```text
PitchVideoPlayer-debug-apk
```

これをダウンロードすると、中にAPKが入っています。

通常は以下のようなファイル名になります。

```text
app-debug.apk
```

---

## Android端末へ入れる方法

1. APKをスマホへ転送
2. スマホでAPKを開く
3. `不明なアプリのインストール` を許可
4. インストール

これは開発・確認用のdebug APKです。
Playストア配布用には、別途release署名APKまたはAABを作る必要があります。

---

## ビルド設定ファイル

GitHub Actions用の設定はここです。

```text
.github/workflows/android-debug-apk.yml
```

中では以下を行っています。

```text
1. ソースコード取得
2. JDK 17セットアップ
3. Android SDKセットアップ
4. Gradle 8.9セットアップ
5. assembleDebug実行
6. APKをArtifactsにアップロード
```

---

## 注意

- GitHubにアップロードした時点で、push時にも自動ビルドされます。
- 手動実行したい場合は `workflow_dispatch` から `Run workflow` を使います。
- debug APKは動作確認用です。
- Playストア配布にはrelease署名設定が必要です。
