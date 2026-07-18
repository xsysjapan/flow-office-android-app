# flow-office-android-app

Android向けのflow-office打刻リーダーアプリです。

## 現在の実装

- package / application ID: `jp.co.xsys.flowoffice`
- Kotlin / Jetpack Compose / Material 3
- アクティベーション画面
- `POST /api/devices/pairing/exchange` による端末アクティベーション
- アクティベーション失敗時の画面エラー表示
- 成功時の端末トークン保存
- Android Keystore AES-GCMによるトークン暗号化
- 初回起動時に生成するアプリインスタンスID
- NFC Reader ModeによるカードUID読み取り
- 4種類の打刻種別選択
- `POST /api/device-punches` による打刻送信
- Debugビルドのみcleartext HTTPを許可
- NFC UID正規化ロジックと単体テスト

## アクティベーション

画面で入力したAPIサーバーURL、端末ID、8文字のアクティベーションコードを使い、次のAPIを呼びます。

```text
POST /api/devices/pairing/exchange
```

APIサーバーURLは `https://example.jp` と `https://example.jp/api` の両方を受け付けます。前者の場合は `/api/devices/pairing/exchange`、後者の場合は `/devices/pairing/exchange` を呼びます。

## ビルドとテスト

PowerShellから実行します。

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest
```

debug APKは次に生成されます。

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 端末識別子

AndroidのハードウェアID、IMEI、シリアル、MACアドレスは使用しません。Android Developersの識別子ベストプラクティスに従い、このアプリでは初回起動時にアプリ専用のUUIDを生成して内部ストレージに保存します。

現行のflow-office APIは管理画面で発行した数値の端末IDを必要とするため、アクティベーション時の端末ID入力は残しています。アプリインスタンスIDは端末側の補助識別子として保存し、打刻API送信時に `X-Flow-Office-App-Instance-Id` ヘッダーで送信します。

## 次の実装候補

- 打刻イベントのRoom保存
- WorkManagerによる未送信打刻の再送
- バックエンド側でアプリインスタンスIDによる端末登録・再アクティベーションに対応
