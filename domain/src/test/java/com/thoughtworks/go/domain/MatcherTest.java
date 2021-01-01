/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MatcherTest {
    @Test
    void shouldRemoveTheLastEmptyMatcher() {
        assertThat(new Matcher("JH,Pavan,")).isEqualTo(new Matcher("JH,Pavan"));
    }

    @Test
    void shouldAllowWhiteSpace() {
        assertThat(new Matcher("JH Pavan").toCollection().size()).isEqualTo(1);
    }

    @Test
    void shouldRemoveDuplication() {
        assertThat(new Matcher("JH,JH")).isEqualTo(new Matcher("JH"));
    }

    @Test
    void shouldRemoveTheFirstEmptyMatcher() {
        assertThat(new Matcher(",JH,Pavan")).isEqualTo(new Matcher("JH,Pavan"));
    }

    @Test
    void shouldRemoveTheEmptyMatcherInTheMiddle() {
        assertThat(new Matcher("JH,,Pavan")).isEqualTo(new Matcher("JH,Pavan"));
    }

    @Test
    void shouldReturnCommaSplittedString() {
        assertThat(new Matcher("JH,Pavan").toString()).isEqualTo("JH,Pavan");
    }

    @Test
    void shouldSplitEachElement() {
        String[] array = new String[]{"JH", "Pavan,Jez", "HK"};
        assertThat(new Matcher(array)).isEqualTo(new Matcher("JH,Pavan,Jez,HK"));
    }

    @Test
    void shouldRemoveTheDuplicationFromEachElement() {
        String[] array = new String[]{"JH", "Pavan,Jez", "Pavan"};
        assertThat(new Matcher(array)).isEqualTo(new Matcher("JH,Pavan,Jez"));
    }

    @Test
    void shouldReturnMatchersAsArray() {
        assertThat(new Matcher("JH,Pavan").toCollection()).isEqualTo(Arrays.asList("JH", "Pavan"));
    }

    @Test
    void shouldTrim() {
        assertThat(new Matcher("  JH   , Pavan ")).isEqualTo(new Matcher("JH,Pavan"));
    }

    @Test
    void shouldMatchWordBoundaries() throws Exception {
        assertThat(new Matcher("!!").matches("!!")).isTrue();
        assertThat(new Matcher("ja").matches(" ja")).isTrue();
        assertThat(new Matcher("ja").matches("ja ")).isTrue();
        assertThat(new Matcher("ja").matches(" ja")).isTrue();
        assertThat(new Matcher("ja").matches("ja:")).isTrue();
        assertThat(new Matcher("jez.humble@thoughtworks.com").matches("[jez.humble@thoughtworks.com] i checkin")).isTrue();
        assertThat(new Matcher("ja").matches("ja&jh")).isTrue();
    }

    @Test
    void shouldNotMatchWordContainsMatcher() throws Exception {
        assertThat(new Matcher("ja").matches("javascript")).isFalse();
        assertThat(new Matcher("ja").matches("kaja")).isFalse();
        assertThat(new Matcher("jez.humble@thoughtworks.com").matches("jez.humble")).isFalse();
    }

    @Test
    void shouldEscapeRegexes() throws Exception {
        assertThat(new Matcher("[").matches("[")).isTrue();
        assertThat(new Matcher("]").matches("]]")).isTrue();
        assertThat(new Matcher("\\").matches("\\\\")).isTrue();
        assertThat(new Matcher("^^").matches("^^")).isTrue();
        assertThat(new Matcher("$").matches("$$")).isTrue();
        assertThat(new Matcher("..").matches("...")).isTrue();
        assertThat(new Matcher("|||").matches("||||")).isTrue();
        assertThat(new Matcher("??").matches("???")).isTrue();
        assertThat(new Matcher("**").matches("**")).isTrue();
        assertThat(new Matcher("++").matches("++")).isTrue();
        assertThat(new Matcher("((").matches("(((")).isTrue();
        assertThat(new Matcher("))").matches(")))")).isTrue();
    }

    @Test
    void shouldNotMatchAnyThing() throws Exception {
        assertThat(new Matcher("").matches("ja")).isFalse();
    }

    @Test
    void shouldValidateAllMatchersUsingAValidator() throws Exception {
        new Matcher(new String[]{"aaa,a"}).validateUsing(Validator.lengthValidator(200));
    }

    @Test
    void shouldMatchInMultiLineText() throws Exception {
        assertThat(new Matcher("abc").matches("abc def\nghi jkl")).isTrue();
        assertThat(new Matcher("ghi").matches("abc def\nghi jkl")).isTrue();
    }
}
