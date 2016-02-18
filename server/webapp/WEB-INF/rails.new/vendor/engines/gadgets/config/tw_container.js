{"gadgets.container": ["default"],
"server.contextpath": "${tw:path_with_leading_and_trailing_slash('server.contextpath', '/go')}",
"gadgets.parent": null,
"gadgets.rewriteProxyBase": "${Cur['server.contextpath']}gadgets/proxy?container=default&url=",
"gadgets.rewriteConcatBase": "${Cur['server.contextpath']}gadgets/concat?container=default&",
"gadgets.lockedDomainRequired": false,
"gadgets.parentOrigins": ["*"],
"gadgets.iframeBaseUri": "http://localhost:9000/gadgets/ifr",
"gadgets.uri.iframe.basePath": "http://localhost:9000/gadgets/ifr",
"gadgets.jsUriTemplate": "http://%host%{Cur['server.contextpath']}gadgets/js/%js%",
"gadgets.oauthGadgetCallbackTemplate": "//%host%${Cur['server.contextpath']}gadgets/oauthcallback",
"gadgets.osDataUri": "http://%host%/rpc",
"gadgets.securityTokenType": "insecure",
"gadgets.features": {
  "core.io": {
    "proxyUrl": "//%host%${Cur['server.contextpath']}gadgets/proxy?container=default&refresh=%refresh%&url=%url%%rewriteMime%",
    "jsonProxyUrl": "//%host%${Cur['server.contextpath']}gadgets/makeRequest"
  },
  "rpc": {
    "parentRelayUrl": "container/rpc_relay.html",
    "useLegacyProtocol": false
  },
  "skins": {
    "properties": {
      "BG_COLOR": "",
      "BG_IMAGE": "",
      "BG_POSITION": "",
      "BG_REPEAT": "",
      "FONT_COLOR": "",
      "ANCHOR_COLOR": ""
    }
  },

  "osml": {
    "library": ""
  }
}}
