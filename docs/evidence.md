# 調査メモ

## 事実

- 実行環境は OpenJDK 21.0.11 と Maven 3.8.7。
- 入力は `注文-42`。
- バグ状態の焦点化テストはコンパイル後に実行され、次を出力した。

```text
[evidence] input=注文-42
[evidence] boundary=
[evidence] final-state=
expected: <注文-42> but was: <>
```

- `PayloadService` は `ByteBuffer.allocate(encoded.length)` で確保し、相対 `put` 後に `buffer.remaining()` で読み取り長を決めている。`put` 後は position が書き込み済みバイト数、limit は容量のままであり、remaining は 0 になる。
- Java SE 21 の `Buffer` API は、position を次に読み書きする位置、limit を読み書き対象の終端として定義している。また `flip()` は limit を現在の position に設定し、position を 0 に戻して相対 get の準備をする、と説明している。

## 一次資料

[1] Oracle, “Buffer (Java SE 21 & JDK 21)”, https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/Buffer.html

[2] Oracle, “ByteBuffer (Java SE 21 & JDK 21)”, https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html
