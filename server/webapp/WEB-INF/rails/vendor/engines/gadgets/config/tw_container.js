/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

{"gadgets.container" : ["default"],

"server.contextpath" : "${tw:path_with_leading_and_trailing_slash('server.contextpath', '/go')}",

// Set of regular expressions to validate the parent parameter. This is
// necessary to support situations where you want a single container to support
// multiple possible host names (such as for localized domains, such as
// <language>.example.org. If left as null, the parent parameter will be
// ignored; otherwise, any requests that do not include a parent
// value matching this set will return a 404 error.
"gadgets.parent" : null,

"gadgets.rewriteProxyBase" : "${Cur['server.contextpath']}gadgets/proxy?container=default&url=",
"gadgets.rewriteConcatBase" : "${Cur['server.contextpath']}gadgets/concat?container=default&",

// Should all gadgets be forced on to a locked domain?
"gadgets.lockedDomainRequired" : false,

// DNS domain on which gadgets should render.
// "gadgets.lockedDomainSuffix" : "-a.example.com:8080",
	
// Origins for CORS requests and/or Referer validation
// Indicate a set of origins or an entry with * to indicate that all origins are allowed
"gadgets.parentOrigins" : ["*"],

// Various urls generated throughout the code base.
// iframeBaseUri will automatically have the host inserted
// if locked domain is enabled and the implementation supports it.
// query parameters will be added.
"gadgets.iframeBaseUri" : "http://localhost:9000/gadgets/ifr",
"gadgets.uri.iframe.basePath" : "http://localhost:9000/gadgets/ifr",

// jsUriTemplate will have %host% and %js% substituted.
// No locked domain special cases, but jsUriTemplate must
// never conflict with a lockedDomainSuffix.
"gadgets.jsUriTemplate" : "http://%host%{Cur['server.contextpath']}gadgets/js/%js%",

//New configuration for iframeUri generation:
// "gadgets.uri.iframe.lockedDomainSuffix" :  "-a.example.com:8080",
// "gadgets.uri.iframe.unlockedDomain" : "www.example.com:8080",
// "gadgets.uri.iframe.basePath" : "/gadgets/ifr",

	
// Callback URL.  Scheme relative URL for easy switch between https/http.
"gadgets.oauthGadgetCallbackTemplate" : "//%host%${Cur['server.contextpath']}gadgets/oauthcallback",

// Config param to load Opensocial data for social
// preloads in data pipelining.  %host% will be
// substituted with the current host.
"gadgets.osDataUri" : "http://%host%/rpc",

// Use an insecure security token by default
"gadgets.securityTokenType" : "insecure",
// Uncomment these to switch to a secure version
//
//"gadgets.securityTokenType" : "secure",
//"gadgets.securityTokenKeyFile" : "/path/to/key/file.txt",



// This config data will be passed down to javascript. Please
// configure your object using the feature name rather than
// the javascript name.

// Only configuration for required features will be used.
// See individual feature.xml files for configuration details.
"gadgets.features" : {
  "core.io" : {
    // Note: /proxy is an open proxy. Be careful how you expose this!
    // Note: Here // is replaced with the current protocol http/https
    "proxyUrl" : "//%host%${Cur['server.contextpath']}gadgets/proxy?container=default&refresh=%refresh%&url=%url%%rewriteMime%",
    "jsonProxyUrl" : "//%host%${Cur['server.contextpath']}gadgets/makeRequest"
  },
  
  "rpc" : {
    // Path to the relay file. Automatically appended to the parent
    // parameter if it passes input validation and is not null.
    // This should never be on the same host in a production environment!
    // Only use this for TESTING!
    "parentRelayUrl" : "container/rpc_relay.html",

    // If true, this will use the legacy ifpc wire format when making rpc
    // requests.
    "useLegacyProtocol" : false
  },
  // Skin defaults
  "skins" : {
    "properties" : {
      "BG_COLOR": "",
      "BG_IMAGE": "",
      "BG_POSITION": "",
      "BG_REPEAT": "",
      "FONT_COLOR": "",
      "ANCHOR_COLOR": ""
    }
  },
  
  "osml": {
    // OSML library resource.  Can be set to null or the empty string to disable OSML
    // for a container.
    "library": ""
  }
}}
