package com.fawry.demo.examples;

/** SimplifyBooleanReturn: return the condition itself instead of "return true" and "return false". */
class SimplifyBooleanReturnExample {

    boolean isEmpty(int count) {
        if (count == 0) {
            return true;
        } else {
            return false;
        }
    }
}
