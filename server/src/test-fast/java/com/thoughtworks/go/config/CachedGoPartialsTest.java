/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.junit.jupiter.api.Assertions.*;

public class CachedGoPartialsTest {

    private CachedGoPartials partials;
    private PartialConfig part1;
    private PartialConfig part2;
    private ServerHealthService serverHealthService;
    private String fingerprintForRepo1;
    private String fingerprintForRepo2;
    private ConfigRepoConfig configRepo1;
    private ConfigRepoConfig configRepo2;

    @BeforeEach
    public void setUp() throws Exception {
        serverHealthService = new ServerHealthService();
        partials = new CachedGoPartials(serverHealthService);
        configRepo1 = ConfigRepoConfig.createConfigRepoConfig(git("url1"), "plugin", "id1");
        part1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(configRepo1, "1"));
        configRepo2 = ConfigRepoConfig.createConfigRepoConfig(git("url2"), "plugin", "id2");
        part2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(configRepo2, "1"));
        partials.cacheAsLastKnown(configRepo1.getRepo().getFingerprint(), part1);
        partials.cacheAsLastKnown(configRepo2.getRepo().getFingerprint(), part2);
        fingerprintForRepo1 = ((RepoConfigOrigin) part1.getOrigin()).getMaterial().getFingerprint();
        fingerprintForRepo2 = ((RepoConfigOrigin) part2.getOrigin()).getMaterial().getFingerprint();
    }

    @Test
    public void shouldMarkAPartialAsValid() {
        partials.markAsValid(fingerprintForRepo1, part1);
        assertTrue(partials.lastValidPartials().contains(part1));
        assertFalse(partials.lastValidPartials().contains(part2));
    }

    @Test
    public void shouldMarkAllKnownAsValid() {
        partials.markAllKnownAsValid();
        assertTrue(partials.lastValidPartials().contains(part1));
        assertTrue(partials.lastValidPartials().contains(part2));
    }

    @Test
    public void shouldRemoveValid() {
        partials.markAllKnownAsValid();
        partials.removeValid(fingerprintForRepo1);
        assertFalse(partials.lastValidPartials().contains(part1));
        assertTrue(partials.lastValidPartials().contains(part2));
    }

    @Test
    public void shouldRemoveKnown() {
        partials.removeKnown(fingerprintForRepo1);
        assertFalse(partials.lastKnownPartials().contains(part1));
        assertTrue(partials.lastKnownPartials().contains(part2));
    }

    @Test
    public void shouldRemoveServerHealthMessageForAPartialWhenItsRemovedFromKnownList() {
        serverHealthService.update(ServerHealthState.error("err_repo1", "err desc", HealthStateType.general(HealthStateScope.forPartialConfigRepo(configRepo1))));
        serverHealthService.update(ServerHealthState.error("err_repo2", "err desc", HealthStateType.general(HealthStateScope.forPartialConfigRepo(configRepo2))));
        partials.removeKnown(configRepo1.getRepo().getFingerprint());
        assertFalse(partials.lastKnownPartials().contains(part1));
        assertTrue(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo1)).isEmpty());
        assertFalse(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo2)).isEmpty());
    }

    @Test
    public void shouldRemoveServerHealthMessageForPartialWhenItIsMarkedAsValid() {
        serverHealthService.update(ServerHealthState.error("error-repo-1", "error-desc-1", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1))));
        serverHealthService.update(ServerHealthState.error("error-repo-2", "error-desc-2", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2))));
        partials.markAsValid(fingerprintForRepo1, part1);
        assertTrue(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).isEmpty());
        assertEquals("error-repo-2", serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2)).get(0).getMessage());
        partials.markAllKnownAsValid();
        assertTrue(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).isEmpty());
        assertTrue(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2)).isEmpty());
    }
}
