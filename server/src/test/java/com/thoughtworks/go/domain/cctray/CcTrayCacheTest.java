/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.security.users.Users;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CcTrayCacheTest {
    private CcTrayCache cache;

    private int statusCounter = 1;

    @BeforeEach
    public void setUp() {
        cache = new CcTrayCache();
    }

    @Test
    public void shouldNotBeAbleToFindAnItemWhichDoesNotExistInCache() {
        assertNull(cache.get("something-which-does-not-exist"));
    }

    @Test
    public void shouldBeAbleToPutAnItemIntoCache() {
        ProjectStatus status = viewableStatusFor("item1");

        cache.put(status);

        assertThat(cache.get("item1")).isEqualTo(status);
    }

    @Test
    public void shouldBeAbleToPutMultipleItemsIntoCache() {
        ProjectStatus status1 = viewableStatusFor("item1");
        ProjectStatus status2 = viewableStatusFor("item2");
        ProjectStatus status3 = viewableStatusFor("item3");

        cache.putAll(List.of(status1, status2, status3));

        assertThat(cache.get("item1")).isEqualTo(status1);
        assertThat(cache.get("item2")).isEqualTo(status2);
        assertThat(cache.get("item3")).isEqualTo(status3);
    }

    @Test
    public void shouldBeAbleToReplaceAnItemInCache() {
        ProjectStatus firstStatus = viewableStatusFor("item1");
        ProjectStatus nextStatus = viewableStatusFor("item1");

        cache.put(firstStatus);
        cache.put(nextStatus);

        assertThat(cache.get("item1")).isEqualTo(nextStatus);
    }

    @Test
    public void shouldBeAbleToReplaceMultipleItemsInCache() {
        ProjectStatus firstStatusOfItem1 = viewableStatusFor("item1");
        ProjectStatus nextStatusOfItem1 = viewableStatusFor("item1");

        ProjectStatus status2 = viewableStatusFor("item2");

        ProjectStatus firstStatusOfItem3 = viewableStatusFor("item3");
        ProjectStatus nextStatusOfItem3 = viewableStatusFor("item3");

        cache.put(firstStatusOfItem1);
        cache.put(status2);
        cache.put(firstStatusOfItem3);

        cache.putAll(List.of(nextStatusOfItem1, status2, nextStatusOfItem3));

        assertThat(cache.get("item1")).isEqualTo(nextStatusOfItem1);
        assertThat(cache.get("item2")).isEqualTo(status2);
        assertThat(cache.get("item3")).isEqualTo(nextStatusOfItem3);
    }

    @Test
    public void shouldBeAbleToClearExistingCacheAndReplaceAllItemsInIt() {
        ProjectStatus status1 = viewableStatusFor("item1");
        ProjectStatus status2 = viewableStatusFor("item2");
        ProjectStatus status3 = viewableStatusFor("item3");
        ProjectStatus status4 = viewableStatusFor("item4");
        ProjectStatus status5 = viewableStatusFor("item5");

        cache.put(status1);
        cache.put(status2);
        cache.put(status3);

        cache.replaceAllEntriesInCacheWith(List.of(status3, status4, status5));

        assertThat(cache.get("item1")).isNull();
        assertThat(cache.get("item2")).isNull();
        assertThat(cache.get("item3")).isEqualTo(status3);
        assertThat(cache.get("item4")).isEqualTo(status4);
        assertThat(cache.get("item5")).isEqualTo(status5);
    }

    @Test
    public void shouldProvideAnOrderedListOfAllItemsInCache() {
        ProjectStatus status1 = viewableStatusFor("item1");
        ProjectStatus status2 = viewableStatusFor("item2");
        ProjectStatus status3 = viewableStatusFor("item3");

        cache.replaceAllEntriesInCacheWith(List.of(status1, status2, status3));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();

        assertThat(allProjects.get(0)).isEqualTo(status1);
        assertThat(allProjects.get(1)).isEqualTo(status2);
        assertThat(allProjects.get(2)).isEqualTo(status3);
    }

    @Test
    public void shouldContainChangedEntryInOrderedListAfterAPut() {
        ProjectStatus status1 = viewableStatusFor("item1");
        ProjectStatus status2 = viewableStatusFor("item2");
        ProjectStatus status3 = viewableStatusFor("item3");
        ProjectStatus status2_changed = viewableStatusFor("item2");

        cache.replaceAllEntriesInCacheWith(List.of(status1, status2, status3));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();
        assertThat(allProjects.get(1)).isEqualTo(status2);

        cache.put(status2_changed);
        allProjects = cache.allEntriesInOrder();

        assertThat(allProjects.get(0)).isEqualTo(status1);
        assertThat(allProjects.get(1)).isEqualTo(status2_changed);
        assertThat(allProjects.get(2)).isEqualTo(status3);
    }

    @Test
    public void shouldContainChangedEntriesInOrderedListAfterAPutAll() {
        ProjectStatus status1 = viewableStatusFor("item1");
        ProjectStatus status2 = viewableStatusFor("item2");
        ProjectStatus status3 = viewableStatusFor("item3");
        ProjectStatus status1_changed = viewableStatusFor("item1");
        ProjectStatus status2_changed = viewableStatusFor("item2");

        cache.replaceAllEntriesInCacheWith(List.of(status1, status2, status3));
        cache.allEntriesInOrder();


        cache.putAll(List.of(status2_changed, status1_changed));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();

        assertThat(allProjects.get(0)).isEqualTo(status1_changed);
        assertThat(allProjects.get(1)).isEqualTo(status2_changed);
        assertThat(allProjects.get(2)).isEqualTo(status3);
    }

    private @NonNull ProjectStatus viewableStatusFor(String name) {
        ProjectStatus status = new ProjectStatus(name, "Sleeping", "Sleeping", "last-build-label-" + (statusCounter++), new Date(), "web-url");
        status.updateViewers(Users.EVERYONE);
        return status;
    }
}
