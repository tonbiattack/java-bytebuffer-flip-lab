# ByteBufferの書き込み後に読み取り位置を切り替えない不具合のデバッグ記録

## 対象の不具合

`PayloadService` は受け取った UTF-8 文字列を `ByteBuffer` に書き込み、読み取った値を境界結果として返し、同じ値を `PayloadStore` に保存する契約を持つ。入力 `注文-42` に対して、境界結果と操作後に独立して読み直した最終状態の両方が `注文-42` になるべきだが、バグ状態では両方とも空文字列になった。

| 観測点 | 期待値 | バグ状態の実際値 |
| --- | --- | --- |
| 境界結果 | `注文-42` | 空文字列 |
| 最終状態 | `注文-42` | 空文字列 |
| 既存動作 | UTF-8入力を同じ文字列として返し保存する | 文字列が失われる |

## 再現条件

バグ状態のコミットは `4c8d4056f3e2e934ecf30a795ec957964fc4ffd9` です。

```bash
git checkout 4c8d4056f3e2e934ecf30a795ec957964fc4ffd9
mvn --batch-mode -Dtest=PayloadServiceTest test
```

実測した出力は次のとおりです。

```text
[evidence] input=注文-42
[evidence] boundary=
[evidence] final-state=
expected: <注文-42> but was: <>
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
```

## 調査

| 確認対象 | 観測結果 | 判断 |
| --- | --- | --- |
| 入力 | `注文-42` がテストから渡される | 入力欠落ではない |
| 境界出力 | `boundary=` の後が空 | 下流保存だけの問題ではない |
| 最終状態 | `final-state=` の後が空 | 境界結果の空値がそのまま保存された |
| 例外 | 例外は発生せず、決定的に空文字列になる | タイミングや例外変換の仮説を除外 |
| 実装 | `put` の直後に `remaining()` と相対 `get` を呼んでいる | 書き込みモードから読み取りモードへ移行していない |
| 仕様 | `Buffer` は position を次に読み書きする位置、limit を読み書き対象の終端とする。`flip()` は limit を現在の position にし、position を 0 にして相対 get の準備をする | `flip()` 不足を直接原因として採用 |

`ByteBuffer.allocate(encoded.length)` と UTF-8バイト列の書き込み自体は成功している。`put` 後の相対読み取り長を `remaining()` から計算しているため、position が書き込み済み末尾、limit が容量のままという状態では残りが 0 になる。したがって、テストの失敗は保存処理や文字コード変換の偶発的な問題ではなく、`ByteBuffer` の状態遷移の不足で説明できる。

## 原因

直接原因は、書き込み後の `ByteBuffer` に対して `flip()` を呼ばず、書き込み用の position/limit のまま相対 `get` を行っていたことである。Java SE 21 の `Buffer` API は、`flip()` を「put または channel-read の後に相対 get の準備をする操作」と定義している。[1]

## 修正

修正前は `put` の直後に `remaining()` を評価していた。

```java
buffer.put(encoded);
byte[] readable = new byte[buffer.remaining()];
```

修正後は、書き込んだバイト列を読み取る前に `flip()` を1行追加した。

```java
buffer.put(encoded);
buffer.flip();
byte[] readable = new byte[buffer.remaining()];
```

修正コミットは `9495fc84b3ae7659181bc38fc3998123444c696c` である。`flip()` によって limit が書き込み済み位置に、position が 0 になるため、`remaining()` は読み取るべきバイト数を返し、相対 `get` が先頭から書き込み済みデータを取得できる。

## 回帰確認

```bash
git checkout main
mvn --batch-mode -Dtest=PayloadServiceTest test
mvn --batch-mode test
```

修正後の焦点化テストでは次の出力を得た。

```text
[evidence] input=注文-42
[evidence] boundary=注文-42
[evidence] final-state=注文-42
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

焦点化テストは境界結果と独立して読み直した最終状態を別々に検証している。修正はこの2つの観測点を同時に満たし、テストの入力契約を弱めていない。

## 設計上の制約

このラボは `ByteBuffer` の相対 `put` と相対 `get` の状態遷移に限定している。チャネル入出力、ダイレクトバッファ、複数スレッドからの共有、部分読み書き、可変長メッセージのフレーミングは扱わない。また、入力の UTF-8 エンコードは `ByteBuffer` の容量不足という別の不具合を混ぜないよう、エンコード後のバイト配列長で容量を確保している。
