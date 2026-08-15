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
package com.thoughtworks.go.tfssdk.wrapper;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.LocationService;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.sharepoint.WSSClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.config.client.ClientFactory;

import ms.tfs.versioncontrol.clientservices._03._Repository4Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap;

/**
 * A minimal {@link ClientFactory}, modelled on {@code DefaultClientFactory} but supporting only the version
 * control client and the connection framework clients required to establish any connection. Everything else
 * fails with {@link UnsupportedOperationException}: their implementations are excluded from GoCD's trimmed
 * SDK jar (see trimTfsSdkJar in build.gradle).
 */
@SuppressWarnings("rawtypes")
class VersionControlClientFactory implements ClientFactory {

    @Override
    public Object newClient(Class clientType, TFSConnection connection) {
        if (clientType == VersionControlClient.class) {
            TFSTeamProjectCollection collection = asProjectCollection(connection, clientType);
            return new VersionControlClient(
                collection,
                (_RepositorySoap) connection.getWebService(_RepositorySoap.class),
                (_RepositoryExtensionsSoap) connection.getWebService(_RepositoryExtensionsSoap.class),
                (_Repository4Soap) connection.getWebService(_Repository4Soap.class),
                (_Repository5Soap) connection.getWebService(_Repository5Soap.class));
        }
        if (clientType == ILocationService.class) {
            return new LocationService(connection);
        }
        if (clientType == RegistrationClient.class) {
            return new RegistrationClient(asProjectCollection(connection, clientType));
        }
        throw new UnsupportedOperationException(
            "Client %s is not supported by GoCD's trimmed TFS SDK".formatted(clientType.getName()));
    }

    @Override
    public WSSClient newWSSClient(TFSTeamProjectCollection connection, ProjectInfo projectInfo) {
        throw new UnsupportedOperationException(
            "SharePoint clients are not supported by GoCD's trimmed TFS SDK");
    }

    private static TFSTeamProjectCollection asProjectCollection(TFSConnection connection, Class clientType) {
        if (!(connection instanceof TFSTeamProjectCollection collection)) {
            throw new IllegalArgumentException(
                "Client class %s can only be created with a %s, %s is not supported".formatted(
                    clientType.getName(), TFSTeamProjectCollection.class.getName(), connection.getClass().getName()));
        }
        return collection;
    }
}
