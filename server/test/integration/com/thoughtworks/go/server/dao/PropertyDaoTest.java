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

package com.thoughtworks.go.server.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Properties;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.helper.PipelineMother;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.util.ListUtil.join;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PropertyDaoTest {
    @Autowired private PropertyDao propertyDao;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private DataSource dataSource;
    @Autowired private DatabaseAccessHelper dbHelper;

    private JobIdentifier pipeline1_1;
    private static final String PIPELINE1 = "pipeline";
    private static final String STAGE1 = "stage";
    private static final String PLAN1 = "plan";
    private Long buildId;
    private static final String LABEL1 = "LABEL-1";
    private static final JobIdentifier PIPELINE1_LABEL1
            = new JobIdentifier(PIPELINE1, LABEL1, STAGE1, "LATEST", PLAN1, null);

    @Before
    public void setup() throws Exception {

        dbHelper.onSetUp();
        pipeline1_1 = createPassedPipeline();
        buildId = pipeline1_1.getBuildId();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test public void shouldSavePropertyToDatabase() throws Exception {
        assertThat(propertyDao.save(buildId, property("name", "value")), is(true));
    }

    @Test public void shouldNotSaveSameKeyWithSaveInstanceId() throws Exception {
        assertThat(propertyDao.save(buildId, property("name", "value1")), is(true));
        assertThat(propertyDao.save(buildId, property("name", "value2")), is(false));
    }

    @Test public void shouldSaveSameNameOnTwoInstances() throws Exception {
        JobIdentifier secondId = createPassedPipeline();
        assertThat(propertyDao.save(buildId, property("name", "value1")), is(true));
        assertThat(propertyDao.save(secondId.getBuildId(), property("name", "value2")), is(true));
    }

    @Test public void shouldThrowExceptionWhenInstanceNotFound() throws Exception {
        long invalidId = buildId + 200L;
        try {
            propertyDao.save(invalidId, property("no.instance", "boo"));
            fail("Should not be able to save");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), is("No instance '" + invalidId + "' found to set property"));
        }
    }

    @Test public void shouldGetPropertyFromDatabase() throws Exception {
        propertyDao.save(buildId, property("name", "value1"));
        String value = propertyDao.value(buildId, "name");
        assertThat(value, is("value1"));
    }

    @Test public void shouldReturnNullWhenPropertyNotInDatabase() throws Exception {
        String value = propertyDao.value(buildId, "not.directoryExists");
        assertThat(value, is(nullValue()));
    }

    @Test public void shouldReturnListOfAllProperties() throws Exception {
        Property propertyB = property("a/2", "d");
        Property propertyA = property("a/1", "b");
        propertyDao.save(buildId, propertyB);
        propertyDao.save(buildId, propertyA);

        Properties properties = propertyDao.list(buildId);
        assertThat(properties.size(), is(2));
        assertThat(properties.get(0), is(propertyA));
        assertThat(properties.get(1), is(propertyB));
    }

    @Test public void shouldLoadPropertiesHistoryLimitToSpecifiedCount() throws Exception {
        Pipeline oldPipeline = createPipelineWithJobProperty(PIPELINE1, property("key1", "value1"));
        Pipeline newPipeline = createPipelineWithJobProperty(PIPELINE1, property("key2", "value2"));

        List<Properties> history = propertyDao.loadHistory(PIPELINE1, STAGE1, PLAN1, newPipeline.getId(), 1);
        assertThat(history.size(), is(1));
        assertThat(history.get(0).toString(), history.get(0).getValue("key2"), is("value2"));

        history = propertyDao.loadHistory(PIPELINE1, STAGE1, PLAN1, newPipeline.getId(), Integer.MAX_VALUE);
        assertThat(history.size(), is(2));
        assertThat(history.get(0).toString(), history.get(0).getValue("key1"), is("value1"));
        assertThat(history.get(1).toString(), history.get(1).getValue("key2"), is("value2"));
    }

    @Test public void shouldLoadAllPropertiesHistoryIfLimitPipelineNotSpecified() throws Exception {
        Pipeline oldPipeline = createPipelineWithJobProperty(PIPELINE1, property("key1", "value1"));
        Pipeline newPipeline = createPipelineWithJobProperty(PIPELINE1, property("key2", "value2"));

        List<Properties> history = propertyDao.loadHistory(PIPELINE1, STAGE1, PLAN1, null, Integer.MAX_VALUE);
        assertThat(history.size(), is(2));
    }

    @Test
    public void shouldGroupByPipelineId() throws Exception {
        ArrayList<Map<String, Object>> flatHistory = new ArrayList<Map<String, Object>>();
        flatHistory.add(new HashMap<String, Object>() {
            {
                put("PIPELINEID", "1");
                put("KEY", "cruise_job_id");
                put("VALUE", "1");
            }
        });
        flatHistory.add(new HashMap<String, Object>() {
            {
                put("PIPELINEID", "1");
                put("KEY", "cruise_agent");
                put("VALUE", "agent1");
            }
        });
        flatHistory.add(new HashMap<String, Object>() {
            {
                put("PIPELINEID", "2");
                put("KEY", "cruise_job_id");
                put("VALUE", "2");
            }
        });
        flatHistory.add(new HashMap<String, Object>() {
            {
                put("PIPELINEID", "2");
                put("KEY", "cruise_agent");
                put("VALUE", "agent2");
            }
        });
        List<Properties> propertiesList = PropertySqlMapDao.groupByPipelineId(flatHistory);
        assertThat(propertiesList.size(), is(2));
        assertThat(propertiesList.get(0).size(), is(2));
        assertThat(propertiesList.get(0).getValue("cruise_agent"), is("agent1"));
        assertThat(propertiesList.get(1).size(), is(2));
        assertThat(propertiesList.get(1).getValue("cruise_agent"), is("agent2"));
    }

    private JobIdentifier save(JobIdentifier jobIdentifier, Property prop) {
        propertyDao.save(jobIdentifier.getBuildId(), prop);
        return jobIdentifier;
    }

    private Property property(String key, String value) {
        return new Property(key, value);
    }

    private JobIdentifier createPassedPipeline() throws SQLException {
        return createPassedPipeline(PIPELINE1);
    }

    private JobIdentifier createPassedPipeline(String pipelineName) throws SQLException {
        Pipeline pipeline = _createPassedPipeline(pipelineName);
        return new JobIdentifier(pipeline, pipeline.getStages().get(0),
                pipeline.getStages().get(0).getJobInstances().get(0));
    }

    private Pipeline _createPassedPipeline(String pipelineName) {
        Pipeline pipeline = PipelineMother.passedPipelineInstance(pipelineName, STAGE1, PLAN1);
        assertThat(pipeline.getStages().get(0).getJobInstances().size(), is(1));
        pipeline.getStages().get(0).getJobInstances().get(0).setScheduledDate(new Date());
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        return pipeline;
    }

    private Pipeline createPipelineWithJobProperty(String pipelineName, Property property) {
        Pipeline pipeline = _createPassedPipeline(pipelineName);
        propertyDao.save(pipeline.getFirstStage().getJobInstances().first().getId(), property);
        return pipeline;
    }

    private TypeSafeMatcher<Properties> contains(final Property... properties) {
        return new TypeSafeMatcher<Properties>() {
            private final List<String> missingProps = new ArrayList<String>();

            public boolean matchesSafely(Properties item) {
                for (Property property : properties) {
                    if (!item.contains(property)) {
                        missingProps.add(property.toString());
                    }
                }
                return missingProps.isEmpty();
            }

            public void describeTo(Description description) {
                description.appendText("contains [").appendText(join(missingProps)).appendText("]");
            }
        };
    }

}
