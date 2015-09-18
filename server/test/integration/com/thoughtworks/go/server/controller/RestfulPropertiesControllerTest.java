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

package com.thoughtworks.go.server.controller;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;
import javax.sql.DataSource;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.functional.helpers.CSVResponse;
import com.thoughtworks.go.server.service.PipelineService;
import com.thoughtworks.go.server.service.PropertiesService;
import com.thoughtworks.go.server.service.RestfulService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.server.controller.RestfulActionTestHelper.assertContentStatusWithTextPlain;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/spring-tabs-servlet.xml",
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})

public class RestfulPropertiesControllerTest {
    @Autowired private DataSource dataSource;
    @Autowired private PipelineDao pipelineDao;
    @Autowired private PropertiesService propertiesService;
    @Autowired private RestfulService restfulService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineService pipelineService;

    private MockHttpServletResponse response;
    @Autowired private DatabaseAccessHelper dbHelper;
    private Pipeline oldPipeline;
    private Pipeline newPipeline;
    private PropertiesController propertiesController;
    private Stage oldStage;
    private Stage newStage;
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before public void setup() throws Exception {
        configHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        response = new MockHttpServletResponse();

        dbHelper.onSetUp();
        oldPipeline = dbHelper.saveTestPipeline("pipeline", "stage", "build");
        oldStage = oldPipeline.getStages().byName("stage");

        newPipeline = dbHelper.saveTestPipeline("pipeline", "stage", "build");
        newStage = newPipeline.getStages().byName("stage");

        configHelper.addPipeline("pipeline", "stage", "build");
        propertiesController = new PropertiesController(propertiesService, restfulService, pipelineService);
    }

    @After public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test public void shouldGetPropertyRestfully() throws Exception {
        setProperty("foo", "bar");

        ModelAndView modelAndView = getProperty("foo", "json");
        Map map = modelAndView.getModel();
        String content = map.get("json").toString();
        assertThat(content, containsString("bar"));
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test public void shouldReturn404WhenPropertyNotSet() throws Exception {
        getProperty("foo", "json");
        assertValidJsonContentAndStatus(SC_NOT_FOUND, "Property 'foo' not found.");
    }

    @Test public void shouldReturn404WhenUnknownBuildOnGettingProperty() throws Exception {
        String counter = String.valueOf(newStage.getCounter());
        propertiesController.jobSearch("unknown", "latest", "stage", counter,
                "build", "json", "foo", response);
        assertValidJsonContentAndStatus(SC_NOT_FOUND, "Job unknown/latest/stage/" + counter + "/build not found.");
    }

    @Test public void shouldReturnCreatedWhenCreatingNewProperty() throws Exception {
        setProperty("a", "b");
        assertValidJsonContentAndStatus(SC_CREATED, "Property 'a' created with value 'b'");
        ModelAndView modelAndView = getProperty("a", "json");
        Map map = modelAndView.getModel();
        String content = map.get("json").toString();
        assertThat(content, containsString("b"));
        assertThat(response.getStatus(), is(SC_OK));
    }

    @Test public void shouldNotCreatePropertyTwice() throws Exception {
        setProperty("a", "b");
        assertValidJsonContentAndStatus(SC_CREATED, "Property 'a' created with value 'b'");
        setProperty("a", "c");
        assertValidJsonContentAndStatus(SC_CONFLICT, "Property 'a' is already set.");
    }

    @Test public void shouldNotAllowCreatingPropertyWithKeyOrValueLargerThat255Characters() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 200; i++) {
            sb.append("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        }
        String manyChars = sb.toString();
        setProperty(manyChars, "a");
        assertValidJsonContentAndStatus(SC_FORBIDDEN, "Unable to set property with key larger than 255 characters.");
        setProperty("a", manyChars);
        assertValidJsonContentAndStatus(SC_FORBIDDEN, "Unable to set property with value larger than 255 characters.");
    }

    @Test public void shouldAllowCreatingAPropertyContainingAURI() throws Exception {
        URI uri = new URI("https", "user:password", "10.18.32.41", 986, "/foo/bar/baz", "a=b&c=d", "some_thing");

        setProperty("uri1", uri.toString());
        assertValidJsonContentAndStatus(SC_CREATED,
                "Property 'uri1' created with value 'https://user:password@10.18.32.41:986/foo/bar/baz?a=b&c=d#some_thing'");

        setProperty("uri2", uri.toString());
        assertValidJsonContentAndStatus(SC_CREATED,
                "Property 'uri2' created with value 'https://user:password@10.18.32.41:986/foo/bar/baz?a=b&c=d#some_thing'");

    }

    @Test public void shouldNotAllowCreatingPropertyWithKeyWithInvalidChars() throws Exception {
        String valid = "4aZ_-./";
        String invalid = "*aZ_-./,";

        setProperty(invalid, valid);
        assertValidJsonContentAndStatus(SC_FORBIDDEN, PropertiesController.INVALID_VALUE);
    }

    @Test public void shouldReturn404WhenUnknownBuildOnSettingProperty() throws Exception {
        propertiesController.setProperty("unknown", "latest", "stage", "1", "build", "foo", "bar", response);
        assertValidJsonContentAndStatus(SC_NOT_FOUND, "Job unknown/latest/stage/1/build not found.");
    }


    @Test public void shouldReturnOkListingAllPropertiesHistoryInCsvFormatBySearch() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        setProperty(newPipeline, "a/2", "400");
        setProperty(newPipeline, "a/1", "300");
        getAllPropertyHistoryListAsCsvBySearch();
        CSVResponse csvResponse = new CSVResponse(response);
        assertThat(csvResponse.isCSV(), is(true));
        assertThat(csvResponse.statusEquals(SC_OK), is(true));
        assertThat(csvResponse.containsRow("a/1", "a/2"), is(true));
        assertThat(csvResponse.containsColumn("a/1", "100", "300"), is(true));
        assertThat(csvResponse.containsColumn("a/2", "200", "400"), is(true));
    }

