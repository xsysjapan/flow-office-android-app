# flow-office Android打刻リーダー

既存の勤怠管理システム`flow-office`と連携し、NFCカードで出勤・退勤・休憩開始・休憩終了を
記録するAndroidアプリ。共有端末モードを初期版の完成対象とする。

## 最初に読むもの

実装・設計・レビューを始める前に、作業範囲に応じて次を読む。

1. `docs/android-punch-reader-design.md` — アーキテクチャ、API、画面、状態、セキュリティの正
2. `.claude/skills/android-ui-design-system/SKILL.md` — UIを作る、直す、レビューする場合
3. `.claude/skills/add-compose-screen/SKILL.md` — Compose画面・コンポーネントを追加する場合
4. 隣接リポジトリ`../flow-office` — API契約を変更・確認する場合

指示書の想定JSONと現行バックエンドが異なる場合、`flow-office`のController、Resource、Feature
Testを正とする。推測でAPI契約を変更しない。

## プロダクトの性格

このアプリは装飾的なコンシューマーアプリではなく、入口や受付で一日に何度も使われる業務端末である。
デザイン品質は、派手さではなく次で評価する。

- 迷わず打刻種別を選べる
- 離れた位置から状態を判別できる
- カードをかざした結果が瞬時に理解できる
- オンライン成功、端末保存、恒久失敗、認証失効を混同しない
- 長時間起動しても視覚的・操作的な疲労が少ない
- 色覚、聴覚、運動特性に依存せず操作できる
- 個人名や認証キーが不要に残らない

## 絶対に外してはいけない原則

1. **打刻イベントをAPI送信前にRoomへ保存する。** 送信中の終了・再起動でも打刻を失わない。
2. **同じ打刻の再送では同じ`idempotency_key`を使う。** 再送ごとにUUIDを作らない。
3. **端末認証と本人特定を分離する。** Bearer tokenは端末、NFC等のキーは社員を表す。
4. **Androidで勤怠計算をしない。** 勤務時間、残業、深夜、休日判定は`flow-office`側の責務。
5. **オフライン保存を打刻完了と表現しない。** 「端末に保存」「復旧後に自動送信」と明示する。
6. **401で未送信打刻を削除しない。** 再送を止め、管理者確認と再ペアリングへ誘導する。
7. **秘密・個人識別値をログへ出さない。** token、pairing code、完全なNFC UID、認証キーは禁止。
8. **UIからRetrofitやDAOを直接呼ばない。** ViewModel → UseCase → Repositoryを守る。
9. **API DTO、domain model、Room Entityを分ける。** 外部契約やDB都合をUIへ漏らさない。
10. **音・色だけで結果を伝えない。** 文字、アイコン、色、振動／音を組み合わせる。

## 技術構成

- Kotlin
- Jetpack Compose / Material 3
- ViewModel / Lifecycle / Navigation Compose
- Coroutines / Flow
- Hilt
- Retrofit / OkHttp / kotlinx.serialization
- Room
- WorkManager
- Android Keystore + 暗号化ストレージ
- Android NFC API

依存を追加する前に、標準Android APIまたは既存依存で実現できないか確認する。新しいUI部品は
Material 3に同等品があれば独自実装しない。

## パッケージと依存方向

```text
com.xsys.flowoffice.reader
├── app
├── presentation
│   ├── pairing
│   ├── punch
│   ├── settings
│   └── diagnostics
├── application
│   ├── pairing
│   ├── punch
│   ├── sync
│   └── heartbeat
├── domain
│   ├── device
│   ├── identity
│   ├── punch
│   └── error
├── data
│   ├── remote
│   ├── local
│   ├── repository
│   └── security
└── infrastructure
    ├── nfc
    ├── network
    ├── worker
    └── logging
```

依存方向は`presentation → application → domain`。`data`と`infrastructure`はdomainで定義した
interfaceを実装する。初期版は単一Gradleモジュールでよい。必要になる前に多モジュール化しない。

## デザイン言語

### 基本方針

