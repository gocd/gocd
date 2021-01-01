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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Map;

public class TestingMaterialConfig extends ScmMaterialConfig {
    public static final Date TWO_DAYS_AGO_CHECKIN = new DateTime().minusDays(2).toDate();

    public static final String MOD_TYPE = "svn";
    public static final String MOD_REVISION = "98";

    private static final String TYPE = "TestingMaterial";

    private String url;

    public TestingMaterialConfig() {
        super(TYPE);
    }

    public TestingMaterialConfig(String url) {
        this();
        this.url = url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getLongDescription() {
        return String.format("Url: %s", url);
    }

    @Override
    protected String getLocation() {
        return getUrl();
    }

    @Override
    public String getUriForDisplay() {
        return this.url;
    }

    @Override
    public void validateConcreteScmMaterial(ValidationContext validationContext) {
    }

    @Override
    public String getTypeForDisplay() {
        return TYPE;
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
    }
}
