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
package com.thoughtworks.go.domain.materials.perforce;

import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.helper.P4TestRepo;
import com.thoughtworks.go.util.command.CommandLineException;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class P4Fixture {
    private P4TestRepo repo;

    public void setRepo(P4TestRepo repo) {
        this.repo = repo;
    }

    public void stop(P4Client p4) {
        stopP4d(p4);
        repo.tearDown();
    }

    private void stopP4d(P4Client p4) {
        try {
            p4.admin("stop");

            await("waiting for p4d to stop")
                .pollDelay(10, TimeUnit.MILLISECONDS)
                .timeout(10, TimeUnit.SECONDS)
                .until(() -> p4.checkConnection().failed());

        } catch (CommandLineException expected) {
            // Stopping p4d on windows returns the following failure:
            if (expected.getResult().errorAsString().contains("WSAECONNRESET") || expected.getResult().errorAsString().contains("WSAECONNREFUSED")) {
                return;
            }
            if (expected.getResult().errorAsString().contains("Connection refused") || expected.getResult().errorAsString().contains("Connection reset")) {
                return;
            }
            throw expected;
        }
    }

    public P4Client createClient() {
        return repo.createClient();
    }

    public P4Client createClient(String name, String view) {
        return repo.createClientWith(name, view);
    }

    public P4Material material(String view) {
        return repo.material(view);
    }

    public P4MaterialConfig materialConfig(String view) {
        return repo.materialConfig(view);
    }

    public P4Material material(String view, String dest) {
        P4Material p4Material = material(view);
        p4Material.setFolder(dest);

        return p4Material;
    }
}
