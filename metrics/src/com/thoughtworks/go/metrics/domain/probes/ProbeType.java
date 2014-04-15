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

package com.thoughtworks.go.metrics.domain.probes;

public class ProbeType {
    public static final ProbeType SAVE_CONFIG_XML_THROUGH_API = new ProbeType("save.config.xml.through.api");
    public static final ProbeType SAVE_CONFIG_XML_THROUGH_XML_TAB = new ProbeType("save.config.xml.through.xml.tab");
    public static final ProbeType SAVE_CONFIG_XML_THROUGH_CLICKY_ADMIN = new ProbeType("save.config.xml.through.clicky.admin");
    public static final ProbeType SAVE_CONFIG_XML_THROUGH_SERVER_CONFIGURATION_TAB = new ProbeType("save.config.xml.through.server.configuration.tab");
    public static final ProbeType UPDATE_CONFIG = new ProbeType("update.config");
    public static final ProbeType CONVERTING_CONFIG_XML_TO_OBJECT = new ProbeType("converting.go.xml.to.object");
    public static final ProbeType PREPROCESS_AND_VALIDATE = new ProbeType("preprocess.and.validate");
    public static final ProbeType VALIDATING_CONFIG = new ProbeType("validate.config");
    public static final ProbeType WRITE_CONFIG_TO_FILE_SYSTEM = new ProbeType("write.config.to.file.system");
    public static final ProbeType MATERIAL_UPDATE_QUEUE_COUNTER = new ProbeType("material.update.queue.counter");
    private final String type;

    private ProbeType(String type) {
        this.type = type;
    }

    public String getName() {
        return type;
    }
}
