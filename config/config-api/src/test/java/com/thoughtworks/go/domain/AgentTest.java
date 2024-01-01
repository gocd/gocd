/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Agent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.config.JobConfig.RESOURCES;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class AgentTest {
    @Test
    void shouldCopyAllFieldsFromAnotherAgentObjectUsingCopyConstructor() {
        Agent origAgent = new Agent("uuid", "host", "127.0.0.1", "cookie");
        origAgent.addResources(List.of("Resource1", "Resource2"));
        origAgent.addEnvironments(List.of("dev", "test"));

        Agent copiedAgent = new Agent(origAgent);
        assertEquals(origAgent, copiedAgent);
    }


    @Test
    void cookieAssignedShouldReturnTrueWhenAgentHasCookie() {
        Agent agent = new Agent("uuid", "host", "127.0.0.1", "cookie");
        assertThat(agent.cookieAssigned(), is(true));
    }

    @Test
    void cookieAssignedShouldReturnFalseWhenAgentDoesNotHaveCookie() {
        Agent agent = new Agent("uuid", "host", "127.0.0.1");
        assertThat(agent.cookieAssigned(), is(false));
    }

    @Nested
    class IPAddress {
        @Test
        void agentWithNoIpAddressShouldBeValid() {
            Agent agent = new Agent("uuid", null, null);
            agent.validate();
            assertFalse(agent.hasErrors());
        }

        @Test
        void shouldValidateIpCorrectIPv4Address() {
            shouldBeValid("127.0.0.1");
            shouldBeValid("0.0.0.0");
            shouldBeValid("255.255.0.0");
            shouldBeValid("0:0:0:0:0:0:0:1");
        }

        @Test
        void shouldValidateIpCorrectIPv6Address() {
            shouldBeValid("0:0:0:0:0:0:0:1");
        }

        @Test
        void shouldFailValidationIfIPAddressIsInvalid1() {
            Agent agent = new Agent("uuid", "host", "blahinvalid");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("'blahinvalid' is an invalid IP address."));

            agent = new Agent("uuid", "host", "blah.invalid");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("'blah.invalid' is an invalid IP address."));
        }

        @Test
        void shouldFailValidationIfIPAddressIsInvalid2() {
            Agent agent = new Agent("uuid", "host", "399.0.0.1");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("'399.0.0.1' is an invalid IP address."));
        }

        @Test
        void shouldInvalidateEmptyIpAddress() {
            Agent agent = new Agent("uuid", "host", "");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("IpAddress cannot be empty if it is present."));
        }

        private void shouldBeValid(String ipAddress) {
            Agent agent = new Agent("some-dummy-uuid");
            agent.setIpaddress(ipAddress);
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is(nullValue()));
        }
    }

    @Nested
    class Resources {
        @Nested
        class AddResources {
            @Test
            void shouldAddResourceWithValidResourceName() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getResources(), is(emptyString()));

                agent.addResource("r1");
                assertThat(agent.getResources(), is("r1"));

                agent.addResource("r2");
                assertThat(agent.getResources(), is("r1,r2"));
            }

            @Test
            void shouldAddResourceWithResourceNameContainingLeadingAndTrailingSpaces() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getResources(), is(emptyString()));

                agent.addResource("  r1");
                assertThat(agent.getResources(), is("r1"));

                agent.addResource("r2      ");
                assertThat(agent.getResources(), is("r1,r2"));

                agent.addResource(" r3 ");
                assertThat(agent.getResources(), is("r1,r2,r3"));
            }

            @Test
            void shouldDoNothingWhenEmptyResourceNameIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getResources(), is(emptyString()));

                agent.addResource("  ");
                assertThat(agent.getResources(), is(emptyString()));

                agent.addResource("");
                assertThat(agent.getResources(), is(emptyString()));
            }

            @Test
            void shouldDoNothingWhenNullCommaSeparatedStringOfResourceNamesIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setResources(null);
                assertThat(agent.getResources(), is(nullValue()));
            }

            @Test
            void shouldDoNothingWhenNullResourceNameIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getResources(), is(emptyString()));

                agent.addResource(null);
                assertThat(agent.getResources(), is(emptyString()));
            }

            @Test
            void shouldAddResourcesWithValidListOfResourceNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

                agent.addResources(List.of("r1", "r2"));
                assertThat(agent.getResources(), is("r1,r2"));

                agent.addResources(List.of("r3"));
                assertThat(agent.getResources(), is("r1,r2,r3"));
            }

            @Test
            void shouldDoNothingWhenNullIsSpecifiedAsListOfResourceNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.addResources(null);
                assertThat(agent.getResources(), is(emptyString()));
            }

            @Test
            void shouldAddResourcesWithListOfResourceNamesContainingLeadingTrailingSpaces() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

                agent.addResources(List.of("r1", "r2"));
                assertThat(agent.getResources(), is("r1,r2"));

                agent.addResources(List.of("  r3", "r4  ", "  r5  ", "r6", "   ", " "));
                assertThat(agent.getResources(), is("r1,r2,r3,r4,r5,r6"));
            }
        }

        @Nested
        class RemoveResources {
            @Test
            void shouldRemoveResourcesFromExistingResources() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setResources("r1,r2");
                agent.removeResources(List.of("r1", "r3"));
                assertThat(agent.getResources(), is("r2"));
            }

            @Test
            void shouldDoNothingWhenListOfResourcesToRemoveDoesNotExist() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.removeResources(List.of("r1", "r2"));
                assertNull(agent.getResources());
            }

            @Test
            void shouldDoNothingWhenListOfResourcesToRemoveIsEmpty() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.removeResources(emptyList());
                assertThat(agent.getResources(), is(emptyString()));
            }

            @Test
            void shouldDoNothingWhenListOfResourcesToRemoveIsNull() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.removeResources(null);
                assertThat(agent.getResources(), is(emptyString()));
            }

            @Test
            void shouldTrimEachResourceNameInTheListOfResourcesToRemove() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setResourcesFromList(List.of("r1", "r2", "r3"));
                agent.removeResources(List.of("  r1 ", "r2 ", " r3"));
                assertNull(agent.getResources());
            }

            @Test
            void shouldNotConsiderNullOrEmptyResourceNamesInTheListOfResourcesToRemove() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setResourcesFromList(List.of("r1", "r2", "r3"));
                agent.removeResources(Arrays.asList(null, " ", ""));
                assertThat(agent.getResources(), is("r1,r2,r3"));
            }
        }

        @Test
        void shouldAllowResourcesOnNonElasticAgents() {
            Agent agent = new Agent("uuid", "hostname", "10.10.10.10");

            agent.setResources("foo");
            agent.validate();
            assertThat(agent.errors().isEmpty(), is(true));
        }

        @Test
        void shouldNotAllowResourcesElasticAgents() {
            Agent agent = new Agent("uuid", "hostname", "10.10.10.10");
            agent.setElasticPluginId("com.example.foo");
            agent.setElasticAgentId("foobar");
            agent.setResources("foo");
            agent.validate();

            assertEquals(1, agent.errors().size());
            assertThat(agent.errors().on(RESOURCES), is("Elastic agents cannot have resources."));
        }
    }

    @Nested
    class UUID {
        @Test
        void shouldPassValidationWhenUUidIsAvailable() {
            Agent agent = new Agent("uuid");
            agent.validate();
            assertThat(agent.errors().on(Agent.UUID), is(nullValue()));
        }

        @Test
        void shouldFailValidationWhenUUidIsBlank() {
            Agent agent = new Agent("");
            agent.validate();
            assertThat(agent.errors().on(Agent.UUID), is("UUID cannot be empty"));
            agent = new Agent("");
            agent.validate();
            assertThat(agent.errors().on(Agent.UUID), is("UUID cannot be empty"));
        }
    }

    @Nested
    class Environments {
        @Nested
        class SetEnvironments {
            @Test
            void shouldSetEnvironmentsWithValidCommaSeparatedStringOfEnvironmentNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironments("env1,env2,env3");
                assertThat(agent.getEnvironments(), is("env1,env2,env3"));

                agent.setEnvironments("env4,env5");
                assertThat(agent.getEnvironments(), is("env4,env5"));
            }

            @Test
            void shouldSetEnvironmentsAsNullWhenEmptyCommaSeparatedStringOfEnvironmentNamesIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironments("      ");
                assertThat(agent.getEnvironments(), is(nullValue()));
            }

            @Test
            void shouldSetEnvironmentsAsNullWhenNullCommaSeparatedStringOfEnvironmentNamesIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironments(null);
                assertThat(agent.getEnvironments(), is(nullValue()));
            }

            @Test
            void shouldSetEnvironmentsAsNullWhenInvalidCommaSeparatedStringOfEnvironmentNamesIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironments("   , ,, ,");
                assertThat(agent.getEnvironments(), is(nullValue()));
            }

            @Test
            void shouldSetEnvironmentsWithValidListOfEnvironmentNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironmentsFrom(List.of("env1", "env2", "env3"));
                assertThat(agent.getEnvironments(), is("env1,env2,env3"));

                agent.setEnvironmentsFrom(List.of("env4", "env5"));
                assertThat(agent.getEnvironments(), is("env4,env5"));
            }

            @Test
            void shouldSetEnvironmentsFromEmptyListOrListContainingEmptyEnvironmentNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironmentsFrom(emptyList());
                assertThat(agent.getEnvironments(), is(nullValue()));

                agent.setEnvironmentsFrom(List.of("      ", " ", "  ", "", "", "  "));
                assertThat(agent.getEnvironments(), is(nullValue()));

                agent.setEnvironmentsFrom(List.of("      , ,  ,,,  "));
                assertThat(agent.getEnvironments(), is(", ,  ,,,"));
            }

            @Test
            void shouldSetEnvironmentsAsNullWhenNullListOfEnvironmentNamesIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironmentsFrom(null);
                assertThat(agent.getEnvironments(), is(nullValue()));
            }
        }

        @Nested
        class AddEnvironments {
            @Test
            void shouldAddEnvironmentWithValidEnvironmentName() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getEnvironments(), is(emptyString()));

                agent.addEnvironment("env1");
                assertThat(agent.getEnvironments(), is("env1"));

                agent.addEnvironment("env2");
                assertThat(agent.getEnvironments(), is("env1,env2"));
            }

            @Test
            void shouldTrimAndAddEnvironmentWhenEnvironmentNameIsSpecifiedAsContainingLeadingAndTrailingSpaces() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getEnvironments(), is(emptyString()));

                agent.addEnvironment("  env1");
                assertThat(agent.getEnvironments(), is("env1"));

                agent.addEnvironment("env2      ");
                assertThat(agent.getEnvironments(), is("env1,env2"));

                agent.addEnvironment(" env3 ");
                assertThat(agent.getEnvironments(), is("env1,env2,env3"));
            }

            @Test
            void shouldDoNothingWhenEmptyEnvironmentNameIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getEnvironments(), is(emptyString()));

                agent.addEnvironment("  ");
                assertThat(agent.getEnvironments(), is(emptyString()));

                agent.addEnvironment("");
                assertThat(agent.getEnvironments(), is(emptyString()));
            }

            @Test
            void shouldDoNothingWhenNullEnvironmentNameIsSpecified() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                assertThat(agent.getEnvironments(), is(emptyString()));

                agent.addEnvironment(null);
                assertThat(agent.getEnvironments(), is(emptyString()));
            }

            @Test
            void shouldAddEnvironmentsWithValidListOfEnvironmentNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

                agent.addEnvironments(List.of("env1", "env2"));
                assertThat(agent.getEnvironments(), is("env1,env2"));

                agent.addEnvironments(List.of("env3"));
                assertThat(agent.getEnvironments(), is("env1,env2,env3"));
            }

            @Test
            void shouldDoNothingWhenNullIsSpecifiedAsListOfEnvironmentNames() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.addEnvironments(null);
                assertThat(agent.getEnvironments(), is(emptyString()));
            }

            @Test
            void shouldAddEnvironmentsWithListOfEnvironmentNamesContainingLeadingTrailingSpaces() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

                agent.addEnvironments(List.of("env1", "env2"));
                assertThat(agent.getEnvironments(), is("env1,env2"));

                agent.addEnvironments(List.of("  env3", "env4  ", "  env5  ", "env6", "   ", " "));
                assertThat(agent.getEnvironments(), is("env1,env2,env3,env4,env5,env6"));
            }
        }

        @Nested
        class RemoveEnvironments {
            @Test
            void shouldRemoveEnvironmentsFromExistingEnvironments() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironments("env1,env2");
                agent.removeEnvironments(List.of("env1", "env3"));
                assertThat(agent.getEnvironments(), is("env2"));
            }

            @Test
            void shouldDoNothingWhenListOfEnvironmentsToRemoveDoesNotExist() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.removeEnvironments(List.of("env1", "env3"));
                assertNull(agent.getEnvironments());
            }

            @Test
            void shouldDoNothingWhenListOfEnvironmentsToRemoveIsEmpty() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.removeEnvironments(emptyList());
                assertNull(agent.getEnvironments());
            }

            @Test
            void shouldDoNothingWhenListOfEnvironmentsToRemoveIsNull() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.removeEnvironments(null);
                assertNull(agent.getEnvironments());
            }

            @Test
            void shouldTrimEachEnvironmentNameInTheListOfEnvironmentsToRemove() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironmentsFrom(List.of("e1", "e2", "e3"));
                agent.removeEnvironments(List.of("  e1 ", "e2 ", " e3"));
                assertNull(agent.getEnvironments());
            }

            @Test
            void shouldNotConsiderNullOrEmptyEnvironmentNamesInTheListOfEnvironmentsToRemove() {
                Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
                agent.setEnvironmentsFrom(List.of("e1", "e2", "e3"));
                agent.removeEnvironments(Arrays.asList(null, " ", ""));
                assertThat(agent.getEnvironments(), is("e1,e2,e3"));
            }
        }

        @Test
        void shouldReturnEmptyListIfEnvironmentIsNullOrEmpty() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

            assertThat(agent.getEnvironments(), is(emptyString()));
            assertThat(agent.getEnvironmentsAsList(), is(empty()));

            agent.setEnvironments("");

            assertThat(agent.getEnvironments(), is(nullValue()));
            assertThat(agent.getEnvironmentsAsList(), is(empty()));
        }

        @Test
        void shouldReturnEnvAsListSplitOnComma() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
            assertThat(agent.getEnvironments(), is(emptyString()));
            assertThat(agent.getEnvironmentsAsList(), is(empty()));

            agent.setEnvironments("env1,env2");

            assertThat(agent.getEnvironments(), not(nullValue()));
            assertThat(agent.getEnvironments(), is("env1,env2"));
            assertThat(agent.getEnvironmentsAsList(), is(List.of("env1", "env2")));
        }
    }

    @Nested
    class hasAllResources {
        @Test
        void shouldMakeACaseInsensitiveComparisonOfResources() {
            Agent agent = new Agent("uuid", "host", "ip", List.of("Postgres DB", "Linux-1", "W1nd0ws", "Mac OS"));

            assertTrue(agent.hasAllResources(List.of("posTgres db", "linux-1", "w1nd0ws")));
        }
    }
}
