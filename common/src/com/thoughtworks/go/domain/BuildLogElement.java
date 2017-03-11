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

package com.thoughtworks.go.domain;

import org.jdom2.CDATA;
import org.jdom2.Element;

public class BuildLogElement {
    private Element build;
    private Element buildTask;

    public BuildLogElement() {
        build = new Element("build");
    }

    public Element getElement() {
        return build;
    }

    public void setBuildDuration(String durationAsString) {
        build.setAttribute("time", durationAsString);
    }

    public void setBuildError(String errorMessage) {
        build.setAttribute("error", errorMessage);
    }

    public Element setBuildLogHeader(String execCommand) {
        Element target = new Element("target");
        target.setAttribute("name", "exec");
        build.addContent(target);
        buildTask = new Element("task");
        buildTask.setAttribute("name", execCommand);
        target.addContent(buildTask);
        return buildTask;
    }

    public void setTaskError() {
        Element msg = new Element("message");
        msg.setAttribute("priority", "error");
        buildTask.addContent(msg);
    }

    public void setTaskError(String errorMessage) {
        Element msg = new Element("message");
        msg.addContent(new CDATA(errorMessage));
        msg.setAttribute("priority", "error");
        buildTask.addContent(msg);
    }

    public String getBuildError() {
        return build.getAttribute("error").getValue();
    }
}