- Material 3を土台に、業務端末らしい低装飾・高判読性・大きな操作面を採用する。
- 主要画面は「現在の状態 → 次に行う操作 → 結果」の順で情報階層を作る。
- 1画面の強い主役は1つにする。打刻画面では「選択中の打刻種別」と「NFC待受」が主役。
- すべてをCardで囲わない。背景、余白、Divider、見出しでまとまりを作る。
- 端末名、時計、通信状態、未送信件数は主操作を邪魔しないが常に確認できる位置へ置く。
- 成功表示は2〜3秒で待受へ戻し、次の利用者の個人名を残し続けない。

### デザイントークン

値はCompose Themeへ集約する。画面内で生の色や場当たり的な寸法を増やさない。

- 余白: 4dp単位、主要間隔は8 / 12 / 16 / 24 / 32dp
- 最小タップ領域: 48dp。打刻ボタンは原則56dp以上
- 角丸: 8 / 12 / 16dp。すべてをpill形状にしない
- 本文: 14〜16sp、主要ラベル: 16〜20sp、時計・結果は画面距離に応じて大きくする
- 色: `MaterialTheme.colorScheme`のsemantic colorだけを使う
- 影: 浮遊関係を示す場合だけ。通常のCardやButtonへ強い影を付けない
- アニメーション: 目的のある短い状態遷移だけ。常時点滅、跳ね、装飾ループは禁止

ライト／ダーク両方でコントラストを保つ。固定した白・黒・16進色をComposableへ直書きしない。

### アイコン

- Material Iconsを基本とし、同じ意味に同じアイコンを使う。
- 絵文字をUIアイコンとして使わない。
- アイコンだけのボタンには必ず`contentDescription`またはsemantics labelを付ける。
- 状態アイコンにはテキストラベルを併記する。

### 禁止する見た目

- 意味のないグラデーション、強い影、ガラス表現
- 原色の多用、色だけの状態区別
- 巨大なタイトルと不要なキャッチコピー
- すべての情報をCardで囲む構成
- 小さなタップ領域、薄すぎる文字、過剰な角丸
- モーダルの乱用
- 成功とオフライン保存が同じ緑色・同じ文言になる設計
- API例外や技術用語をそのまま利用者へ見せる設計

## 画面別の優先事項

### PairingScreen

- 開発ビルドだけAPI URLを編集可能にし、本番は固定値を表示する。
- 端末ID、8文字のペアリングコード、実行ボタンを明確な順序で置く。
- 入力エラーと通信エラーを分ける。
- pairing code表示中は必要に応じて`FLAG_SECURE`を使う。
- 成功後にtokenと端末設定の保存が両方完了するまでPunchScreenへ遷移しない。

### PunchScreen

- 4種の打刻ボタンは日本語ラベルを常に表示する。
- 選択中の種別は色だけでなく、枠・アイコン・「選択中」テキストで示す。
- NFC待受、処理中、成功、端末保存、エラー、認証失効を別のUiStateとして扱う。
- 未送信件数が0でない場合は常時確認できるが、通常操作を妨げない位置へ置く。
- NFC無効／非対応時は操作不能の理由と設定方法を表示する。
- 連続利用を想定し、結果表示後は選択中の種別を維持するか解除するかを仕様として固定する。

### Result表示

- 成功: 打刻種別、時刻、成功文。社員名はAPIで取得できた場合だけ短時間表示する。
- オフライン: 「端末に保存」「自動送信予定」を明記し、成功チェックだけを使わない。
- 恒久失敗: 利用者が次に行うことを示す。例「管理者にカード登録を依頼してください」。
- 401: 「端末認証が無効」「管理者確認」「再ペアリング」を明示する。

### Settings / Diagnostics

- 通常利用者の誤操作を避けるため、主画面から一段奥へ置く。
- token、pairing code、完全な認証キーを表示しない。
- 端末ID、端末名、アプリ版、最終通信、未送信／恒久失敗件数、NFC状態を表示する。
- 未送信がある場合はペアリング解除を禁止する。

## Compose実装規約

- Route Composableと表示専用Screen Composableを分ける。
- Screenはimmutableな`UiState`とcallbackだけを受け取る。
- ユーザー操作はsealed interfaceの`UiAction`または明示的callbackへ集約する。
- navigation、snackbar、音、振動など一度だけの副作用を永続UiStateへ混ぜない。
- `collectAsStateWithLifecycle()`を使い、Lifecycle外でFlowを収集し続けない。
- 文字列、色、寸法はresource／Themeへ置く。利用者向け文字列をComposableへ散在させない。
- Loading時も最終配置に近い領域を確保し、レイアウトシフトを抑える。
- `LazyColumn`のitemには安定したkeyを指定する。
- PreviewのためにAndroidサービス、NFC、RepositoryをComposable内で取得しない。