    @Test public void shouldSupportLimitingHistoryBySearch() throws Exception {
        setProperty(oldPipeline, "a", "100");
        setProperty(newPipeline, "a", "300");
        getAllPropertyHistoryListAsCsvBySearch(null, 1);
        CSVResponse csvResponse = new CSVResponse(response);
        assertThat(csvResponse.isCSV(), is(true));
        assertThat(csvResponse.statusEquals(SC_OK), is(true));
        assertThat(csvResponse.containsColumn("a", "300"), is(true));

    }

    @Test public void shouldSupportLimitingHistoryBasedOnPipelineLabelBySearch() throws Exception {
        setProperty(oldPipeline, "a", "100");
        setProperty(newPipeline, "a", "300");
        getAllPropertyHistoryListAsCsvBySearch(oldPipeline, 1);
        CSVResponse csvResponse = new CSVResponse(response);
        assertThat(csvResponse.statusEquals(SC_OK), is(true));
        assertThat(csvResponse.containsColumn("a", "100"), is(true));
    }

    @Test public void shouldReturnOkListingAllPropertiesInCsvFormatBySearch() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        getPropertyHistoryListBySearch(oldStage.getCounter(), oldPipeline.getLabel(), "csv", null);
        CSVResponse csvResponse = new CSVResponse(response);
        assertThat(csvResponse.isCSV(), is(true));
        assertThat(csvResponse.statusEquals(SC_OK), is(true));
        assertThat(csvResponse.containsRow("a/1", "a/2"), is(true));
        assertThat(csvResponse.containsColumn("a/1", "100"), is(true));
        assertThat(csvResponse.containsColumn("a/2", "200"), is(true));
    }

    @Test public void shouldReturnOkListingAllPropertiesInCsvFormatAsDefaultBySearch() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        getPropertyHistoryListBySearch(oldStage.getCounter(), oldPipeline.getLabel(), null, null);
        CSVResponse csvResponse = new CSVResponse(response);
        assertThat(csvResponse.isCSV(), is(true));
        assertThat(csvResponse.statusEquals(SC_OK), is(true));
        assertThat(csvResponse.containsRow("a/1", "a/2"), is(true));
        assertThat(csvResponse.containsColumn("a/1", "100"), is(true));
        assertThat(csvResponse.containsColumn("a/2", "200"), is(true));
    }

    @Test public void shouldReturnOkSpecificPropertyInCsvFormatBySearch() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        getPropertyHistoryListBySearch(oldStage.getCounter(), oldPipeline.getLabel(), null, "a/2");
        CSVResponse csvResponse = new CSVResponse(response);
        assertThat(csvResponse.isCSV(), is(true));
        assertThat(csvResponse.statusEquals(SC_OK), is(true));
        assertThat(csvResponse.containsRow("a/2"), is(true));
        assertThat(csvResponse.containsColumn("a/2", "200"), is(true));
        assertThat(csvResponse.containsColumn("a/1", "100"), is(false));
    }

    @Test public void shouldReturnOkSpecificPropertyInJSONFormatBySearch() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        response = new MockHttpServletResponse();
        ModelAndView modelAndView = propertiesController.jobSearch("pipeline", oldPipeline.getLabel(), "stage",
                String.valueOf(oldStage.getCounter()), "build",
                "json", "a/2", response);
        Map map = modelAndView.getModel();
        String content = map.get("json").toString();
        assertThat(content, containsString("a/2"));
        assertThat(content, containsString("200"));
        assertThat(content, not(containsString("a/1")));
        assertThat(content, not(containsString("100")));
    }

    @Test public void shouldReturnOkListingAllPropertiesInJsonFormatBySearch() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        response = new MockHttpServletResponse();
        ModelAndView modelAndView = propertiesController.jobSearch("pipeline", oldPipeline.getLabel(), "stage",
                String.valueOf(oldStage.getCounter()), "build",
                "json", null, response);
        Map map = modelAndView.getModel();
        String content = map.get("json").toString();
        assertThat(content, containsString("a/2"));
        assertThat(content, containsString("a/1"));
        assertThat(content, containsString("200"));
        assertThat(content, containsString("100"));
    }

    @Test public void shouldReturn404WhenUnknownBuildOnList() throws Exception {
        setProperty(oldPipeline, "a/2", "200");
        setProperty(oldPipeline, "a/1", "100");
        response = new MockHttpServletResponse();
        propertiesController.jobSearch("unknown", oldPipeline.getLabel(), "stage",
                String.valueOf(oldStage.getCounter()), "build",
                "json", null, response);
        assertThat(response.getStatus(), Is.is(SC_NOT_FOUND));
    }


    private void getAllPropertyHistoryListAsCsvBySearch() throws Exception {
        getAllPropertyHistoryListAsCsvBySearch(null, null);
    }

    private ModelAndView getPropertyHistoryListBySearch(Integer counter, String label, String type, String propertyKey)
            throws Exception {
        response = new MockHttpServletResponse();
        return propertiesController.jobSearch("pipeline", label, "stage", String.valueOf(counter), "build", type,
                propertyKey, response);
    }

    private void getAllPropertyHistoryListAsCsvBySearch(Pipeline startFrom, Integer count) throws Exception {
        response = new MockHttpServletResponse();
        String limitLabel = startFrom == null ? null : startFrom.getLabel();
        propertiesController.jobsSearch("pipeline", "stage", "build", limitLabel, count, response);
    }

    private ModelAndView getProperty(String property, String type) throws Exception {
        response = new MockHttpServletResponse();
        return propertiesController.jobSearch("pipeline", "latest", "stage", String.valueOf(newStage.getCounter()),
                "build", type, property, response);
    }

    private void setProperty(String property, String value) throws Exception {
        response = new MockHttpServletResponse();
        propertiesController.setProperty("pipeline", "latest", "stage", null, "build", property, value, response);
    }

    private void setProperty(Pipeline pipeline, String property, String value) throws Exception {
        response = new MockHttpServletResponse();
        propertiesController.setProperty("pipeline", pipeline.getLabel(), "stage", null, "build",
                property, value, response);
        assertThat(response.getContentAsString(), response.getStatus(), is(SC_CREATED));
    }

    private void assertValidJsonContentAndStatus(int status, String content) throws UnsupportedEncodingException {
        assertContentStatusWithTextPlain(response, status, content);
    }
}

