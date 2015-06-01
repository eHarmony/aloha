package com.eharmony.matching.notaloha;

public enum AnEnum {
    VALUE_2(2),
    VALUE_3(3);

    private final int number;

    private AnEnum(final int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
