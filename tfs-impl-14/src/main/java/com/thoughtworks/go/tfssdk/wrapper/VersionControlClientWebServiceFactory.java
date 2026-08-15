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
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.framework.internal.SpecialURLs;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler;
import com.microsoft.tfs.util.GUID;
import ms.tfs.services.linking._03._IntegrationServiceSoap;
import ms.tfs.services.registration._03._RegistrationSoap;
import ms.tfs.services.registration._03._RegistrationSoap12Service;
import ms.tfs.versioncontrol.clientservices._03.*;
import ms.ws._LocationWebServiceSoap;
import ms.ws._LocationWebServiceSoap12Service;
import ms.ws._SecurityWebServiceSoap;
import ms.ws._SecurityWebServiceSoap12Service;
import ms.wss._ListsSoap;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;

/**
 * A minimal {@link WebServiceFactory}, modelled on {@code DefaultWebServiceFactory} but supporting only the
 * web services used for version control and by the connection framework of every connection. Everything else
 * fails with {@link UnsupportedOperationException}: their implementations are excluded from GoCD's trimmed
 * SDK jar (see trimTfsSdkJar in build.gradle). Unlike {@code DefaultWebServiceFactory}, whose initialization
 * eagerly resolves class literals for every service proxy the SDK supports, the supported services below are
 * registered from a map whose entries only reference proxies present in the trimmed jar.
 */
class VersionControlClientWebServiceFactory implements WebServiceFactory {

    private final Locale acceptLanguageLocale;
    private final TransportRequestHandler transportRequestHandler;

    private final Map<Class<?>, WebServiceMetadata> metadataByServiceInterface = Map.of(
        _RegistrationSoap.class, new WebServiceMetadata(ServiceInterfaceNames.REGISTRATION, ServiceInterfaceIdentifiers.REGISTRATION, SpecialURLs.DEFAULT_REGISTRATION_ENDPOINT, _RegistrationSoap12Service.getPortQName(), _RegistrationSoap12Service::new),
        // Null endpoint as for DefaultWebServiceFactory: the full location service URI is always passed to newLocationWebService()
        _LocationWebServiceSoap.class, new WebServiceMetadata(ServiceInterfaceNames.LOCATION, ServiceInterfaceIdentifiers.LOCATION, null, _LocationWebServiceSoap12Service.getPortQName(), _LocationWebServiceSoap12Service::new),
        _RepositorySoap.class, new WebServiceMetadata(ServiceInterfaceNames.VERSION_CONTROL, ServiceInterfaceIdentifiers.VERSION_CONTROL, _RepositorySoap12Service.getEndpointPath(), _RepositorySoap12Service.getPortQName(), _RepositorySoap12Service::new),
        _RepositoryExtensionsSoap.class, new WebServiceMetadata(ServiceInterfaceNames.VERSION_CONTROL_3, ServiceInterfaceIdentifiers.VERSION_CONTROL_3, _RepositoryExtensionsSoap12Service.getEndpointPath(), _RepositoryExtensionsSoap12Service.getPortQName(), _RepositoryExtensionsSoap12Service::new),
        _Repository4Soap.class, new WebServiceMetadata(ServiceInterfaceNames.VERSION_CONTROL_4, ServiceInterfaceIdentifiers.VERSION_CONTROL_4, _Repository4Soap12Service.getEndpointPath(), _Repository4Soap12Service.getPortQName(), _Repository4Soap12Service::new),
        _Repository5Soap.class, new WebServiceMetadata(ServiceInterfaceNames.VERSION_CONTROL_5, ServiceInterfaceIdentifiers.VERSION_CONTROL_5, _Repository5Soap12Service.getEndpointPath(), _Repository5Soap12Service.getPortQName(), _Repository5Soap12Service::new),
        _SecurityWebServiceSoap.class, new WebServiceMetadata(ServiceInterfaceNames.SECURITY, ServiceInterfaceIdentifiers.SECURITY, _SecurityWebServiceSoap12Service.getEndpointPath(), _SecurityWebServiceSoap12Service.getPortQName(), _SecurityWebServiceSoap12Service::new)
    );

    VersionControlClientWebServiceFactory(Locale acceptLanguageLocale, TransportRequestHandler transportRequestHandler) {
        this.acceptLanguageLocale = acceptLanguageLocale;
        this.transportRequestHandler = transportRequestHandler;
    }

