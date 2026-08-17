package com.example.bytebufferlab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PayloadServiceTest {
    @Test
    void acceptsUtf8PayloadAndPersistsTheSameValue() {
        PayloadStore store = new PayloadStore();
        PayloadService service = new PayloadService(store);

        String actualBoundary = service.acceptAndPersist("注文-42");
        String actualFinalState = store.read();

        System.out.println("[evidence] input=注文-42");
        System.out.println("[evidence] boundary=" + actualBoundary);
        System.out.println("[evidence] final-state=" + actualFinalState);

        assertEquals("注文-42", actualBoundary);
        assertEquals("注文-42", actualFinalState);
    }
}
