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
package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;

public abstract class CRBase implements Locatable {
    // plugin can optionally assign location on any configuration element
    protected String location;

    //TODO rename to collectErrors
    public abstract void getErrors(ErrorCollection errors,String parentLocation);

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public ErrorCollection getErrors() // shorthand for tests
    {
        ErrorCollection errors = new ErrorCollection();
        getErrors(errors,"Unknown");
        return errors;
    }
}