package com.pumpkings.pkcrates.core.animation.script;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptActionTest {

    private final ScriptAction action = new ScriptAction(
            "test", 10, 20, 3, null, ScriptAction.Type.TITLE, Map.of(), List.of());

    @Test
    void runsOnlyInsideRangeAtConfiguredInterval() {
        assertThat(action.runsAt(9)).isFalse();
        assertThat(action.runsAt(10)).isTrue();
        assertThat(action.runsAt(13)).isTrue();
        assertThat(action.runsAt(14)).isFalse();
        assertThat(action.runsAt(19)).isTrue();
        assertThat(action.runsAt(21)).isFalse();
    }

    @Test
    void progressIsClamped() {
        assertThat(action.progress(5)).isZero();
        assertThat(action.progress(15)).isEqualTo(0.5);
        assertThat(action.progress(30)).isEqualTo(1.0);
    }
}
