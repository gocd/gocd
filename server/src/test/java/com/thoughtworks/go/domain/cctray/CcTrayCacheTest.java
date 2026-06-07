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

import static com.thoughtworks.go.domain.cctray.ProjectStatus.Key.keyFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CcTrayCacheTest {
    private static final String PIPELINE_NAME = "pipeline1";
    private static final ProjectStatus.Key STAGE_3 = keyFrom(PIPELINE_NAME, "stage3");
    private static final ProjectStatus.Key STAGE_2 = keyFrom(PIPELINE_NAME, "stage2");
    private static final ProjectStatus.Key STAGE_1 = keyFrom(PIPELINE_NAME, "stage1");

    private CcTrayCache cache;

    private int statusCounter = 1;

    @BeforeEach
    public void setUp() {
        cache = new CcTrayCache();
    }

    @Test
    public void shouldNotBeAbleToFindAnItemWhichDoesNotExistInCache() {
        assertNull(cache.get(keyFrom("something-which-does-not-exist")));
    }

    @Test
    public void shouldBeAbleToFindADefaultValueForAnItemWhichDoesNotExistInCache() {
        ProjectStatus.Key key = keyFrom("something-which-does-not-exist");
        assertThat(cache.getOrDefault(key, 43))
            .isEqualTo(new ProjectStatus.NullProjectStatus(key, 43));
    }

    @Test
    public void shouldBeAbleToPutAnItemIntoCache() {
        ProjectStatus status = viewableStatusFor(STAGE_1);

        cache.put(status);

        assertThat(cache.get(STAGE_1)).isEqualTo(status);
    }

    @Test
    public void shouldBeAbleToPutMultipleItemsIntoCache() {
        ProjectStatus status1 = viewableStatusFor(STAGE_1);
        ProjectStatus status2 = viewableStatusFor(STAGE_2);
        ProjectStatus status3 = viewableStatusFor(STAGE_3);

        cache.replaceForPipeline(PIPELINE_NAME, List.of(status1, status2, status3));

        assertThat(cache.get(STAGE_1)).isEqualTo(status1);
        assertThat(cache.get(STAGE_2)).isEqualTo(status2);
        assertThat(cache.get(STAGE_3)).isEqualTo(status3);
    }

    @Test
    public void shouldBeAbleToReplaceAnItemInCache() {
        ProjectStatus firstStatus = viewableStatusFor(STAGE_1);
        ProjectStatus nextStatus = viewableStatusFor(STAGE_1);

        cache.put(firstStatus);
        cache.put(nextStatus);

        assertThat(cache.get(STAGE_1)).isEqualTo(nextStatus);
    }

    @Test
    public void shouldBeAbleToReplaceMultipleItemsInCache() {
        ProjectStatus firstStatusOfItem1 = viewableStatusFor(STAGE_1);
        ProjectStatus nextStatusOfItem1 = viewableStatusFor(STAGE_1);

        ProjectStatus status2 = viewableStatusFor(STAGE_2);

        ProjectStatus firstStatusOfItem3 = viewableStatusFor(STAGE_3);
        ProjectStatus nextStatusOfItem3 = viewableStatusFor(STAGE_3);

        cache.put(firstStatusOfItem1);
        cache.put(status2);
        cache.put(firstStatusOfItem3);

        cache.replaceForPipeline(PIPELINE_NAME, List.of(nextStatusOfItem1, status2, nextStatusOfItem3));

        assertThat(cache.get(STAGE_1)).isEqualTo(nextStatusOfItem1);
        assertThat(cache.get(STAGE_2)).isEqualTo(status2);
        assertThat(cache.get(STAGE_3)).isEqualTo(nextStatusOfItem3);
    }

    @Test
    public void shouldBeAbleToRemoveIrrelevantPipelineItemsDuringReplacement() {
        ProjectStatus firstStatusOfItem1 = viewableStatusFor(STAGE_1);
        ProjectStatus nextStatusOfItem1 = viewableStatusFor(STAGE_1);
        ProjectStatus status2 = viewableStatusFor(STAGE_2);

        ProjectStatus otherPipeline = viewableStatusFor(keyFrom("pipeline2", "stage1"));

        cache.put(firstStatusOfItem1);
        cache.put(status2);
        cache.put(otherPipeline);

        cache.replaceForPipeline(PIPELINE_NAME, List.of(nextStatusOfItem1));

        assertThat(cache.get(STAGE_1)).isEqualTo(nextStatusOfItem1);
        assertThat(cache.get(STAGE_2)).isNull();
        assertThat(cache.get(otherPipeline.key())).isEqualTo(otherPipeline);
    }

    @Test
    public void shouldBeAbleToRemoveIrrelevantStageItemsDuringReplacement() {
        ProjectStatus firstStatusOfItem1 = viewableStatusFor(STAGE_1);
        ProjectStatus nextStatusOfItem1 = viewableStatusFor(STAGE_1);
        ProjectStatus otherStage = viewableStatusFor(keyFrom(STAGE_1.pipeline(), STAGE_1.stage(), "some-job"));
        ProjectStatus status2 = viewableStatusFor(STAGE_2);

        ProjectStatus otherPipeline = viewableStatusFor(keyFrom("pipeline2", "stage1"));

        cache.put(firstStatusOfItem1);
        cache.put(status2);
        cache.put(otherStage);
        cache.put(otherPipeline);

        cache.replaceForStage(STAGE_1.pipeline(), STAGE_1.stage(), List.of(nextStatusOfItem1));

        assertThat(cache.get(STAGE_1)).isEqualTo(nextStatusOfItem1);
        assertThat(cache.get(STAGE_2)).isEqualTo(status2);
        assertThat(cache.get(otherStage.key())).isNull();
        assertThat(cache.get(otherPipeline.key())).isEqualTo(otherPipeline);
    }

    @Test
    public void shouldBeAbleToClearExistingCacheAndReplaceAllItemsInIt() {
        ProjectStatus status1 = viewableStatusFor(STAGE_1);
        ProjectStatus status2 = viewableStatusFor(STAGE_2);
        ProjectStatus status3 = viewableStatusFor(STAGE_3);
        ProjectStatus status4 = viewableStatusFor(keyFrom(PIPELINE_NAME, "stage4"));
        ProjectStatus status5 = viewableStatusFor(keyFrom(PIPELINE_NAME, "stage5"));

        cache.put(status1);
        cache.put(status2);
        cache.put(status3);

        cache.replaceAll(List.of(status3, status4, status5));

        assertThat(cache.get(STAGE_1)).isNull();
        assertThat(cache.get(STAGE_2)).isNull();
        assertThat(cache.get(STAGE_3)).isEqualTo(status3);
        assertThat(cache.get(status4.key())).isEqualTo(status4);
        assertThat(cache.get(status5.key())).isEqualTo(status5);
    }

    @Test
    public void shouldProvideAnOrderedListOfAllItemsInCache() {
        ProjectStatus status1 = viewableStatusFor(STAGE_1);
        ProjectStatus status2 = viewableStatusFor(STAGE_2);
        ProjectStatus status3 = viewableStatusFor(STAGE_3);

        cache.replaceAll(List.of(status1, status2, status3));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();

        assertThat(allProjects).containsExactly(status1, status2, status3);
    }

    @Test
    public void shouldContainChangedEntryInOrderedListAfterAPut() {
        ProjectStatus status1 = viewableStatusFor(STAGE_1);
        ProjectStatus status2 = viewableStatusFor(STAGE_2);
        ProjectStatus status3 = viewableStatusFor(STAGE_3);
        ProjectStatus status2_changed = viewableStatusFor(STAGE_2);

        cache.replaceAll(List.of(status1, status2, status3));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();
        assertThat(allProjects.get(1)).isEqualTo(status2);

        cache.put(status2_changed);
        allProjects = cache.allEntriesInOrder();

        assertThat(allProjects).containsExactly(status1, status2_changed, status3);
    }

    @Test
    public void shouldContainChangedEntriesInOrderedListAfterAPutAll() {
        ProjectStatus status1 = viewableStatusFor(STAGE_1, 3);
        ProjectStatus status2 = viewableStatusFor(STAGE_2, 2);
        ProjectStatus status3 = viewableStatusFor(STAGE_3, 1);
        ProjectStatus status1_changed = viewableStatusFor(STAGE_1, 3);
        ProjectStatus status2_changed = viewableStatusFor(STAGE_2, 2);

        cache.replaceAll(List.of(status1, status2, status3));

        cache.replaceForPipeline(PIPELINE_NAME, List.of(status2_changed, status1_changed));
        List<ProjectStatus> allProjects = cache.allEntriesInOrder();

        assertThat(allProjects).containsExactly(status2_changed, status1_changed);
    }

    private @NonNull ProjectStatus viewableStatusFor(ProjectStatus.Key key) {
        return viewableStatusFor(key, 0);
    }

    private @NonNull ProjectStatus viewableStatusFor(ProjectStatus.Key key, int stageOrder) {
        ProjectStatus status = new ProjectStatus(key, stageOrder, "Sleeping", "Sleeping", "last-build-label-" + (statusCounter++), new Date(), "web-url");
        status.updateViewers(Users.EVERYONE);
        return status;
    }
}
