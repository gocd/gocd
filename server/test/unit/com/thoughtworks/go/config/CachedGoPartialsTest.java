/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CachedGoPartialsTest {

    private CachedGoPartials partials;
    private PartialConfig part1;
    private PartialConfig part2;
    private ServerHealthService serverHealthService;
    private String fingerprintForRepo1;
    private String fingerprintForRepo2;
    private ConfigRepoConfig configRepo1;
    private ConfigRepoConfig configRepo2;

    @Before
    public void setUp() throws Exception {
        serverHealthService = new ServerHealthService();
        partials = new CachedGoPartials(serverHealthService);
        configRepo1 = new ConfigRepoConfig(new GitMaterialConfig("url1"), "plugin");
        part1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(configRepo1, "1"));
        configRepo2 = new ConfigRepoConfig(new GitMaterialConfig("url2"), "plugin");
        part2 = PartialConfigMother.withPipeline("p2", new RepoConfigOrigin(configRepo2, "1"));
        partials.addOrUpdate(configRepo1.getMaterialConfig().getFingerprint(), part1);
        partials.addOrUpdate(configRepo2.getMaterialConfig().getFingerprint(), part2);
        fingerprintForRepo1 = ((RepoConfigOrigin) part1.getOrigin()).getMaterial().getFingerprint();
        fingerprintForRepo2 = ((RepoConfigOrigin) part2.getOrigin()).getMaterial().getFingerprint();
    }

    @Test
    public void shouldMarkAPartialAsValid() {
        partials.markAsValid(fingerprintForRepo1, part1);
        assertThat(partials.lastValidPartials().contains(part1), is(true));
        assertThat(partials.lastValidPartials().contains(part2), is(false));
    }

    @Test
    public void shouldMarkAllKnownAsValid() {
        partials.markAllKnownAsValid();
        assertThat(partials.lastValidPartials().contains(part1), is(true));
        assertThat(partials.lastValidPartials().contains(part2), is(true));
    }

    @Test
    public void shouldRemoveValid() {
        partials.markAllKnownAsValid();
        partials.removeValid(fingerprintForRepo1);
        assertThat(partials.lastValidPartials().contains(part1), is(false));
        assertThat(partials.lastValidPartials().contains(part2), is(true));
    }

    @Test
    public void shouldRemoveKnown() {
        partials.removeKnown(fingerprintForRepo1);
        assertThat(partials.lastKnownPartials().contains(part1), is(false));
        assertThat(partials.lastKnownPartials().contains(part2), is(true));
    }

    @Test
    public void shouldRemoveServerHealthMessageForAPartialWhenItsRemovedFromKnownList() {
        serverHealthService.update(ServerHealthState.error("err_repo1", "err desc", HealthStateType.general(HealthStateScope.forPartialConfigRepo(configRepo1))));
        serverHealthService.update(ServerHealthState.error("err_repo2", "err desc", HealthStateType.general(HealthStateScope.forPartialConfigRepo(configRepo2))));
        partials.removeKnown(configRepo1.getMaterialConfig().getFingerprint());
        assertThat(partials.lastKnownPartials().contains(part1), is(false));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo2)).isEmpty(), is(false));
    }

    @Test
    public void shouldRemoveServerHealthMessageForPartialWhenItIsMarkedAsValid() {
        serverHealthService.update(ServerHealthState.error("error-repo-1", "error-desc-1", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1))));
        serverHealthService.update(ServerHealthState.error("error-repo-2", "error-desc-2", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2))));
        partials.markAsValid(fingerprintForRepo1, part1);
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2)).get(0).getMessage(), is("error-repo-2"));
        partials.markAllKnownAsValid();
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2)).isEmpty(), is(true));
    }
}