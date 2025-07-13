/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TimerConfigTest {

    private TimerConfig timerConfig;

    @Test
    public void shouldPopulateErrorsWhenTimerSpecIsInvalid() {
        timerConfig = new TimerConfig("SOME JUNK TIMER SPEC", false);
        timerConfig.validate(null);
        assertThat(timerConfig.errors().firstError()).startsWith("Invalid cron syntax");
    }

    @Test
    public void shouldPopulateErrorsWhenTimerSpecIsNull() {
        timerConfig = new TimerConfig(null, true);
        timerConfig.validate(null);
        assertThat(timerConfig.errors().firstError()).isEqualTo("Timer Spec can not be null.");
    }

    @Test
    public void shouldNotPopulateErrorsWhenTimerSpecIsValid() {
        timerConfig = new TimerConfig("0 0 12 * * ?", false);
        timerConfig.validate(null);
        assertThat(timerConfig.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldCreateTimerGivenTheAttributeMapIfOnlyOnChangesIsCheckedInUI() {
        Map<String, String> mapOfTimerValues = new HashMap<>();
        mapOfTimerValues.put(TimerConfig.TIMER_SPEC, "0 0 * * * ?");
        mapOfTimerValues.put(TimerConfig.TIMER_ONLY_ON_CHANGES, "1");
        TimerConfig timer = TimerConfig.createTimer(mapOfTimerValues);
        assertThat(timer.getTimerSpec()).isEqualTo("0 0 * * * ?");
        assertThat(timer.shouldTriggerOnlyOnChanges()).isTrue();
    }

    @Test
    public void shouldCreateTimerGivenTheAttributeMapOnlyOnChangesIsNotPresent() {
        Map<String, String> mapOfTimerValues = new HashMap<>();
        mapOfTimerValues.put(TimerConfig.TIMER_SPEC, "0 0 * * * ?");
        TimerConfig timer = TimerConfig.createTimer(mapOfTimerValues);
        assertThat(timer.getTimerSpec()).isEqualTo("0 0 * * * ?");
        assertThat(timer.shouldTriggerOnlyOnChanges()).isFalse();
    }

    @Test
    public void shouldCreateTimerGivenTheAttributeMapIfOnlyOnChangesIsNotCheckedInUI() {
        Map<String, String> mapOfTimerValues = new HashMap<>();
        mapOfTimerValues.put(TimerConfig.TIMER_SPEC, "0 0 * * * ?");
        mapOfTimerValues.put(TimerConfig.TIMER_ONLY_ON_CHANGES, "0");
        TimerConfig timer = TimerConfig.createTimer(mapOfTimerValues);
        assertThat(timer.getTimerSpec()).isEqualTo("0 0 * * * ?");
        assertThat(timer.getOnlyOnChanges()).isFalse();
    }

}
