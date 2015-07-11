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


import com.thoughtworks.go.config.Resource;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @understands persisting and retrieving resource
 */
@Service
public class ResourceRepository extends HibernateDaoSupport {

    private static final String GET_RESOURCES_BY_BUILD_ID = "SELECT r FROM Resource r WHERE r.buildId = ? ORDER BY r.id";

    @Autowired
    public ResourceRepository(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    public void save(Resource resource) {
        if (resource != null) {
            getHibernateTemplate().save(resource);
        }
    }

    public Resource saveCopyOf(long jobId, Resource resource) {
        Resource copyOfResource = new Resource(resource);
        copyOfResource.setBuildId(jobId);
        save(copyOfResource);
        return copyOfResource;
    }

    public List<Resource> findByBuildId(long buildId) {
        return (List<Resource>) getHibernateTemplate().find(GET_RESOURCES_BY_BUILD_ID, buildId);
    }
}
