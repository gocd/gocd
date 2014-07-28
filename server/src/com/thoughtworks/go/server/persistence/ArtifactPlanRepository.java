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
}
