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


import com.thoughtworks.go.config.ArtifactPropertiesGenerator;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @understands persisting and retrieving artifact properties generator
 */
@Service
public class ArtifactPropertiesGeneratorRepository extends HibernateDaoSupport {

    private static final String GET_ARTIFACT_PROPERTY_GENERATORS_BY_BUILD_ID =
            "SELECT a FROM ArtifactPropertiesGenerator a WHERE a.jobId = ? ORDER BY a.id";

    @Autowired
    public ArtifactPropertiesGeneratorRepository(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    public void save(ArtifactPropertiesGenerator artifactPropertiesGenerator) {
        if (artifactPropertiesGenerator != null) {
            getHibernateTemplate().save(artifactPropertiesGenerator);
        }
    }

    public List<ArtifactPropertiesGenerator> findByBuildId(long buildId) {
        return (List<ArtifactPropertiesGenerator>) getHibernateTemplate().find(GET_ARTIFACT_PROPERTY_GENERATORS_BY_BUILD_ID, buildId);
    }

    public ArtifactPropertiesGenerator saveCopyOf(long jobId, ArtifactPropertiesGenerator generator) {
        ArtifactPropertiesGenerator copyOfGenerator = new ArtifactPropertiesGenerator(generator);
        copyOfGenerator.setJobId(jobId);
        save(copyOfGenerator);
        return copyOfGenerator;
    }
}
