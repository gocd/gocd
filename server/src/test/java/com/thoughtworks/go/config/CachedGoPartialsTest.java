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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.service.EntityHashes;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    public void setUp() {
        serverHealthService = new ServerHealthService(mock());
        partials = new CachedGoPartials(serverHealthService, new PartialConfigHelper(new EntityHashes(ConfigElementImplementationRegistryMother.withNoPlugins())));
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
    public void shouldReplaceValidPartialWhenStructureChangesAtTheSameOrigin() {
        // e.g. a plugin doing branch/PR scanning produces different output for the same material revision
        partials.markAsValid(fingerprintForRepo1, part1);

        PartialConfig part1WithExtraPipeline = PartialConfigMother.withPipeline("p1-and-p3", new RepoConfigOrigin(configRepo1, "1"));
        assertEquals(part1.getOrigin(), part1WithExtraPipeline.getOrigin());

        partials.markAsValid(fingerprintForRepo1, part1WithExtraPipeline);

        assertSame(part1WithExtraPipeline, partials.getValid(fingerprintForRepo1));
        assertFalse(partials.lastValidPartials().contains(part1));
    }

    @Test
    public void shouldReplaceValidPartialWhenOriginChanges() {
        partials.markAsValid(fingerprintForRepo1, part1);

        PartialConfig part1AtNewRevision = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(configRepo1, "2"));
        partials.markAsValid(fingerprintForRepo1, part1AtNewRevision);

        assertSame(part1AtNewRevision, partials.getValid(fingerprintForRepo1));
    }

    @Test
    public void shouldNotClearServerHealthMessageWhenReMarkingAnEquivalentPartialAsValid() {
        // Fallback path: a save that fails on last-known partials is retried with last-valid partials, and re-marks
        // them as valid. That must not clear the health message describing why the newer partial was rejected.
        partials.markAsValid(fingerprintForRepo1, part1);
        serverHealthService.update(ServerHealthState.error("error-repo-1", "latest partial is broken", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1))));

        PartialConfig equivalentToPart1 = PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(configRepo1, "1"));
        partials.markAsValid(fingerprintForRepo1, equivalentToPart1);

        assertSame(part1, partials.getValid(fingerprintForRepo1));
        assertEquals("error-repo-1", serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).getFirst().getMessage());
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
        assertTrue(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(configRepo1)).isEmpty());
        assertFalse(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(configRepo2)).isEmpty());
    }

    @Test
    public void shouldRemoveServerHealthMessageForPartialWhenItIsMarkedAsValid() {
        serverHealthService.update(ServerHealthState.error("error-repo-1", "error-desc-1", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1))));
        serverHealthService.update(ServerHealthState.error("error-repo-2", "error-desc-2", HealthStateType.general(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2))));
        partials.markAsValid(fingerprintForRepo1, part1);
        assertTrue(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).isEmpty());
        assertEquals("error-repo-2", serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2)).getFirst().getMessage());
        partials.markAllKnownAsValid();
        assertTrue(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo1)).isEmpty());
        assertTrue(serverHealthService.logsSortedForScope(HealthStateScope.forPartialConfigRepo(fingerprintForRepo2)).isEmpty());
    }
}
