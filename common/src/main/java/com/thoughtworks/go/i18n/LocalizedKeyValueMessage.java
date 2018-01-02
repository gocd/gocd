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

import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

/**
 * @understands How to localize key value messages
 */
public class LocalizedKeyValueMessage implements Localizable.CurryableLocalizable {
    private final String key;
    private final Object[] args;

    static final String PIPELINE_CANNOT_VIEW = "PIPELINE_CANNOT_VIEW";
    static final String PIPELINE_CANNOT_OPERATE = "PIPELINE_CANNOT_OPERATE";
    static final String MATERIAL_CANNOT_VIEW = "MATERIAL_CANNOT_VIEW";
    static final String MATERIAL_WITH_FINGERPRINT_NOT_FOUND = "MATERIAL_WITH_FINGERPRINT_NOT_FOUND";
    static final String MODIFIED_BY_VALUE = "MODIFIED_BY_VALUE";
    static final String NO_STAGE_IN_PIPELINE = "NO_STAGE_IN_PIPELINE";
    static final String NO_JOB_IN_STAGE = "NO_JOB_IN_STAGE";
    static final String X_OF_Y = "X_OF_Y";
    static final String STAGE_FOR_LOCATOR_NOT_FOUND = "STAGE_FOR_LOCATOR_NOT_FOUND";
    static final String UNABLE_TO_RETRIEVE_FAILURE_RESULTS = "UNABLE_TO_RETRIEVE_FAILURE_RESULTS";
    static final String FAILURES_COUNT = "FAILURES_COUNT";
    static final String ERRORS_COUNT = "ERRORS_COUNT";
    static final String WARNINGS_COUNT = "WARNINGS_COUNT";
    static final String URL_NOT_KNOWN = "URL_NOT_KNOWN";
    public static final String NO_VIEW_PERMISSION_ON_PIPELINE = "NO_VIEW_PERMISSION_ON_PIPELINE";

    LocalizedKeyValueMessage(Object key, Object... args) {
        this.key = makeKey(key);
        this.args = args;
    }

    private static String makeKey(Object key) {
        return key.toString().toUpperCase().replace(" ", "_");
    }


    public String localize(Localizer localizer) {
        return localizer.localize(key, args);
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) { return true; }
        if (that == null) { return false; }
        if (getClass() != that.getClass()) { return false; }

        return equals((LocalizedKeyValueMessage)that);
    }

    private boolean equals(LocalizedKeyValueMessage that) {
        if (!Arrays.equals(this.args, that.args)) { return false; }
        if (!this.key.equals(that.key)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
        return result;
    }

    @Override public String toString() {
        return "LocalizedKeyValueMessage{" +
                "key='" + key + '\'' +
                ", args=" + (args == null ? null : Arrays.asList(args)) +
                '}';
    }

    public CurryableLocalizable addParam(Object param) {
        return new LocalizedKeyValueMessage(key, ArrayUtils.add(args, param));
    }
}
