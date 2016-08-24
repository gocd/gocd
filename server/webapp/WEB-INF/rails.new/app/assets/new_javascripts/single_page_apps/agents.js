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

requirejs([
  'jquery', 'mithril',
  'js-routes',
  'models/agents/resources',
  'models/agents/environments',
  'views/agents/agents_widget',
  'foundation.util.mediaQuery', 'foundation.dropdownMenu', 'foundation.responsiveToggle', 'foundation.dropdown'
], function ($, m, JsRoutes,
             Resources, Environments, AgentsWidget) {

  $(function () {

    Resources.init();
    Environments.init();
    $(document).foundation();

    var mount = function(){
      m.mount(document.getElementById('agents'), AgentsWidget);
    };

    mount();
  });
});
