/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.lang.NotImplementedException;

import java.util.Map;

public class DummyMaterialConfig extends ScmMaterialConfig {
    public DummyMaterialConfig() {
        super("DummyMaterial");
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getEncryptedPassword() {
        return null;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public void setUrl(String url) {
        throw new NotImplementedException();
    }

    @Override
    protected UrlArgument getUrlArgument() {
        return null;
    }

    @Override
    protected String getLocation() {
        return null;
    }

    @Override
    public void validateConcreteScmMaterial() {
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
    }

    @Override
    public String getTypeForDisplay() {
        return null;
    }

    @Override
    public String getLongDescription() {
        return null;
    }
}
