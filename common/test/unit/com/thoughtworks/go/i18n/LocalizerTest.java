/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.i18n;

import static org.hamcrest.core.Is.is;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public class LocalizerTest {

    @Before
    public void setUp() throws Exception {
        CurrentLocale.setLocaleString(null);
    }

    @Test
    public void shouldReturnTheLocalizedMessageForTheCurrentLocale() {
        CurrentLocale locale = new CurrentLocale();
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("test_message");
        MessageSourceAccessor accessor = new MessageSourceAccessor(source);
        Localizer localizer = new Localizer(accessor, locale);

        assertThat(localizer.localize("hello_world"), is("Hello World"));

        locale.setLocaleString(Localizer.LOCALE_KANNADA);
        assertThat(localizer.localize("hello_world"), is("ನಮಸ್ಕಾರ"));
    }

    @Test
    public void shouldLocalizeDurations() {
        CurrentLocale locale = new CurrentLocale();
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("test_message");
        MessageSourceAccessor accessor = new MessageSourceAccessor(source);
        Localizer localizer = new Localizer(accessor, locale);

        assertThat(localizer.localize(new Duration(1000)), is("1 second"));
        assertThat(localizer.localize(new Duration(0)), is(""));

        locale.setLocaleString(Localizer.LOCALE_KANNADA);
        assertThat(localizer.localize(new Duration(1000)), is("1 second"));
    }

    @Test
    public void shouldReturnTheFormattedLocalizedMessage() {
        CurrentLocale locale = new CurrentLocale();
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("test_message");
        MessageSourceAccessor accessor = new MessageSourceAccessor(source);
        Localizer localizer = new Localizer(accessor, locale);

        assertThat(localizer.localize("hello_world"), is("Hello World"));

        locale.setLocaleString(Localizer.LOCALE_ENGLISH_US);
        assertThat(localizer.localize("MY_HOME", "San Francisco"), is("My home is San Francisco"));

        locale.setLocaleString(Localizer.LOCALE_KANNADA);
        assertThat(localizer.localize("MY_HOME", "ಬೆಂಗಳೂರು"), is("ನಮ್ಮ ಊರು ಬೆಂಗಳೂರು"));

        locale.setLocaleString(Localizer.LOCALE_KANNADA);
        assertThat(localizer.localize("MY_BIRTHDAY", new DateTime(1984, 12, 23, 14, 0, 0, 0, DateTimeZone.UTC).toDate()), is("ನನ್ನ ಹುಟ್ಟು ಹಬ್ಬ 12/23/84"));

        locale.setLocaleString(Localizer.LOCALE_ENGLISH_US);
        assertThat(localizer.localize("MY_BIRTHDAY", new DateTime(1984, 12, 23, 14, 0, 0, 0, DateTimeZone.UTC).toDate()), is("My birthday is 12/23/84"));

    }
}
