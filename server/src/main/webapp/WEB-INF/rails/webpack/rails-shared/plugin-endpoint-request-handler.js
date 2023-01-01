/*
 * Copyright 2023 Thoughtworks, Inc.
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
/* eslint-disable */
(function () {
  "use strict";

  if ("undefined" !== typeof module) {
    // Darn.. externally require while coming from spa
    var Routes          = require('gen/js-routes');
    var PluginEndpoint  = require('rails-shared/plugin-endpoint');
    var GoCDLinkSupport = require('rails-shared/gocd-link-support');
  } else {
    var PluginEndpoint  = window.PluginEndpoint;
    var GoCDLinkSupport = window.GoCDLinkSupport;
  }

  var serialize = function (obj) {
    var str = [];
    for (var p in obj)
      if (obj.hasOwnProperty(p)) {
        str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
      }
    return str.join("&");
  };

  var PluginEndpointRequestHandler = {
    defineFetchAnalyticsHandler: function defineFetchAnalyticsHandler(models) {
      PluginEndpoint.define({
        "go.cd.analytics.v1.fetch-analytics": function goCdAnalyticsV1FetchAnalytics(message, trans) {
          var meta   = message.head,
              model  = models[meta.uid] || models.get(meta.uid),
              params = message.body,
              type   = params.type,
              metric = params.metric;

          delete params.type;
          delete params.metric;

          model.fetch("/go/analytics/" + meta.pluginId + "/" + type + "/" + metric + "?" + serialize(params), function (data, errors) {
            trans.respond({data: data, errors: errors});
          });
        }
      });
    },

    defineLinkHandler: function defineLinkHandler() {
      PluginEndpoint.define({
        "go.cd.analytics.v1.link-external": function goCdAnalyticsV1LinkExternal(message, trans) {
          var params = _.clone(message.body),
              url    = params.url;
          window.open(url, "_blank").focus();
        },

        "go.cd.analytics.v1.link-to": function goCdAnalyticsV1LinkTo(message, trans) {
          var params = _.clone(message.body);
          var linkTo = params.link_to;

          if (!GoCDLinkSupport[linkTo]) {
            var error = 'Don\'t know how to handle link-to request for ' + linkTo + ' with params ' + JSON.stringify(params, null, 2) + '!';
            trans.respond({error: error});
          }

          try {
            GoCDLinkSupport[linkTo](params);
            var _success = 'Successfully linked to ' + linkTo + ' for ' + JSON.stringify(params, null, 2);
            trans.respond({success: _success});
          } catch (e) {
            var _error = 'Failed to link to \'' + linkTo + '\' for ' + JSON.stringify(params, null, 2) + '. Reason ' + e + '!';
            trans.respond({error: _error});
          }
        }
      });
    }
  };

  if ("undefined" !== typeof module) {
    module.exports = PluginEndpointRequestHandler;
  } else {
    window.PluginEndpointRequestHandler = PluginEndpointRequestHandler;
  }
})();
