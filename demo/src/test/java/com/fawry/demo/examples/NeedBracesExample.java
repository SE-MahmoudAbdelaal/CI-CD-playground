package com.fawry.demo.examples;

/** NeedBraces: an "if" body needs braces, even when it is a single statement. */
class NeedBracesExample {

    int limit(int quantity) {
        if (quantity > 10) return 10;
        return quantity;
    }
}
