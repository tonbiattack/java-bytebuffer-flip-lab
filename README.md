# Java ByteBuffer flip デバッグラボ

Java 21 と JUnit 5 を使い、`ByteBuffer` へ書き込んだ後に `flip()` を呼ばず読み取ると、境界結果と保存状態が空になる不具合を、失敗テストから調査・修正する最小再現プロジェクトです。

## 前提条件

Java 21、JDK、Maven 3.8 以降を使用します。確認コマンドは次のとおりです。

```bash
java -version
javac -version
mvn --version
```

## 実行方法

修正済みの既定ブランチで全テストを実行します。

```bash
mvn --batch-mode test
```

バグ状態を再現する場合は、バグコミットへ移動して焦点化テストを実行します。

```bash
git checkout 4c8d4056f3e2e934ecf30a795ec957964fc4ffd9
mvn --batch-mode -Dtest=PayloadServiceTest test
```

バグ状態では、入力 `注文-42` に対する境界結果と最終状態が空文字列になり、テストが `expected: <注文-42> but was: <>` で失敗します。修正済みの `main` では同じテストが成功し、両方の観測点が `注文-42` になります。

## デバッグ記録

調査の仮説、実測ログ、原因、最小修正、回帰確認、制約は [`docs/debugging-record.md`](docs/debugging-record.md) にまとめています。

## コミット履歴

| 役割 | コミット |
| --- | --- |
| バグ再現 | `4c8d4056f3e2e934ecf30a795ec957964fc4ffd9` |
| 最小修正 | `9495fc84b3ae7659181bc38fc3998123444c696c` |

## ライセンス

学習用サンプルとして提供します。
