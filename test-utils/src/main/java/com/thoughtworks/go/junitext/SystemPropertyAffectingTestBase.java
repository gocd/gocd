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

package com.thoughtworks.go.junitext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;

/* Automatically cleans up system properties changed in the @BeforeClass of the integration test.
 *
 * Also, marks the test as @DirtiesContext, so that Spring test framework will know to recreate the
 * context for the next integration test.
 */
@DirtiesContext
public abstract class SystemPropertyAffectingTestBase {
    private static Map<String, String> oldProperties = new HashMap<>();

    @BeforeClass
    public static void startWithEmptyProperties() {
        oldProperties.clear();
    }

    protected static void overrideProperty(String key, String value) {
        oldProperties.put(key, value);
        System.setProperty(key, value);
    }

    @AfterClass
    public static void resetAllOverriddenProperties() {
        for (String key : oldProperties.keySet()) {
            System.setProperty(key, oldProperties.get(key));
        }
    }
}
