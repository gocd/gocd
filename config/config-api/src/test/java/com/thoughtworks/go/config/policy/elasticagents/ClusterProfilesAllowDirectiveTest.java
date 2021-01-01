/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.policy.elasticagents;

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.policy.Allow;
import com.thoughtworks.go.config.policy.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ClusterProfilesAllowDirectiveTest {
    @Test
    void forViewOfAllClusterProfiles() {
        Allow directive = new Allow("view", "cluster_profile", "*");

        Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
        Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
        Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
        Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);

        assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.ALLOW);
        assertThat(viewAllClusterProfiles).isEqualTo(Result.ALLOW);
        assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.SKIP);
        assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
    }

    @Test
    void forAdministerOfAllClusterProfiles() {
        Allow directive = new Allow("administer", "cluster_profile", "*");

        Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
        Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
        Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
        Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);

        assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.ALLOW);
        assertThat(viewAllClusterProfiles).isEqualTo(Result.ALLOW);
        assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.ALLOW);
        assertThat(administerAllClusterProfiles).isEqualTo(Result.ALLOW);
    }

    @Test
    void forViewOfWildcardDefinedClusterProfile() {
        Allow directive = new Allow("view", "cluster_profile", "team1_*");

        Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
        Result viewAllElasticAgentProfilesUnderTeam1 = directive.apply("view", ElasticProfile.class, "*", "team1_uat");
        Result viewAllElasticAgentProfilesUnderTeam2 = directive.apply("view", ElasticProfile.class, "*", "team2_uat");

        Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
        Result viewTeam1ClusterProfile = directive.apply("view", ClusterProfile.class, "team1_uat", null);
        Result viewTeam2ClusterProfile = directive.apply("view", ClusterProfile.class, "team2_uat", null);

        Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
        Result administerAllElasticAgentProfilesUnderTeam1 = directive.apply("administer", ElasticProfile.class, "*", "team1_uat");
        Result administerAllElasticAgentProfilesUnderTeam2 = directive.apply("administer", ElasticProfile.class, "*", "team2_uat");

        Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);
        Result administerTeam1ClusterProfile = directive.apply("administer", ClusterProfile.class, "team1_uat", null);
        Result administerTeam2ClusterProfile = directive.apply("administer", ClusterProfile.class, "team2_uat", null);

        assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.SKIP);
        assertThat(viewAllElasticAgentProfilesUnderTeam1).isEqualTo(Result.ALLOW);
        assertThat(viewAllElasticAgentProfilesUnderTeam2).isEqualTo(Result.SKIP);

        assertThat(viewAllClusterProfiles).isEqualTo(Result.SKIP);
        assertThat(viewTeam1ClusterProfile).isEqualTo(Result.ALLOW);
        assertThat(viewTeam2ClusterProfile).isEqualTo(Result.SKIP);

        assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.SKIP);
        assertThat(administerAllElasticAgentProfilesUnderTeam1).isEqualTo(Result.SKIP);
        assertThat(administerAllElasticAgentProfilesUnderTeam2).isEqualTo(Result.SKIP);

        assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
        assertThat(administerTeam1ClusterProfile).isEqualTo(Result.SKIP);
        assertThat(administerTeam2ClusterProfile).isEqualTo(Result.SKIP);
    }

    @Test
    void forAdministerOfWildcardDefinedClusterProfile() {
        Allow directive = new Allow("administer", "cluster_profile", "team1_*");

        Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
        Result viewAllElasticAgentProfilesUnderTeam1 = directive.apply("view", ElasticProfile.class, "*", "team1_uat");
        Result viewAllElasticAgentProfilesUnderTeam2 = directive.apply("view", ElasticProfile.class, "*", "team2_uat");

        Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
        Result viewTeam1ClusterProfile = directive.apply("view", ClusterProfile.class, "team1_uat", null);
        Result viewTeam2ClusterProfile = directive.apply("view", ClusterProfile.class, "team2_uat", null);

        Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
        Result administerAllElasticAgentProfilesUnderTeam1 = directive.apply("administer", ElasticProfile.class, "*", "team1_uat");
        Result administerAllElasticAgentProfilesUnderTeam2 = directive.apply("administer", ElasticProfile.class, "*", "team2_uat");

        Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);
        Result administerTeam1ClusterProfile = directive.apply("administer", ClusterProfile.class, "team1_uat", null);
        Result administerTeam2ClusterProfile = directive.apply("administer", ClusterProfile.class, "team2_uat", null);

        assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.SKIP);
        assertThat(viewAllElasticAgentProfilesUnderTeam1).isEqualTo(Result.ALLOW);
        assertThat(viewAllElasticAgentProfilesUnderTeam2).isEqualTo(Result.SKIP);

        assertThat(viewAllClusterProfiles).isEqualTo(Result.SKIP);
        assertThat(viewTeam1ClusterProfile).isEqualTo(Result.ALLOW);
        assertThat(viewTeam2ClusterProfile).isEqualTo(Result.SKIP);

        assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.SKIP);
        assertThat(administerAllElasticAgentProfilesUnderTeam1).isEqualTo(Result.ALLOW);
        assertThat(administerAllElasticAgentProfilesUnderTeam2).isEqualTo(Result.SKIP);

        assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
        assertThat(administerTeam1ClusterProfile).isEqualTo(Result.ALLOW);
        assertThat(administerTeam2ClusterProfile).isEqualTo(Result.SKIP);
    }
}
