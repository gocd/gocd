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
