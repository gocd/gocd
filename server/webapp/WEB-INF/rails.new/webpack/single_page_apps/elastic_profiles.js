/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const $                     = require('jquery');
const m                     = require('mithril');
const ElasticProfilesWidget = require('views/elastic_profiles/elastic_profiles_widget');
const VersionUpdater        = require('models/shared/version_updater');

require('foundation-sites');
require('helpers/server_health_messages_helper');

$(() => {
  $(document).foundation();
  new VersionUpdater().update();
  m.mount($("#elastic-profiles").get(0), ElasticProfilesWidget);
});
