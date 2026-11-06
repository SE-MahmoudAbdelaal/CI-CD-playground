package com.fawry.demo.examples;

/** StringLiteralEquality: compare strings with equals(), because "==" compares object identity. */
class StringLiteralEqualityExample {

    boolean isAdmin(String name) {
        return name == "admin";
    }
}
