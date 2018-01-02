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

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormat;
import org.springframework.context.support.MessageSourceAccessor;

/**
 * @understands converting to a localized message for a given key
 */
public class Localizer {
    private final MessageSourceAccessor accessor;
    private final CurrentLocale currentLocale;
    public static final String LOCALE_ENGLISH_US = "en";
    public static final String LOCALE_KANNADA = "kd";

    public Localizer(MessageSourceAccessor accessor, CurrentLocale currentLocale) {
        this.accessor = accessor;
        this.currentLocale = currentLocale;
    }

    public String localize(String key) {
        return accessor.getMessage(key, currentLocale.getLocale());
    }

    public String localize(String key, Object... args) {
        return accessor.getMessage(key, args, currentLocale.getLocale());
    }

    public String localize(Duration d) {
        if(d.equals(new Duration(0))) return "";
        return PeriodFormat.getDefault().withLocale(currentLocale.getLocale()).print(d.toPeriod());
    }
}
