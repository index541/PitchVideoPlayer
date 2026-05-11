# Pitch Video Player

Android向けの「アプリ内動画/音声プレイヤー + ピッチ変更」サンプル実装です。

## この版でできること

- スマホ内の動画/音声ファイルを開く
- 直接再生できるURLを開く
  - MP4
  - M3U8/HLS
  - MP3/AACなど、ExoPlayerが対応するメディア
- ピッチ変更
  - -12.0半音 ～ +12.0半音
  - 0.1半音単位
- 再生速度変更
  - 0.50x ～ 2.00x
- 音量調整
- YouTube / ニコニコ動画のWebView表示

## 重要な制限

YouTube / ニコニコ動画は、この版では公式ページをWebViewで表示するだけです。
WebView内で再生される音声は、アプリ内プレイヤーのピッチ変更対象外です。

理由：

- 公式埋め込みプレイヤーやWebView再生では、アプリ側が生の音声PCMを直接取得できない
- YouTubeなどの動画音声を非公式に抽出・保存・加工する実装は、規約や公開アプリ運用上のリスクが高い

そのため、このプロジェクトでは安全な設計として、
「ピッチ変更できるプレイヤーモード」と「公式サービス表示用WebViewモード」を分けています。

## 開き方

1. Android Studioを起動
2. `PitchVideoPlayer` フォルダを開く
3. Gradle Syncを実行
4. Android端末またはエミュレーターで実行

## 推奨環境

- Android Studio Ladybug以降
- JDK 17
- Android SDK 35
- Android 7.0以上の端末

## ファイル構成

```text
PitchVideoPlayer/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ README.md
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/kento/pitchvideoplayer/MainActivity.java
      └─ res/
         ├─ values/
         │  ├─ strings.xml
         │  ├─ colors.xml
         │  └─ styles.xml
         └─ drawable/ic_launcher_foreground.xml
```

## 今後の拡張案

### 1. 高音質DSP化

現在はMedia3 ExoPlayerの `PlaybackParameters` を使っています。
さらに音楽用途で高品質にする場合は、C++/NDKでRubber BandなどのDSPエンジンを組み込む構成にします。

### 2. 歌詞・音程バー

- LRC読み込み
- 再生位置に同期した歌詞表示
- MIDI音程バー表示
- マイク音程検出
- オクターブ補正

### 3. 常駐操作

- 通知コントローラー
- クイック設定タイル
- MediaSessionService対応

### 4. ニコニコ動画の正式Provider化

利用規約上問題ない公式API/正式な再生URL取得方法が使える場合のみ、Providerとして分離実装します。

## 主要な調整値

`MainActivity.java` の上部にまとめています。

```java
private static final float PITCH_MIN_SEMITONE = -12.0f;
private static final float PITCH_MAX_SEMITONE = 12.0f;
private static final float SPEED_MIN = 0.50f;
private static final float SPEED_MAX = 2.00f;
private static final int SLIDER_SCALE = 10;
```

## 注意

このプロジェクトは、最初に安定して動かすためのMVPです。
公開アプリ化する場合は、以下を追加してください。

- エラー画面の強化
- 再生履歴保存
- 端末別の表示調整
- バックグラウンド再生
- 利用規約・プライバシーポリシー

## Web上でAPKをビルドする場合

この版にはGitHub Actions用のクラウドビルド設定を同梱しています。

```text
.github/workflows/android-debug-apk.yml
```

GitHubにプロジェクトをアップロードして、Actionsから `Build Android Debug APK` を実行すると、APKがArtifactsとして出力されます。
詳しい手順は以下を参照してください。

```text
CLOUD_BUILD_GITHUB_ACTIONS.md
```
