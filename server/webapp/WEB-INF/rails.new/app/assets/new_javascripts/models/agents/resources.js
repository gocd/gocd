/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'helpers/mrequest', 'models/agents/tri_state_checkbox'], function (m, _, mrequest, TriStateCheckbox) {
  var Resources         = {};
  Resources.list        = m.prop([]);

  Resources.init = function (selectedAgents) {
    m.request({
      method:        'GET',
      url:           Routes.apiv1AdminInternalResourcesPath(),
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: function (responseBody) {
        var list = _.map(responseBody, function (value) {
          return new TriStateCheckbox(value, _.map(selectedAgents, function (agent) {
            return agent.resources();
          }));
        });

        Resources.list(list);
      }
    })
  };

  return Resources;
});
