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

## 次の実装候補

- アクティベーション成功後の打刻画面への遷移
- NFC Reader Mode
- 打刻イベントのRoom保存
- `POST /api/device-punches` 送信
- WorkManagerによる未送信打刻の再送
