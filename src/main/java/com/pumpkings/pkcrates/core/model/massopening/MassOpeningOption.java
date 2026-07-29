package com.pumpkings.pkcrates.core.model.massopening;

import java.util.Objects;

public class MassOpeningOption {

    public static final int ALL_AMOUNT = -1;

    private final int amount; // -1 for "all"

    public MassOpeningOption(int amount) {
        this.amount = amount;
    }

    public static MassOpeningOption of(int amount) {
        return new MassOpeningOption(amount);
    }

    public static MassOpeningOption all() {
        return new MassOpeningOption(ALL_AMOUNT);
    }

    public int getAmount() {
        return amount;
    }

    public boolean isAll() {
        return amount == ALL_AMOUNT || amount == Integer.MAX_VALUE;
    }

    public String getLabel() {
        return isAll() ? "Open All" : "x" + amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MassOpeningOption that = (MassOpeningOption) o;
        return amount == that.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return "MassOpeningOption{" +
                "amount=" + amount +
                '}';
    }
}
