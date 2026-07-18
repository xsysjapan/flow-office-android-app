# flow-office-android-app

Android向けのflow-office打刻リーダーアプリです。

## 現在の実装

- package / application ID: `jp.co.xsys.flowoffice`
- Kotlin / Jetpack Compose / Material 3
- アクティベーション画面
- QRコードまたはclaim tokenによる端末アクティベーション
- `POST /api/devices/pairing/claim` による本トークンへの交換
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

管理画面が表示する次のJSON形式のQRコードを読み取ります。

```json
{
  "url": "https://example.jp/api/devices/pairing/claim",
  "claim_token": "一時トークン"
}
```

`url` に対して、`claim_token`をBearer tokenとして次のAPIを呼びます。リクエストボディはありません。

```text
POST /api/devices/pairing/claim
Authorization: Bearer <claim_token>
```

QRコードを使えない場合は、APIサーバーURLと管理画面からコピーしたclaim tokenを手入力できます。claim tokenは5分間有効で、一度交換すると再利用できません。成功レスポンスの`device.id`と本トークンを端末へ保存します。

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

flow-officeの`devices.id`はバックエンドが採番する整数で、claim tokenからサーバー側が対象端末を特定し、成功レスポンスの`device.id`として返します。Androidが初回起動時に生成するUUIDは別のアプリインスタンスIDです。端末側の補助識別子として保存し、打刻API送信時に`X-Flow-Office-App-Instance-Id`ヘッダーで送信します。

## 次の実装候補

- 打刻イベントのRoom保存
- WorkManagerによる未送信打刻の再送
- バックエンド側でアプリインスタンスIDを監査・再アクティベーションへ利用する場合の契約整備
