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

package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ExponentialBackoffService implements GoMessageListener<MaterialUpdateCompletedMessage>  {
    private final SystemEnvironment systemEnvironment;
    private ConcurrentMap<Material, ExponentialBackOff> materials = new ConcurrentHashMap<>();

    @Autowired
    public ExponentialBackoffService(MaterialUpdateCompletedTopic completed, SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        completed.addListener(this);
    }

    public BackOffResult shouldBackOff(Material material) {
        ExponentialBackOff exponentialBackOff = materials.get(material);

        if (exponentialBackOff == null) {
            return BackOffResult.PERMIT;
        }

        return exponentialBackOff.backOffResult();
    }

    @Override
    public void onMessage(MaterialUpdateCompletedMessage message) {
        if (message instanceof MaterialUpdateFailedMessage) {
            ExponentialBackOff backOff = materials.get(message.getMaterial());

            if (null == backOff) {
                materials.put(message.getMaterial(), new ExponentialBackOff(systemEnvironment.getMDUExponentialBackOffMultiplier()));
            } else {
                backOff.failedAgain();
            }
        } else {
            materials.remove(message.getMaterial());
        }
    }
}
