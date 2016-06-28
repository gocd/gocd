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

package com.thoughtworks.go.server.materials;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.log4j.Logger;

@Component
public class MaterialUpdateStatusNotifier implements GoMessageListener<MaterialUpdateCompletedMessage> {
    private static final Logger LOGGER = Logger.getLogger(MaterialUpdateStatusNotifier.class);
    private final ConcurrentMap<String, MaterialUpdateStatusListener> pending = new ConcurrentHashMap<>();

    @Autowired
    public MaterialUpdateStatusNotifier(MaterialUpdateCompletedTopic topic) {
        topic.addListener(this);
    }

    public void registerListenerFor(PipelineConfig pipelineConfig, MaterialUpdateStatusListener materialUpdateStatusListener) {
        pending.putIfAbsent(CaseInsensitiveString.str(pipelineConfig.name()), materialUpdateStatusListener);
    }

    public boolean hasListenerFor(PipelineConfig pipelineConfig) {
        return pending.containsKey(CaseInsensitiveString.str(pipelineConfig.name()));
    }

    public void removeListenerFor(PipelineConfig pipelineConfig) {
        pending.remove(CaseInsensitiveString.str(pipelineConfig.name()));
    }

    public void onMessage(MaterialUpdateCompletedMessage message) {
        for (MaterialUpdateStatusListener listener : pending.values()) {
            if (listener.isListeningFor(message.getMaterial())) {
                try {
                    listener.onMaterialUpdate(message);
                } catch (Exception e) {
                    LOGGER.error("Caught error when notifying listeners of material-update", e);
                }
            }
        }
    }
}
