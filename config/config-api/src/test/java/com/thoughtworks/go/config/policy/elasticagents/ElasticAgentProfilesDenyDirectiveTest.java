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
import com.thoughtworks.go.config.policy.Deny;
import com.thoughtworks.go.config.policy.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ElasticAgentProfilesDenyDirectiveTest {

    @Nested
    class shouldDefinePermissions {
        @Test
        void forViewOfAllElasticAgentProfiles() {
            Deny directive = new Deny("view", "elastic_agent_profile", "*");

            Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
            Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
            Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
            Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);

            assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.DENY);
            assertThat(viewAllClusterProfiles).isEqualTo(Result.DENY);
            assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.SKIP);
            assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forAdministerOfAllElasticAgentProfiles() {
            Deny directive = new Deny("administer", "elastic_agent_profile", "*");

            Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
            Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
            Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
            Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);

            assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.DENY);
            assertThat(viewAllClusterProfiles).isEqualTo(Result.DENY);
            assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.DENY);
            assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forViewOfAllElasticAgentProfiles_usingWildcardAllowAllElasticAgentProfilesPattern() {
            Deny directive = new Deny("view", "elastic_agent_profile", "*:*");

            Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
            Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
            Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
            Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);

            assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.DENY);
            assertThat(viewAllClusterProfiles).isEqualTo(Result.DENY);
            assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.SKIP);
            assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forAdministerOfAllElasticAgentProfiles_usingWildcardAllowAllElasticAgentProfilesPattern() {
            Deny directive = new Deny("administer", "elastic_agent_profile", "*:*");

            Result viewAllElasticAgentProfiles = directive.apply("view", ElasticProfile.class, "*", null);
            Result viewAllClusterProfiles = directive.apply("view", ClusterProfile.class, "*", null);
            Result administerAllElasticAgentProfiles = directive.apply("administer", ElasticProfile.class, "*", null);
            Result administerAllClusterProfiles = directive.apply("administer", ClusterProfile.class, "*", null);

            assertThat(viewAllElasticAgentProfiles).isEqualTo(Result.DENY);
            assertThat(viewAllClusterProfiles).isEqualTo(Result.DENY);
            assertThat(administerAllElasticAgentProfiles).isEqualTo(Result.DENY);
            assertThat(administerAllClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forViewOfAllElasticAgentProfilesWithinCluster() {
            Deny directive = new Deny("view", "elastic_agent_profile", "team1_*:*");

            Result viewElasticAgentProfilesUnderTeam1UAT = directive.apply("view", ElasticProfile.class, "*", "team1_uat");
            Result viewElasticAgentProfilesUnderTeam2UAT = directive.apply("view", ElasticProfile.class, "*", "team2_uat");
            Result viewTeam1UATClusterProfile = directive.apply("view", ClusterProfile.class, "team1_uat", null);
            Result viewTeam2UATClusterProfiles = directive.apply("view", ClusterProfile.class, "team2_uat", null);

            Result administerElasticAgentProfilesUnderTeam1UAT = directive.apply("administer", ElasticProfile.class, "*", "team1_uat");
            Result administerElasticAgentProfilesUnderTeam2UAT = directive.apply("administer", ElasticProfile.class, "*", "team2_uat");
            Result administerTeam1UATClusterProfile = directive.apply("administer", ClusterProfile.class, "team1_uat", null);
            Result administerTeam2UATClusterProfiles = directive.apply("administer", ClusterProfile.class, "team2_uat", null);

            assertThat(viewElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.DENY);
            assertThat(viewElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(viewTeam1UATClusterProfile).isEqualTo(Result.DENY);
            assertThat(viewTeam2UATClusterProfiles).isEqualTo(Result.SKIP);

            assertThat(administerElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.SKIP);
            assertThat(administerElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(administerTeam1UATClusterProfile).isEqualTo(Result.SKIP);
            assertThat(administerTeam2UATClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forAdministerOfAllElasticAgentProfilesWithinCluster() {
            Deny directive = new Deny("administer", "elastic_agent_profile", "team1_*:*");

            Result viewElasticAgentProfilesUnderTeam1UAT = directive.apply("view", ElasticProfile.class, "*", "team1_uat");
            Result viewElasticAgentProfilesUnderTeam2UAT = directive.apply("view", ElasticProfile.class, "*", "team2_uat");
            Result viewTeam1UATClusterProfile = directive.apply("view", ClusterProfile.class, "team1_uat", null);
            Result viewTeam2UATClusterProfiles = directive.apply("view", ClusterProfile.class, "team2_uat", null);

            Result administerElasticAgentProfilesUnderTeam1UAT = directive.apply("administer", ElasticProfile.class, "*", "team1_uat");
            Result administerElasticAgentProfilesUnderTeam2UAT = directive.apply("administer", ElasticProfile.class, "*", "team2_uat");
            Result administerTeam1UATClusterProfile = directive.apply("administer", ClusterProfile.class, "team1_uat", null);
            Result administerTeam2UATClusterProfiles = directive.apply("administer", ClusterProfile.class, "team2_uat", null);

            assertThat(viewElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.DENY);
            assertThat(viewElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(viewTeam1UATClusterProfile).isEqualTo(Result.DENY);
            assertThat(viewTeam2UATClusterProfiles).isEqualTo(Result.SKIP);

            assertThat(administerElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.DENY);
            assertThat(administerElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(administerTeam1UATClusterProfile).isEqualTo(Result.SKIP);
            assertThat(administerTeam2UATClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forViewOfSpecificElasticAgentProfilesWithinCluster() {
            Deny directive = new Deny("view", "elastic_agent_profile", "team1_*:agent1_*");

            Result viewAgent1ElasticAgentProfilesUnderTeam1UAT = directive.apply("view", ElasticProfile.class, "agent1_high_mem", "team1_uat");
            Result viewAgent1ElasticAgentProfilesUnderTeam2UAT = directive.apply("view", ElasticProfile.class, "agent1_high_mem", "team2_uat");
            Result viewAgent2ElasticAgentProfilesUnderTeam1UAT = directive.apply("view", ElasticProfile.class, "agent2_high_mem", "team1_uat");
            Result viewAgent2ElasticAgentProfilesUnderTeam2UAT = directive.apply("view", ElasticProfile.class, "agent2_high_mem", "team2_uat");
            Result viewTeam1UATClusterProfile = directive.apply("view", ClusterProfile.class, "team1_uat", null);
            Result viewTeam2UATClusterProfiles = directive.apply("view", ClusterProfile.class, "team2_uat", null);

            Result administerAgent1ElasticAgentProfilesUnderTeam1UAT = directive.apply("administer", ElasticProfile.class, "agent1_high_mem", "team1_uat");
            Result administerAgent1ElasticAgentProfilesUnderTeam2UAT = directive.apply("administer", ElasticProfile.class, "agent1_high_mem", "team2_uat");
            Result administerAgent2ElasticAgentProfilesUnderTeam1UAT = directive.apply("administer", ElasticProfile.class, "agent2_high_mem", "team1_uat");
            Result administerAgent2ElasticAgentProfilesUnderTeam2UAT = directive.apply("administer", ElasticProfile.class, "agent2_high_mem", "team2_uat");
            Result administerTeam1UATClusterProfile = directive.apply("administer", ClusterProfile.class, "team1_uat", null);
            Result administerTeam2UATClusterProfiles = directive.apply("administer", ClusterProfile.class, "team2_uat", null);

            assertThat(viewAgent1ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.DENY);
            assertThat(viewAgent1ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(viewAgent2ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.SKIP);
            assertThat(viewAgent2ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(viewTeam1UATClusterProfile).isEqualTo(Result.DENY);
            assertThat(viewTeam2UATClusterProfiles).isEqualTo(Result.SKIP);

            assertThat(administerAgent1ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.SKIP);
            assertThat(administerAgent1ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(administerAgent2ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.SKIP);
            assertThat(administerAgent2ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(administerTeam1UATClusterProfile).isEqualTo(Result.SKIP);
            assertThat(administerTeam2UATClusterProfiles).isEqualTo(Result.SKIP);
        }

        @Test
        void forAdministerOfSpecificElasticAgentProfilesWithinCluster() {
            Deny directive = new Deny("administer", "elastic_agent_profile", "team1_*:agent1_*");

            Result viewAgent1ElasticAgentProfilesUnderTeam1UAT = directive.apply("view", ElasticProfile.class, "agent1_high_mem", "team1_uat");
            Result viewAgent1ElasticAgentProfilesUnderTeam2UAT = directive.apply("view", ElasticProfile.class, "agent1_high_mem", "team2_uat");
            Result viewAgent2ElasticAgentProfilesUnderTeam1UAT = directive.apply("view", ElasticProfile.class, "agent2_high_mem", "team1_uat");
            Result viewAgent2ElasticAgentProfilesUnderTeam2UAT = directive.apply("view", ElasticProfile.class, "agent2_high_mem", "team2_uat");
            Result viewTeam1UATClusterProfile = directive.apply("view", ClusterProfile.class, "team1_uat", null);
            Result viewTeam2UATClusterProfiles = directive.apply("view", ClusterProfile.class, "team2_uat", null);

            Result administerAgent1ElasticAgentProfilesUnderTeam1UAT = directive.apply("administer", ElasticProfile.class, "agent1_high_mem", "team1_uat");
            Result administerAgent1ElasticAgentProfilesUnderTeam2UAT = directive.apply("administer", ElasticProfile.class, "agent1_high_mem", "team2_uat");
            Result administerAgent2ElasticAgentProfilesUnderTeam1UAT = directive.apply("administer", ElasticProfile.class, "agent2_high_mem", "team1_uat");
            Result administerAgent2ElasticAgentProfilesUnderTeam2UAT = directive.apply("administer", ElasticProfile.class, "agent2_high_mem", "team2_uat");
            Result administerTeam1UATClusterProfile = directive.apply("administer", ClusterProfile.class, "team1_uat", null);
            Result administerTeam2UATClusterProfiles = directive.apply("administer", ClusterProfile.class, "team2_uat", null);

            assertThat(viewAgent1ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.DENY);
            assertThat(viewAgent1ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(viewAgent2ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.SKIP);
            assertThat(viewAgent2ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(viewTeam1UATClusterProfile).isEqualTo(Result.DENY);
            assertThat(viewTeam2UATClusterProfiles).isEqualTo(Result.SKIP);

            assertThat(administerAgent1ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.DENY);
            assertThat(administerAgent1ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(administerAgent2ElasticAgentProfilesUnderTeam1UAT).isEqualTo(Result.SKIP);
            assertThat(administerAgent2ElasticAgentProfilesUnderTeam2UAT).isEqualTo(Result.SKIP);
            assertThat(administerTeam1UATClusterProfile).isEqualTo(Result.SKIP);
            assertThat(administerTeam2UATClusterProfiles).isEqualTo(Result.SKIP);
        }
    }
}
