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

package com.thoughtworks.go.server.web;

/**
 * Maps a tab name to a link.
 */
public class TabConfiguration {
    private String name;
    private String link;
    private String[] cssFiles;
    private String viewName;

    public void setName(final String name) {
        this.name = name;
    }

    public void setLink(final String link) {
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public String[] getCssFiles() {
        return cssFiles;
    }

    public void setCssFiles(String[] cssFiles) {
        this.cssFiles = cssFiles;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public String getViewName() {
        return viewName;
    }

    public String toString() {
        return this.getClass().toString() + ": " + name + ", " + link;
    }
}
