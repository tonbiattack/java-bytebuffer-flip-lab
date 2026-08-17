package com.example.bytebufferlab;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class PayloadService {
    private final PayloadStore store;

    public PayloadService(PayloadStore store) {
        this.store = store;
    }

    public String acceptAndPersist(String payload) {
        byte[] encoded = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(encoded.length);
        buffer.put(encoded);

        // BUG: the buffer is still in write mode. Reading starts at position == limit.
        byte[] readable = new byte[buffer.remaining()];
        buffer.get(readable);
        String normalized = new String(readable, StandardCharsets.UTF_8);

        store.save(normalized);
        return normalized;
    }
}
