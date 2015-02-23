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
