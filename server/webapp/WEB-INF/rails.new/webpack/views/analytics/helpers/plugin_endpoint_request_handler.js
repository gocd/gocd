/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const $      = require('jquery');
const Routes = require('gen/js-routes');

const PluginEndpoint  = require('rails-shared/plugin-endpoint');
const GoCDLinkSupport = require('views/analytics/helpers/gocd_link_support');

const PluginEndpointRequestHandler = {
  defineFetchAnalyticsHandler: (models) => {
    PluginEndpoint.define({
      "go.cd.analytics.v1.fetch-analytics": (message, trans) => {
        const meta   = message.head,
              model  = models[meta.uid],

              params = $.extend({}, message.body),
              type   = params.type,
              metric = params.metric;

        delete params.type;
        delete params.metric;

        model.fetch(Routes.showAnalyticsPath(meta.pluginId, type, metric, params), (data, errors) => {
          trans.respond({data, errors});
        });
      }
    });
  },

  defineLinkHandler: () => {
    PluginEndpoint.define({
      "go.cd.analytics.v1.link-to": (message, trans) => {
        const params = $.extend({}, message.body);
        const linkTo = params.link_to;

        if (!GoCDLinkSupport[linkTo]) {
          const error = `Don't know how to handle link-to request for ${linkTo} with params ${JSON.stringify(params, null, 2)}!`;
          trans.respond({error});
        }

        try {
          GoCDLinkSupport[linkTo](params);
          const success = `Successfully linked to ${linkTo} for ${JSON.stringify(params, null, 2)}`;
          trans.respond({success});
        } catch (e) {
          const error = `Failed to link to '${linkTo}' for ${JSON.stringify(params, null, 2)}. Reason ${e}!`;
          trans.respond({error});
        }
      }
    });
  }
};

module.exports = PluginEndpointRequestHandler;
