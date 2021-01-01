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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.util.ConfigUtil;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;

import java.util.List;

public class UniqueOnCancelValidator implements GoConfigXMLValidator {
    @Override
    public void validate(Element element, ConfigElementImplementationRegistry registry) throws Exception {
        XPathFactory xPathFactory = XPathFactory.instance();
        List<String> tasks = ConfigUtil.allTasks(registry);
        for (String task : tasks) {
            List<Element> taskNodes = xPathFactory.compile("//" + task, Filters.element()).evaluate(element);
            for (Element taskNode : taskNodes) {
                List<Element> list = xPathFactory.compile("oncancel", Filters.element()).evaluate(taskNode);
                if (list.size() > 1) {
                    throw new Exception("Task [" + task + "] should not contain more than 1 oncancel task");
                }
            }
        }
    }
}
