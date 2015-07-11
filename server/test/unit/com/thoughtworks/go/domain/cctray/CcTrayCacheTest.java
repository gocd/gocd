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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.activity.ProjectStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class CcTrayCacheTest {
    private CcTrayCache cache;

    @Before
    public void setUp() throws Exception {
        cache = new CcTrayCache();
    }

    @Test
    public void shouldNotBeAbleToFindAnItemWhichDoesNotExistInCache() throws Exception {
        assertNull(cache.get("something-which-does-not-exist"));
    }

    @Test
    public void shouldBeAbleToPutAnItemIntoCache() throws Exception {
        ProjectStatus status = new ProjectStatus("item1", "Sleeping", "last-build-status", "last-build-label", new Date(), "web-url");

        cache.put(status);

        assertThat(cache.get("item1"), is(status));
    }

    @Test
    public void shouldBeAbleToPutMultipleItemsIntoCache() throws Exception {
        ProjectStatus status1 = new ProjectStatus("item1", "Sleeping", "last-build-status", "last-build-label", new Date(), "web-url");
        ProjectStatus status2 = new ProjectStatus("item2", "Sleeping", "last-build-status", "last-build-label", new Date(), "web-url");
        ProjectStatus status3 = new ProjectStatus("item3", "Sleeping", "last-build-status", "last-build-label", new Date(), "web-url");

        cache.putAll(asList(status1, status2, status3));

        assertThat(cache.get("item1"), is(status1));
        assertThat(cache.get("item2"), is(status2));
        assertThat(cache.get("item3"), is(status3));
    }

    @Test
    public void shouldBeAbleToReplaceAnItemInCache() throws Exception {
        ProjectStatus firstStatus = new ProjectStatus("item1", "Sleeping 1", "last-build-status 1", "last-build-label 1", new Date(), "web-url 1");
        ProjectStatus nextStatus = new ProjectStatus("item1", "Sleeping 2", "last-build-status 2", "last-build-label 2", new Date(), "web-url 2");

        cache.put(firstStatus);
        cache.put(nextStatus);

        assertThat(cache.get("item1"), is(nextStatus));
    }

    @Test
    public void shouldBeAbleToReplaceMultipleItemsInCache() throws Exception {
        ProjectStatus firstStatusOfItem1 = new ProjectStatus("item1", "Sleeping 1", "last-build-status 1", "last-build-label 1", new Date(), "web-url 1");
        ProjectStatus nextStatusOfItem1 = new ProjectStatus("item1", "Sleeping 2", "last-build-status 2", "last-build-label 2", new Date(), "web-url 2");

        ProjectStatus status2 = new ProjectStatus("item2", "Sleeping", "last-build-status", "last-build-label", new Date(), "web-url");

        ProjectStatus firstStatusOfItem3 = new ProjectStatus("item3", "Sleeping A", "last-build-status A", "last-build-label A", new Date(), "web-url A");
        ProjectStatus nextStatusOfItem3 = new ProjectStatus("item3", "Sleeping B", "last-build-status B", "last-build-label B", new Date(), "web-url B");

        cache.put(firstStatusOfItem1);
        cache.put(status2);
        cache.put(firstStatusOfItem3);

        cache.putAll(asList(nextStatusOfItem1, status2, nextStatusOfItem3));

        assertThat(cache.get("item1"), is(nextStatusOfItem1));
        assertThat(cache.get("item2"), is(status2));
        assertThat(cache.get("item3"), is(nextStatusOfItem3));
    }

    @Test
    public void shouldBeAbleToClearExistingCacheAndReplaceAllItemsInIt() throws Exception {
        ProjectStatus status1 = new ProjectStatus("item1", "Sleeping 1", "last-build-status 1", "last-build-label 1", new Date(), "web-url 1");
        ProjectStatus status2 = new ProjectStatus("item2", "Sleeping 2", "last-build-status 2", "last-build-label 2", new Date(), "web-url 2");
        ProjectStatus status3 = new ProjectStatus("item3", "Sleeping 3", "last-build-status 3", "last-build-label 3", new Date(), "web-url 3");
        ProjectStatus status4 = new ProjectStatus("item4", "Sleeping 4", "last-build-status 4", "last-build-label 4", new Date(), "web-url 4");
        ProjectStatus status5 = new ProjectStatus("item5", "Sleeping 5", "last-build-status 5", "last-build-label 5", new Date(), "web-url 5");

        cache.put(status1);
        cache.put(status2);
        cache.put(status3);

        cache.replaceAllEntriesInCacheWith(asList(status3, status4, status5));

        assertThat(cache.get("item1"), is(nullValue()));
        assertThat(cache.get("item2"), is(nullValue()));
        assertThat(cache.get("item3"), is(status3));
        assertThat(cache.get("item4"), is(status4));
        assertThat(cache.get("item5"), is(status5));
    }

    @Test
    public void shouldProvideAnOrderedListOfAllItemsInCache() throws Exception {
        ProjectStatus status1 = new ProjectStatus("item1", "Sleeping 1", "last-build-status 1", "last-build-label 1", new Date(), "web-url 1");
        ProjectStatus status2 = new ProjectStatus("item2", "Sleeping 2", "last-build-status 2", "last-build-label 2", new Date(), "web-url 2");
        ProjectStatus status3 = new ProjectStatus("item3", "Sleeping 3", "last-build-status 3", "last-build-label 3", new Date(), "web-url 3");

        cache.replaceAllEntriesInCacheWith(asList(status1, status2, status3));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();

        assertThat(allProjects.get(0), is(status1));
        assertThat(allProjects.get(1), is(status2));
        assertThat(allProjects.get(2), is(status3));
    }

    @Test
    public void shouldContainChangedEntryInOrderedListAfterAPut() throws Exception {
        ProjectStatus status1 = new ProjectStatus("item1", "Sleeping 1", "last-build-status 1", "last-build-label 1", new Date(), "web-url 1");
        ProjectStatus status2 = new ProjectStatus("item2", "Sleeping 2", "last-build-status 2", "last-build-label 2", new Date(), "web-url 2");
        ProjectStatus status3 = new ProjectStatus("item3", "Sleeping 3", "last-build-status 3", "last-build-label 3", new Date(), "web-url 3");
        ProjectStatus status2_changed = new ProjectStatus("item2", "CHANGED Sleeping 2C", "last-build-status 2C", "last-build-label 2C", new Date(), "web-url 2C");

        cache.replaceAllEntriesInCacheWith(asList(status1, status2, status3));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();
        assertThat(allProjects.get(1), is(status2));


        cache.put(status2_changed);
        allProjects = cache.allEntriesInOrder();


        assertThat(allProjects.get(0), is(status1));
        assertThat(allProjects.get(1), is(status2_changed));
        assertThat(allProjects.get(2), is(status3));
    }

    @Test
    public void shouldContainChangedEntriesInOrderedListAfterAPutAll() throws Exception {
        ProjectStatus status1 = new ProjectStatus("item1", "Sleeping 1", "last-build-status 1", "last-build-label 1", new Date(), "web-url 1");
        ProjectStatus status2 = new ProjectStatus("item2", "Sleeping 2", "last-build-status 2", "last-build-label 2", new Date(), "web-url 2");
        ProjectStatus status3 = new ProjectStatus("item3", "Sleeping 3", "last-build-status 3", "last-build-label 3", new Date(), "web-url 3");
        ProjectStatus status1_changed = new ProjectStatus("item1", "CHANGED Sleeping 1C", "last-build-status 1C", "last-build-label 1C", new Date(), "web-url 1C");
        ProjectStatus status2_changed = new ProjectStatus("item2", "CHANGED Sleeping 2C", "last-build-status 2C", "last-build-label 2C", new Date(), "web-url 2C");

        cache.replaceAllEntriesInCacheWith(asList(status1, status2, status3));
        cache.allEntriesInOrder();


        cache.putAll(asList(status2_changed, status1_changed));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();


        assertThat(allProjects.get(0), is(status1_changed));
        assertThat(allProjects.get(1), is(status2_changed));
        assertThat(allProjects.get(2), is(status3));
    }
}