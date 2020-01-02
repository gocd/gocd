/*
 * Copyright 2020 ThoughtWorks, Inc.
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


import java.util.Arrays;

import com.thoughtworks.go.validation.Validator;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MatcherTest {
    @Test
    public void shouldRemoveTheLastEmptyMatcher() {
        assertThat(new Matcher("JH,Pavan,"), is(new Matcher("JH,Pavan")));
    }

    @Test
    public void shouldAllowWhiteSpace() {
        assertThat(new Matcher("JH Pavan").toCollection().size(), is(1));
    }

    @Test
    public void shouldRemoveDuplication() {
        assertThat(new Matcher("JH,JH"), is(new Matcher("JH")));
    }

    @Test
    public void shouldRemoveTheFirstEmptyMatcher() {
        assertThat(new Matcher(",JH,Pavan"), is(new Matcher("JH,Pavan")));
    }

    @Test
    public void shouldRemoveTheEmptyMatcherInTheMiddle() {
        assertThat(new Matcher("JH,,Pavan"), is(new Matcher("JH,Pavan")));
    }

    @Test
    public void shouldReturnCommaSplittedString() {
        assertThat(new Matcher("JH,Pavan").toString(), is("JH,Pavan"));
    }

    @Test
    public void shouldSplitEachElement() {
        String[] array = new String[]{"JH", "Pavan,Jez", "HK"};
        assertThat(new Matcher(array), is(new Matcher("JH,Pavan,Jez,HK")));
    }

    @Test
    public void shouldRemoveTheDuplicationFromEachElement() {
        String[] array = new String[]{"JH", "Pavan,Jez", "Pavan"};
        assertThat(new Matcher(array), is(new Matcher("JH,Pavan,Jez")));
    }

    @Test
    public void shouldReturnMatchersAsArray() {
        assertThat(new Matcher("JH,Pavan").toCollection(), is(Arrays.asList("JH", "Pavan")));
    }

    @Test
    public void shouldTrim() {
        assertThat(new Matcher("  JH   , Pavan "), is(new Matcher("JH,Pavan")));
    }

    @Test
    public void shouldMatchWordBoundaries() throws Exception {
        assertThat(new Matcher("!!").matches("!!"), is(true));
        assertThat(new Matcher("ja").matches(" ja"), is(true));
        assertThat(new Matcher("ja").matches("ja "), is(true));
        assertThat(new Matcher("ja").matches(" ja"), is(true));
        assertThat(new Matcher("ja").matches("ja:"), is(true));
        assertThat(new Matcher("jez.humble@thoughtworks.com").matches("[jez.humble@thoughtworks.com] i checkin"),
                is(true));
        assertThat(new Matcher("ja").matches("ja&jh"), is(true));
    }

    @Test
    public void shouldNotMatchWordContainsMatcher() throws Exception {
        assertThat(new Matcher("ja").matches("javascript"), is(false));
        assertThat(new Matcher("ja").matches("kaja"), is(false));
        assertThat(new Matcher("jez.humble@thoughtworks.com").matches("jez.humble"), is(false));
    }

    @Test
    public void shouldEscapeRegexes() throws Exception {
        assertThat(new Matcher("[").matches("["), is(true));
        assertThat(new Matcher("]").matches("]]"), is(true));
        assertThat(new Matcher("\\").matches("\\\\"), is(true));
        assertThat(new Matcher("^^").matches("^^"), is(true));
        assertThat(new Matcher("$").matches("$$"), is(true));
        assertThat(new Matcher("..").matches("..."), is(true));
        assertThat(new Matcher("|||").matches("||||"), is(true));
        assertThat(new Matcher("??").matches("???"), is(true));
        assertThat(new Matcher("**").matches("**"), is(true));
        assertThat(new Matcher("++").matches("++"), is(true));
        assertThat(new Matcher("((").matches("((("), is(true));
        assertThat(new Matcher("))").matches(")))"), is(true));
    }

    @Test
    public void shouldNotMatchAnyThing() throws Exception {
        assertThat(new Matcher("").matches("ja"), is(false));
    }

    @Test
    public void shouldValidateAllMatchersUsingAValidator() throws Exception {
        new Matcher(new String[]{"aaa,a"}).validateUsing(Validator.lengthValidator(200));
    }

    @Test
    public void shouldMatchInMultiLineText() throws Exception {
        assertThat(new Matcher("abc").matches("abc def\nghi jkl"), is(true));
        assertThat(new Matcher("ghi").matches("abc def\nghi jkl"), is(true));
    }
}
