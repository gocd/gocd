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

package com.thoughtworks.go.metrics.service;

import java.util.HashMap;

import com.thoughtworks.go.metrics.domain.context.Context;
import com.thoughtworks.go.metrics.domain.probes.DoNothingProbe;
import com.thoughtworks.go.metrics.domain.probes.MessageQueueCounterProbe;
import com.thoughtworks.go.metrics.domain.probes.Probe;
import com.thoughtworks.go.metrics.domain.probes.ProbeType;
import com.thoughtworks.go.metrics.domain.probes.TimerProbe;
import com.thoughtworks.go.util.SystemEnvironment;
import com.yammer.metrics.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetricsProbeService {
    private HashMap<ProbeType, Probe> map;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public MetricsProbeService(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        map = new HashMap<ProbeType, Probe>();
        if (systemEnvironment.get(SystemEnvironment.CAPTURE_METRICS)) {
            map.put(ProbeType.SAVE_CONFIG_XML_THROUGH_API, new TimerProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_API, "AdminController.handleEditConfiguration"));
            map.put(ProbeType.SAVE_CONFIG_XML_THROUGH_XML_TAB, new TimerProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_XML_TAB, "AdminService.updateConfig"));
            map.put(ProbeType.SAVE_CONFIG_XML_THROUGH_CLICKY_ADMIN, new TimerProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_CLICKY_ADMIN, "GoConfigService.updateConfigFromUI"));
            map.put(ProbeType.SAVE_CONFIG_XML_THROUGH_SERVER_CONFIGURATION_TAB, new TimerProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_SERVER_CONFIGURATION_TAB, "ServerConfigService.updateServerConfig"));
            map.put(ProbeType.UPDATE_CONFIG, new TimerProbe(ProbeType.UPDATE_CONFIG, "GoConfigDao.updateConfig"));
            map.put(ProbeType.CONVERTING_CONFIG_XML_TO_OBJECT, new TimerProbe(ProbeType.CONVERTING_CONFIG_XML_TO_OBJECT, "MagicalGoConfigXmlLoader.loadConfigHolder"));
            map.put(ProbeType.PREPROCESS_AND_VALIDATE, new TimerProbe(ProbeType.PREPROCESS_AND_VALIDATE, "MagicalGoConfigXmlLoader"));
            map.put(ProbeType.VALIDATING_CONFIG, new TimerProbe(ProbeType.VALIDATING_CONFIG, "MagicalGoConfigXmlLoader"));
            map.put(ProbeType.WRITE_CONFIG_TO_FILE_SYSTEM, new TimerProbe(ProbeType.WRITE_CONFIG_TO_FILE_SYSTEM, "MagicalGoConfigXmlWriter.write"));
            map.put(ProbeType.MATERIAL_UPDATE_QUEUE_COUNTER, new MessageQueueCounterProbe(ProbeType.MATERIAL_UPDATE_QUEUE_COUNTER, "MaterialUpdateService.updateMaterial"));
        } else {
            Metrics.shutdown();
        }
    }

    public Context begin(ProbeType action) {
        return getProbe(action).begin();
    }

    public void end(ProbeType action, Context context) {
        getProbe(action).end(context);
    }

    Probe getProbe(ProbeType action) {
        if (systemEnvironment.get(SystemEnvironment.CAPTURE_METRICS)) {
            return map.get(action);
        }
        return new DoNothingProbe();
    }
}