    @Override
    public _RegistrationSoap newRegistrationWebService(URI connectionBaseURI, HttpClient httpClient) throws URISyntaxException {
        WebServiceMetadata metadata = supportedMetadataFor(_RegistrationSoap.class);
        // As for DefaultWebServiceFactory: cannot use the ServerDataProvider to find Registration (it might be
        // implemented using Registration), so find it directly using the default endpoint path
        URI webServiceURI = getSafeURI(resolveEndpointURI(connectionBaseURI, metadata.defaultEndpointPath()));
        return (_RegistrationSoap) newConfiguredService(metadata, httpClient, webServiceURI);
    }

    @Override
    public _LocationWebServiceSoap newLocationWebService(URI fullLocationServiceURI, HttpClient httpClient) throws URISyntaxException {
        WebServiceMetadata metadata = supportedMetadataFor(_LocationWebServiceSoap.class);
        return (_LocationWebServiceSoap) newConfiguredService(metadata, httpClient, getSafeURI(fullLocationServiceURI));
    }

    @Override
    public Object newWebService(TFSConnection connection, Class<?> webServiceInterfaceType, URI connectionBaseURI, HttpClient httpClient, ServerDataProvider serverDataProvider, RegistrationClient registrationClient) throws URISyntaxException {
        WebServiceMetadata metadata = supportedMetadataFor(webServiceInterfaceType);

        String endpointPath = serverDataProvider.locationForCurrentConnection(metadata.serviceInterfaceName(), metadata.serviceInterfaceIdentifier());

        // As for DefaultWebServiceFactory: the server's location/registration services are authoritative, and
        // the web service may be unavailable for the server version of the current connection
        if (endpointPath == null) {
            return null;
        }

        // Unlike DefaultWebServiceFactory, no rewriting of "internal" endpoint host names leaked by pre-2010
        // servers is attempted: GoCD supports TFS 2018 and later
        return newConfiguredService(metadata, httpClient, getSafeURI(resolveEndpointURI(connectionBaseURI, endpointPath)));
    }

    @Override
    public _ListsSoap newWSSWebService(TFSConnection connection, ProjectInfo projectInfo, URI connectionBaseURI, HttpClient httpClient, RegistrationClient registrationClient) {
        throw new UnsupportedOperationException("The SharePoint web service is not supported by GoCD's trimmed TFS SDK");
    }

    @Override
    public _IntegrationServiceSoap newLinkingWebService(TFSConnection connection, String linkingEndpoint, URI connectionBaseURI, HttpClient httpClient, RegistrationClient registrationClient) {
        throw new UnsupportedOperationException("The linking web service is not supported by GoCD's trimmed TFS SDK");
    }

    @Override
    public URI getWebServiceURI(Object webService) {
        return ((SOAPService) webService).getEndpoint();
    }

    private WebServiceMetadata supportedMetadataFor(Class<?> webServiceInterfaceType) {
        WebServiceMetadata metadata = metadataByServiceInterface.get(webServiceInterfaceType);
        if (metadata == null) {
            throw new UnsupportedOperationException("Web service %s is not supported by GoCD's trimmed TFS SDK".formatted(webServiceInterfaceType.getName()));
        }
        return metadata;
    }

    private Object newConfiguredService(WebServiceMetadata metadata, HttpClient httpClient, URI webServiceURI) {
        Object service = metadata.instantiator().newWebServiceImplementation(httpClient, webServiceURI, metadata.portQName());
        ((SOAPService) service).setAcceptLanguage(acceptLanguageLocale);
        ((SOAPService) service).addTransportRequestHandler(transportRequestHandler);
        return service;
    }

    /** As for DefaultWebServiceFactory: endpoint paths are absolute but must resolve relative to the server URI */
    private static URI resolveEndpointURI(URI connectionBaseURI, String endpointPath) {
        return connectionBaseURI.resolve(endpointPath.startsWith("/") ? endpointPath.substring(1) : endpointPath);
    }

    /** As for DefaultWebServiceFactory: converts to US-ASCII to avoid I18n issues in the SDK's HTTP client */
    private static URI getSafeURI(URI input) throws URISyntaxException {
        return new URI(input.toASCIIString());
    }

    private interface WebServiceInstantiator {
        Object newWebServiceImplementation(HttpClient httpClient, URI endpoint, QName port);
    }

    private record WebServiceMetadata(String serviceInterfaceName, GUID serviceInterfaceIdentifier, String defaultEndpointPath, QName portQName, WebServiceInstantiator instantiator) { }
}
