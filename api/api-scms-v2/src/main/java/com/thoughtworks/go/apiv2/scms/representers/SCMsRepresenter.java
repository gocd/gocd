/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv2.scms.representers;


import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.spark.Routes;

import java.util.List;

public class SCMsRepresenter {

    public static void toJSON(OutputWriter writer, List<SCM> scms) {
        writer.addLinks(
            outputLinkWriter -> outputLinkWriter.addAbsoluteLink("doc", Routes.SCM.DOC)
                .addLink("find", Routes.SCM.find())
                .addLink("self", Routes.SCM.BASE))
            .addChild("_embedded", embeddedWriter -> embeddedWriter.addChildList("scms", scmsWriter -> scms.forEach(scm -> scmsWriter.addChild(scmWriter -> SCMRepresenter.toJSON(scmWriter, scm)))));
    }
}
