# GitHub Releases への APK リリース

`v1.2.3` のようなタグをpushすると、GitHub Actionsがテスト、署名済みrelease APKのビルド、署名検証を行い、GitHub Releaseを自動作成します。ReleaseにはAPKとSHA-256チェックサムが添付されます。

## 1. リリース用キーストアを作成する

初回のみ、リポジトリの外にキーストアを作成します。以後のアップデートでも同じキーストアを使用するため、安全な場所にバックアップしてください。

```powershell
keytool -genkeypair -v -keystore flow-office-release.jks -alias flow-office -keyalg RSA -keysize 4096 -validity 10000
```

キーストアをBase64文字列に変換します。

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("flow-office-release.jks")) | Set-Clipboard
```

## 2. GitHub Actions Secretsを登録する

GitHubリポジトリの `Settings` → `Secrets and variables` → `Actions` に次のRepository secretsを登録します。

| Secret | 値 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 上でコピーしたBase64文字列 |
| `ANDROID_KEYSTORE_PASSWORD` | キーストアのパスワード |
| `ANDROID_KEY_ALIAS` | キーのエイリアス（例: `flow-office`） |
| `ANDROID_KEY_PASSWORD` | キーのパスワード |

キーストア本体やパスワードはGitへコミットしないでください。

## 3. リリースする

リリースしたいコミットでバージョンタグを作成し、pushします。

```powershell
git tag -a v1.0.0 -m "v1.0.0"
git push origin v1.0.0
```

`Actions` タブの `Release APK` が成功すると、`Releases` に次のファイルが公開されます。

- `flow-office-1.0.0.apk`
- `flow-office-1.0.0.apk.sha256`

タグは `v<major>.<minor>.<patch>` を基本とし、`v1.0.0-rc.1` のような接尾辞も使用できます。接尾辞に `-` を含むタグはGitHub上でもプレリリースとして作成されます。APKの `versionName` はタグから先頭の `v` を除いた値、`versionCode` はGitHub Actionsの連番になります。

> [!IMPORTANT]
> リリース済みのタグを削除・再利用せず、修正時は新しいバージョンタグを作成してください。また、キーストアを失うと同じ署名でアップデート版を配布できなくなります。
