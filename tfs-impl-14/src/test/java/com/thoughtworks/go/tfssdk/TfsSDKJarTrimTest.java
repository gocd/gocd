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
package com.thoughtworks.go.tfssdk;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.thoughtworks.go.tfssdk.wrapper.VersionControlConnectionAdvisor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TfsSDKJarTrimTest {

    /**
     * Exercises the class-literal resolution that the SDK performs when the first client is created:
     * TFSConnection.getClient() unconditionally references WSSClient.class, and the DefaultClientFactory /
     * DefaultWebServiceFactory initialization eagerly resolves class literals for every feature client and
     * *Soap proxy interface. This is the canary for classes that MUST NOT be excluded by trimTfsSdkJar even
     * though GoCD never uses them: they are resolved on this always-executed path, not lazily. Connection
     * failures are expected (there is no TFS server); missing classes are the failure being tested for.
     */
    @Test
    public void versionControlClientCreationShouldNotRequireAnyTrimmedClasses() {
        TFSTeamProjectCollection collection = new TFSTeamProjectCollection(
            URI.create("http://tfs.invalid:8080/"), new UsernamePasswordCredentials("user", "password"), new VersionControlConnectionAdvisor());
        try {
            collection.getVersionControlClient();
        } catch (UnsatisfiedLinkError acceptable) {
            // expected: the SDK's native libraries are not exploded/configured in unit tests (that happens at
            // runtime via TfsSDKCommandBuilder), and reaching this point means class resolution succeeded
        } catch (LinkageError e) {
            fail("Trimmed SDK jar is missing classes required on the client creation path; adjust trimTfsSdkJar in build.gradle", e);
        } catch (Exception acceptable) {
            // expected: no TFS server is reachable from tests
        } finally {
            collection.close();
        }
    }

    /**
     * Recreates the production failure mode that a fresh test environment cannot see: a server that has
     * previously run TFS materials has a populated workspace cache, and scanning it during
     * VersionControlClient creation is what first links Workspace (the dedupe scan in InternalCache.load
     * compares each loaded workspace against those already loaded, so at least two entries are needed).
     * Overriding the persistence store points the SDK at a seeded cache without touching the real user home.
     */
    @Test
    public void versionControlClientCreationShouldSurviveAPopulatedWorkspaceCache(@TempDir Path cacheDir) throws Exception {
        Files.writeString(cacheDir.resolve("VersionControl.config"), """
            <?xml version="1.0" encoding="UTF-8"?>
            <VersionControlServer>
              <Servers>
                <ServerInfo uri="http://tfs.invalid:8080/tfs/collection" repositoryGuid="ba99f1f5-d1fa-4029-8621-28b72ed1b4fb">
                  <WorkspaceInfo name="workspace-one" ownerName="user" computer="go-server" comment="">
                    <MappedPaths><MappedPath path="/tmp/one"/></MappedPaths>
                  </WorkspaceInfo>
                  <WorkspaceInfo name="workspace-two" ownerName="user" computer="go-server" comment="">
                    <MappedPaths><MappedPath path="/tmp/two"/></MappedPaths>
                  </WorkspaceInfo>
                </ServerInfo>
              </Servers>
            </VersionControlServer>
            """);


        TFSTeamProjectCollection collection = new TFSTeamProjectCollection(
            URI.create("http://tfs.invalid:8080/"), new UsernamePasswordCredentials("user", "password"),
            connectionAdvisorFromLocalStore(cacheDir));
        try {
            collection.getVersionControlClient();
        } catch (UnsatisfiedLinkError acceptable) {
            // the SDK has no natives for some dev platforms (e.g. macOS arm64); on platforms with natives
            // (all CI platforms) this path runs through the seeded cache scan
        } catch (LinkageError e) {
            fail("Trimmed SDK jar is missing classes required when the workspace cache is populated; adjust trimTfsSdkJar in build.gradle", e);
        } catch (Exception acceptable) {
            // expected: no TFS server is reachable from tests
        } finally {
            collection.close();
        }
    }

    private static @NonNull VersionControlConnectionAdvisor connectionAdvisorFromLocalStore(Path cacheDir) {
        return new VersionControlConnectionAdvisor() {
            final FilesystemPersistenceStore store = new FilesystemPersistenceStore(cacheDir.toFile());

            @Override
            public PersistenceStoreProvider getPersistenceStoreProvider(ConnectionInstanceData instanceData) {
                return new PersistenceStoreProvider() {
                    @Override
                    public FilesystemPersistenceStore getCachePersistenceStore() {
                        return store;
                    }

                    @Override
                    public FilesystemPersistenceStore getConfigurationPersistenceStore() {
                        return store;
                    }

                    @Override
                    public FilesystemPersistenceStore getLogPersistenceStore() {
                        return store;
                    }
                };
            }
        };
    }

    /**
     * NestedJarClassLoader relies on the SDK jar NOT bundling its own commons-logging: since the classes are
     * absent from the jar, the SDK's logging via the commons-logging API resolves against the parent
     * classloader's jcl-over-slf4j and flows into GoCD's slf4j/logback setup. The SDK's bundled log4j2 and
     * TEE logging adapter are unreachable as a result and are also removed (see trimTfsSdkJar).
     */
    @Test
    public void trimmedSdkJarShouldNotBundleAnyLoggingImplementation() throws Exception {
        File sdkJar = new File(TFSTeamProjectCollection.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        try (JarFile jar = new JarFile(sdkJar)) {
            List<String> loggingEntries = jar.stream()
                .map(JarEntry::getName)
                .filter(name -> name.startsWith("org/apache/commons/logging/")
                    || name.startsWith("org/apache/logging/log4j/")
                    || name.startsWith("com/microsoft/tfs/logging/"))
                .toList();

            assertThat(loggingEntries)
                .describedAs("SDK jar %s should not bundle any logging classes; see trimTfsSdkJar in build.gradle", sdkJar)
                .isEmpty();
        }
    }
}
