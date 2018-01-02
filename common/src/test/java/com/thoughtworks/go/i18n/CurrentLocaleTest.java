/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.i18n;

import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CurrentLocaleTest {

    @Test
    public void shouldReturnLocaleForTheCurrentThread() {
        final CurrentLocale locale = new CurrentLocale();
        new Thread(new Runnable() {

            public void run() {
                locale.setLocaleString("in");
                synchronized (this) {
                    try {
                        wait();//Wait for the other thread to set the locale and finish assertion
                    } catch (InterruptedException ignored) {
                    }
                }
                assertThat(locale.getLocale().getLanguage(), is(new Locale("in").getLanguage()));
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                locale.setLocaleString("en");
                assertThat(locale.getLocale().getLanguage(), is(new Locale("en").getLanguage()));
                synchronized (this) {
                    notifyAll();//Notify the waiting thread
                }
            }
        }).start();
    }
}
