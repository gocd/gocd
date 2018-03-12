/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * @understands what the locale of the current thread is
 */
@Component
public class CurrentLocale {
    private static ThreadLocal<String> locale = new ThreadLocal<>();

    public static void setLocaleString(String locale) {
        CurrentLocale.locale.set(locale);
    }

    public static String getLocaleString() {
        return CurrentLocale.locale.get();
    }

    public Locale getLocale() {
        String localeString = getLocaleString();
        return localeString == null ? Locale.getDefault() : new Locale(localeString);
    }

}
