---
name: add-compose-screen
description: flow-office Android打刻リーダーへ新しいJetpack Compose画面、ダイアログ、主要UIコンポーネントを追加するとき、または既存画面を構造から作り直すときに使用する。UiState、Route/Screen分離、Material 3、Preview、semantics、Compose UIテスト、状態別の視覚確認までを一体で実施する。
---

# Compose画面を追加する

`android-ui-design-system`を併用し、実装・Preview・テストを同じ変更で完成させる。

## 1. 契約を確認する

1. `CLAUDE.md`を読む。
2. `docs/android-punch-reader-design.md`で対象画面、状態、API、セキュリティを確認する。
3. 既存のTheme、共通Composable、navigation、UiState、テストパターンを検索する。
4. APIに関係する場合はDTOと`../flow-office`のController、Resource、Feature Testを確認する。

実装がまだない場合でも、将来の名前やGradle taskを推測して既成事実にしない。

## 2. デザインメモを作る

着手前に次を短く整理する。

- 目的、利用者、最重要操作
- 情報の優先順位
- 画面構造
- 再利用するコンポーネント
- phone / tablet / orientationの差
- 必要なUiState
- TalkBackと大きなfont scaleへの対応
- 他画面と異なる判断、その理由

## 3. UiStateとActionを先に定義する

表示条件をComposable内の複数booleanへ散らさない。矛盾しないimmutable stateを作る。

```kotlin
@Immutable
data class PunchUiState(
    val deviceName: String,
    val selectedType: PunchType?,
    val connection: ConnectionState,
    val pendingCount: Int,
    val nfcState: NfcState,
    val operation: PunchOperationState,
)

sealed interface PunchUiAction {
    data class SelectType(val type: PunchType) : PunchUiAction
    data object OpenSettings : PunchUiAction
    data object Retry : PunchUiAction
}
```

一度だけのnavigation、snackbar、音、振動は永続UiStateと分離する。offline保存をsuccess stateへ
丸めない。

## 4. RouteとScreenを分ける

```kotlin
@Composable
fun PunchRoute(
    viewModel: PunchViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PunchScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun PunchScreen(
    state: PunchUiState,
    onAction: (PunchUiAction) -> Unit,
    modifier: Modifier = Modifier,
) { /* stateless UI */ }
```

- RouteでViewModel、Lifecycle、navigation、副作用を接続する。
- Screenはstateとcallbackだけで描画可能にする。
- Screen内でRepository、DAO、Retrofit、NFCサービスを取得しない。
- Modifierは公開Composableの最後付近に置き、呼び出し側へ適用余地を残す。

## 5. Material 3とThemeを使う

- `MaterialTheme.colorScheme`, `typography`, `shapes`を使う。
- 既存の共通Composableを先に再利用する。
- 標準部品に同等品があれば独自Button、Dialog、Snackbarを作らない。
- 画面固有の色、角丸、文字サイズを直接増やさない。
- タップ領域48dp以上、打刻ボタン56dp以上を確保する。
- 文字列はresourceへ置く。

## 6. 全状態を実装する

対象画面に該当する状態を省略しない。

- Idle / Content
- Loading / Processing
- Empty
- Validation Error
- Offline
- Retryable Failure
- Permanent Failure
- Authentication Invalid
- Disabled
- NFC Unsupported / Disabled

エラーには「何が起きたか」だけでなく「次に何をするか」を表示する。APIの内部messageは未知の
場合そのまま見せず、安全な定型文へ変換する。

## 7. Previewを同時に作る

Screenを実サービスなしで描画できるようにし、次のPreviewを追加する。

- Light / Dark
- 通常状態
- 処理中
- 成功
- オフライン保存
- 恒久失敗
- 401
- 長い文字列
- phone / tablet
- 大きなfont scale

Previewの重複が多い場合は`PreviewParameterProvider`でUiState fixtureを供給する。fixtureへtoken、
pairing code、完全な認証キーを入れない。

## 8. テストを同時に作る

Compose UI testで最低限次を確認する。

- 主要なUiStateが正しい見出し、説明、操作を表示する
- 操作が正しい`UiAction`を発火する
- 選択中ボタンにselected semanticsがある
- 無効操作にdisabled semanticsがある
- アイコンボタンをlabelで取得できる
- offline表示が成功表示と異なる
- 401で再ペアリング案内が出る
- 長い文字列でも主要操作が存在する

ViewModel testではstate transitionと一度だけのeffectを確認する。時刻、dispatcher、接続状態は
注入し、実時間や実ネットワークへ依存させない。

## 9. 視覚確認する

1. PreviewをLight / Darkで確認する。
2. phone portrait、tablet、必要ならlandscapeを確認する。
3. font scaleを上げる。
4. 長い端末名、未送信2桁以上、長いエラーを入れる。
5. TalkBackの読み順と重複読み上げを確認する。
6. NFC待受から結果、待受復帰までの状態遷移を確認する。

可能ならスクリーンショットを比較し、余白・整列・コントラスト・タップ領域を目視する。

## 完了チェックリスト

- [ ] 設計メモの目的・最重要操作・状態が実装へ反映されている
- [ ] Routeとstateless Screenが分離されている
- [ ] UIからRepository / DAO / Retrofitを直接呼んでいない
- [ ] Themeと既存コンポーネントを再利用している
- [ ] Happy Path以外の状態がある
- [ ] Light / Darkと主要UiStateのPreviewがある
- [ ] phone / tablet / 大きなfont scaleを確認した
- [ ] semanticsをlabel / role / selected / disabledで確認した
- [ ] Compose UI testとViewModel testが通る
- [ ] offline保存をサーバー成功と表現していない
- [ ] token、pairing code、認証キーが画面・Preview・ログにない
- [ ] 実装と設計書に差があれば設計書も更新した

