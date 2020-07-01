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

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.server.service.EntityHashes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PartialConfigHelper {
    private final EntityHashes hashes;

    @Autowired
    public PartialConfigHelper(EntityHashes hashes) {
        this.hashes = hashes;
    }

    /**
     * Tests whether two {@link PartialConfig} instances have identical structure and origin
     *
     * @param left  a {@link PartialConfig}
     * @param right a {@link PartialConfig}
     * @return whether or not the {@link PartialConfig}s are equivalent
     */
    public boolean isEquivalent(PartialConfig left, PartialConfig right) {
        return hasSameOrigins(left, right) && isStructurallyEquivalent(left, right);
    }

    /**
     * Determines the equivalence of to collections of {@link PartialConfig}s
     *
     * @param left  a {@link Collection<PartialConfig>}
     * @param right a {@link Collection<PartialConfig>}
     * @return whether or not the {@link Collection<PartialConfig>}s are equivalent
     */
    public boolean isEquivalent(Collection<PartialConfig> left, Collection<PartialConfig> right) {
        return Objects.equals(identities(left), identities(right));
    }

    private Set<String> identities(Collection<PartialConfig> collection) {
        if (null == collection) return Collections.emptySet();
        return collection.stream().map(this::identity).collect(Collectors.toSet());
    }

    private String identity(PartialConfig partial) {
        final RepoConfigOrigin origin = (RepoConfigOrigin) partial.getOrigin();
        return hash(partial) + ":" + origin.getMaterial().getFingerprint() + ":" + origin.getRevision();
    }

    /**
     * Tests whether two {@link PartialConfig} instances define structurally identical configurations.
     *
     * @param previous a {@link PartialConfig}
     * @param incoming a {@link PartialConfig}
     * @return whether or not the structures are identical
     */
    private boolean isStructurallyEquivalent(PartialConfig previous, PartialConfig incoming) {
        return hash(incoming).equals(hash(previous));
    }

    /**
     * Tests whether two {@link PartialConfig} instances share an identical
     * {@link com.thoughtworks.go.config.remote.ConfigOrigin}.
     * <p>
     * This is needed because we need to update the origins of the generated {@link PipelineConfig} instances to match
     * the revisions of their {@link com.thoughtworks.go.domain.materials.MaterialConfig}s. If they don't, the pipelines
     * will not be scheduled to build.
     * <p>
     * See {@link com.thoughtworks.go.domain.buildcause.BuildCause#pipelineConfigAndMaterialRevisionMatch(PipelineConfig)}.
     *
     * @param previous a {@link PartialConfig}
     * @param incoming a {@link PartialConfig}
     * @return whether or not the origins are identical
     */
    private boolean hasSameOrigins(PartialConfig previous, PartialConfig incoming) {
        return Objects.equals(
                Optional.ofNullable(previous).map(PartialConfig::getOrigin).orElse(null),
                Optional.ofNullable(incoming).map(PartialConfig::getOrigin).orElse(null)
        );
    }

    private String hash(PartialConfig partial) {
        return hashes.digestPartial(partial);
    }
}