## 必須UI状態

新しい画面はHappy Pathだけで完了にしない。該当する状態を必ず設計・Preview・テストする。

- Initial / Idle
- Loading / Processing
- Content / Ready
- Empty
- Validation Error
- Network Error
- Offline
- Retryable Failure
- Permanent Failure
- Authentication Invalid
- Disabled
- NFC Unsupported / Disabled

## アクセシビリティ

- 主要操作をTalkBackで区別できるsemanticsにする。
- 選択状態は`selected`、無効状態は`disabled`など標準semanticsで伝える。
- 時計の毎秒更新でTalkBackを毎秒読み上げさせない。
- 結果通知は適切なlive regionを使うが、同じ内容を重複して読み上げない。
- font scale 1.3〜2.0でも主要操作が切れないことを確認する。
- 横向き、狭いスマートフォン、設置用タブレットで確認する。
- 色、音、振動のいずれか一つだけに依存しない。

## Previewとテスト

新しいScreenまたは再利用コンポーネントにはPreviewを付ける。

- Light / Dark
- 主要なUiState
- 長い端末名・長いエラーメッセージ
- 大きなfont scale
- 代表的なphone / tablet幅

テストは次を優先する。

- UiStateに対して必要な情報と操作が表示される
- 4種類の打刻を正しく選択できる
- offlineを成功と誤表示しない
- semantics role / label / selected / disabled
- 401、422、通信失敗の状態遷移
- 未送信ありの解除禁止

実装後は可能ならスクリーンショットまたは実機／エミュレータで視覚確認する。ビルド成功だけで
デザイン完了としない。

## API契約の要点

- Base path: `/api`
- Pairing: `POST /devices/pairing/exchange`、成功token項目は`token`
- Shared owner type: `organization_shared`
- Punch: `POST /device-punches`、成功は現行`AttendancePunchResource`
- Heartbeat: `POST /devices/heartbeat`、現行requestは`app_version`のみ
- Punch type: `clock_in`, `clock_out`, `break_start`, `break_end`
- 日時: offset必須ISO 8601
- 共有端末は`authentication_key_value`が必須

API変更を提案する場合は、Androidだけを先行変更せず`docs/android-punch-reader-design.md`の
「バックエンド改善候補」と`../flow-office`の実装を確認する。

## セキュリティ

- releaseはHTTPSだけを許可する。
- Bearer tokenはKeystore保護ストレージへ保存する。
- Roomはアプリ専用領域に置き、バックアップ除外と送信後の認証キー消去を行う。
- OkHttp body loggingを本番で有効にしない。
- Workerのinput dataへtokenや認証キーを入れず、local IDだけを渡す。
- crash report、analytics、診断表示へ秘密情報を送らない。

## 作業手順

1. 既存ファイル、設計書、関連Skillを読む。
2. UI作業では、目的・ユーザー・最重要操作・情報優先度・状態を短く整理する。
3. Themeと既存コンポーネントを再利用する。
4. UiState、Preview、テストを画面実装と同時に作る。
5. 関連するunit / UI testとGradle taskを実行する。
6. Light / Dark、狭幅、tablet、font scale、offline / errorを視覚確認する。
7. 実装と設計書がずれた場合は同じ変更で設計書も更新する。

リポジトリが未セットアップの段階ではGradleコマンドを推測してREADMEへ書かない。wrapperと
module構成を作成・確認してから、実際に通ったコマンドだけを記載する。

## Claude Code Agent Skills

- `android-ui-design-system` — UI設計、デザイン刷新、アクセシビリティ、視覚レビューで使用する。
- `add-compose-screen` — 新しいCompose画面・コンポーネントを実装する場合に併用する。

該当作業ではSkillを読み、チェックリストを満たす。ユーザーの明示指示がSkillと異なる場合は
ユーザー指示を優先し、設計上の影響を説明する。

