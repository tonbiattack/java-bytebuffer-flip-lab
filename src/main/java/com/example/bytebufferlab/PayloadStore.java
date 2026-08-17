package com.example.bytebufferlab;

public final class PayloadStore {
    private String value;

    public void save(String value) {
        this.value = value;
    }

    public String read() {
        return value;
    }
}
