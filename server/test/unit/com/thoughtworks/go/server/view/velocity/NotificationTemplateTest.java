/* **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.view.velocity;

import com.thoughtworks.go.domain.NotificationFilter;
import com.thoughtworks.go.domain.StageEvent;
import com.thoughtworks.go.domain.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NotificationTemplateTest {
    public static final String TEMPLATE_PATH = "/WEB-INF/vm/mycruise/_notification.vm";

    @Test
    public void shouldShowEmailAndMatchersSectionsWithBasicData() throws Exception {
        HashMap<String, Object> data = new HashMap<>();

        User user = new User("name", "display-name", new String[]{"matcher1", "matcher2"}, "email1", true);
        user.populateModel(data);
        data.put("pipelines", "[]");
        data.put("l", null);

        Document actualDoc = Jsoup.parse(new TestVelocityView(TEMPLATE_PATH, data).render());

        assertEmailSection(actualDoc, "email1", "checked");
        assertMatchers(actualDoc, "matcher1,matcher2", "matcher1,", "matcher2");
    }

    @Test
    public void shouldHTMLEscapeEmailAddress() throws Exception {
        HashMap<String, Object> data = new HashMap<>();

        User user = new User("name", "display-name", new String[]{"matcher1", "matcher2"}, "email1@host.com<script>this && that</script>", true);
        user.populateModel(data);
        data.put("pipelines", "[]");
        data.put("l", null);

        Document actualDoc = Jsoup.parse(new TestVelocityView(TEMPLATE_PATH, data).render());

        assertEmailSection(actualDoc, "email1@host.com&lt;script&gt;this &amp;&amp; that&lt;/script&gt;", "checked");
    }

    @Test
    public void shouldHTMLEscapeMatchers() throws Exception {
        HashMap<String, Object> data = new HashMap<>();

        User user = new User("name", "display-name", new String[]{"<script>this && that</script>", "matcher2"}, "email1@host.com", true);
        user.populateModel(data);
        data.put("pipelines", "[]");
        data.put("l", null);

        Document actualDoc = Jsoup.parse(new TestVelocityView(TEMPLATE_PATH, data).render());

        assertMatchers(actualDoc, "<script>this && that</script>,matcher2", "&lt;script&gt;this &amp;&amp; that&lt;/script&gt;,", "matcher2");
    }

    @Test
    public void shouldHTMLEscapeChosenNotificationFilters() throws Exception {
        HashMap<String, Object> data = new HashMap<>();

        User user = new User("name", "display-name", new String[]{"matcher1", "matcher2"}, "email1@host.com", true);
        user.addNotificationFilter(new NotificationFilter("<script>pipeline1 && that</script>", "stage1", StageEvent.Passes, false));
        user.addNotificationFilter(new NotificationFilter("pipeline2", "<script>stage2 && that</script>", StageEvent.Fails, true));
        user.populateModel(data);
        data.put("pipelines", "[]");
        data.put("l", null);

        Document actualDoc = Jsoup.parse(new TestVelocityView(TEMPLATE_PATH, data).render());

        assertNotificationFilter(actualDoc, 0, "&lt;script&gt;pipeline1 &amp;&amp; that&lt;/script&gt;", "stage1", "Passes", "All");
        assertNotificationFilter(actualDoc, 1, "pipeline2", "&lt;script&gt;stage2 &amp;&amp; that&lt;/script&gt;", "Fails", "Mine");
    }

    private void assertEmailSection(Document document, String emailAddress, String emailMeCheckboxValue) {
        assertThat(document.select("#tab-content-of-notifications #email-settings #email-text").first().html(), is(emailAddress));
        assertThat(document.select("#tab-content-of-notifications #email-settings #emailme").attr("checked"), is(emailMeCheckboxValue));
    }

    private void assertMatchers(Document document, String expectedCombinedMatchersValue, String... individualMatchers) {
        for (int i = 0; i < individualMatchers.length; i++) {
            assertThat(document.select("#tab-content-of-notifications #email-settings .matcher").get(i).html(), is(individualMatchers[i]));
        }
        assertThat(document.select("#tab-content-of-notifications #email-settings #matchers").val(), is(expectedCombinedMatchersValue));
    }

    private void assertNotificationFilter(Document document, int notificationIndex, String pipelineName, String stageName, String event, String checkInMatcher) {
        Element rowForNotification = document.select("#tab-content-of-notifications #filters-settings table.filters tbody tr").get(notificationIndex);
        assertThat(rowForNotification.select("td").get(0).html(), is(pipelineName));
        assertThat(rowForNotification.select("td").get(1).html(), is(stageName));
        assertThat(rowForNotification.select("td").get(2).html(), is(event));
        assertThat(rowForNotification.select("td").get(3).html(), is(checkInMatcher));
    }
}