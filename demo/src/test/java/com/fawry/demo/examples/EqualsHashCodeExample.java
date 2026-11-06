package com.fawry.demo.examples;

/** EqualsHashCode: a class that overrides equals() must also override hashCode(). */
class EqualsHashCodeExample {

    private int id;

    @Override
    public boolean equals(Object other) {
        return other instanceof EqualsHashCodeExample that && that.id == id;
    }
}
