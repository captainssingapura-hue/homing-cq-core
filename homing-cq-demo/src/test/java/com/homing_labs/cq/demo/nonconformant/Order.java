package com.homing_labs.cq.demo.nonconformant;

/**
 * The one class here that conforms: a record with a constant of an immutable type. No finding names it.
 */
public record Order(String id, int quantity) {

    static final int MAX_QUANTITY = 99;

    public Order {
        if (quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("An order is for at most " + MAX_QUANTITY + ", not " + quantity);
        }
    }
}
