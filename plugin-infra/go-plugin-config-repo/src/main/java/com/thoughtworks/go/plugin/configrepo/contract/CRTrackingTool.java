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

package com.thoughtworks.go.plugin.configrepo.contract;

public class CRTrackingTool extends CRBase {
    private String link;
    private String regex;

    public CRTrackingTool(){}
    public CRTrackingTool(String link, String regex) {
        this.link = link;
        this.regex = regex;
    }

    private void validateLink(ErrorCollection errors, String location) {
        if (link != null && !link.contains("${ID}")) {
            errors.addError(location, "Link must be a URL containing '${ID}'. Go will replace the string '${ID}' with the first matched group from the regex at run-time.");
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CRTrackingTool that = (CRTrackingTool) o;
        if (link != null ? !link.equals(that.link) : that.link != null) {
            return false;
        }
        return !(regex != null ? !regex.equals(that.regex) : that.regex != null);

    }

    @Override
    public int hashCode() {
        int result = link != null ? link.hashCode() : 0;
        result = 31 * result + (regex != null ? regex.hashCode() : 0);
        return result;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }


    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location,"link",link);
        errors.checkMissing(location,"regex",regex);
        validateLink(errors,location);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        return String.format("%s; Tracking tool",myLocation);
    }
}
