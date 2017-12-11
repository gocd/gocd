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
package com.thoughtworks.go.server.persistence;


import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.TestArtifactPlan;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @understands persisting and retrieving artifact plan
 */
@Service
public class ArtifactPlanRepository extends HibernateDaoSupport {

    private static final String GET_ARTIFACT_PLANS_BY_BUILD_ID =
            "SELECT a FROM ArtifactPlan a WHERE a.buildId = ? ORDER BY a.id";

    @Autowired
    public ArtifactPlanRepository(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    public void save(ArtifactPlan artifactPlan) {
        if (artifactPlan != null) {
            getHibernateTemplate().save(artifactPlan);
        }
    }

    public void save(TestArtifactPlan testArtifactPlan) {
        if (testArtifactPlan != null) {
            save(testArtifactPlan);
        }
    }

    public List<ArtifactPlan> findByBuildId(long buildId) {
        return (List<ArtifactPlan>) getHibernateTemplate().find(GET_ARTIFACT_PLANS_BY_BUILD_ID, buildId);
    }

    public ArtifactPlan saveCopyOf(long jobId, ArtifactPlan artifactPlan) {
        ArtifactPlan copyOfArtifactPlan = new ArtifactPlan(artifactPlan);
        copyOfArtifactPlan.setBuildId(jobId);
        save(copyOfArtifactPlan);
        return copyOfArtifactPlan;
    }
}
