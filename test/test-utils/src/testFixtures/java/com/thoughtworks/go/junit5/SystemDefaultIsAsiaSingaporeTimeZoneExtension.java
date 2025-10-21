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

package com.thoughtworks.go.junit5;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.TimeZone;

public class SystemDefaultIsAsiaSingaporeTimeZoneExtension implements BeforeEachCallback, AfterEachCallback {
    public static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone("Asia/Singapore");

    private TimeZone original;

    @Override
    public void beforeEach(@Nullable ExtensionContext ignore) {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TEST_TIME_ZONE);
    }

    @Override
    public void afterEach(@Nullable ExtensionContext context) {
        TimeZone.setDefault(original);
    }
}
