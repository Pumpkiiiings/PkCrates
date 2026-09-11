package com.pumpkings.pkcrates.core.animation.script;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionEngineTest {

    @Test
    void evaluatesArithmeticVariablesAndFunctions() {
        Map<String, Double> variables = Map.of("time", 10.0, "radius", 2.0);

        double result = ExpressionEngine.evaluate(
                "cos(time * pi / 10) * radius + clamp(5, 0, 3)", variables, new Random(1));

        assertThat(result).isCloseTo(1.0, within(1.0E-9));
    }

    @Test
    void comparisonsDriveConditions() {
        Map<String, Double> variables = Map.of("winner.weight", 4.0, "time", 20.0);

        assertThat(ExpressionEngine.condition("winner.weight <= 5", variables, new Random(1))).isTrue();
        assertThat(ExpressionEngine.condition("time > 100", variables, new Random(1))).isFalse();
    }

    @Test
    void randomIsDeterministicForTheSessionSeed() {
        double first = ExpressionEngine.evaluate("random(-2, 4)", Map.of(), new Random(42));
        double second = ExpressionEngine.evaluate("random(-2, 4)", Map.of(), new Random(42));

        assertThat(first).isEqualTo(second).isBetween(-2.0, 4.0);
    }

    @Test
    void rejectsUnknownNamesAndDivisionByZero() {
        assertThatThrownBy(() -> ExpressionEngine.evaluate("runtime.exec(1)", Map.of(), new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpressionEngine.evaluate("1 / 0", Map.of(), new Random()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Division by zero");
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
