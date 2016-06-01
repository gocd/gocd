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

package com.thoughtworks.go.server.service;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.StageArtifactCleanupProhibited;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

@Component
public class ConfigDbStateRepository extends HibernateDaoSupport {

    private final GoConfigService goConfigService;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public ConfigDbStateRepository(SessionFactory sessionFactory, GoConfigService goConfigService, TransactionTemplate transactionTemplate) {
        this.goConfigService = goConfigService;
        this.transactionTemplate = transactionTemplate;
        setSessionFactory(sessionFactory);
    }

    public void flushConfigState() {
        transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                return flushArtifactCleanupProhibitions();
            }
        });
    }

    private Object flushArtifactCleanupProhibitions() {
        List<StageArtifactCleanupProhibited> existingEntries = getHibernateTemplate().find("from StageArtifactCleanupProhibited");
        HashMap<Map.Entry<String, String>, StageArtifactCleanupProhibited> persistentStateMap = new HashMap<>();
        for (StageArtifactCleanupProhibited persistentState : existingEntries) {
            persistentState.setProhibited(false);
            persistentStateMap.put(new AbstractMap.SimpleEntry<>(persistentState.getPipelineName(), persistentState.getStageName()), persistentState);
        }
        List<PipelineConfig> pipelineConfigs = goConfigService.currentCruiseConfig().allPipelines();
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            for (StageConfig stageConfig : pipelineConfig) {
                StageArtifactCleanupProhibited stageArtifactCleanupProhibited = persistentStateMap.get(new AbstractMap.SimpleEntry<>(CaseInsensitiveString.str(pipelineConfig.name()),
                        CaseInsensitiveString.str(stageConfig.name())));
                if (stageArtifactCleanupProhibited == null) {
                    stageArtifactCleanupProhibited = new StageArtifactCleanupProhibited(CaseInsensitiveString.str(pipelineConfig.name()), CaseInsensitiveString.str(stageConfig.name()));
                }
                stageArtifactCleanupProhibited.setProhibited(stageConfig.isArtifactCleanupProhibited());
                getHibernateTemplate().saveOrUpdate(stageArtifactCleanupProhibited);
            }
        }
        return null;
    }

}
