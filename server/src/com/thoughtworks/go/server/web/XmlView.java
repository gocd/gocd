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

import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.springframework.web.servlet.view.AbstractView;

public class XmlView extends AbstractView {

    public XmlView() {
        setContentType("text/xml");
    }

    protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Document document = (Document) model.get("document");
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            XmlUtils.writeXml(document, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }
}
