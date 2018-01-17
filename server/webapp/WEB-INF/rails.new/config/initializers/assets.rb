##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################
Rails.application.config.assets.precompile += %w(lib/d3-3.1.5.min.js css/*.css)
Rails.application.config.assets.precompile += %w(frameworks.css single_page_apps/pipeline_configs.css single_page_apps/agents.css single_page_apps/elastic_profiles.css single_page_apps/preferences.css single_page_apps/analytics.css single_page_apps/auth_configs.css single_page_apps/roles.css single_page_apps/plugins.css single_page_apps/new_dashboard.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
Rails.application.config.assets.precompile += %w( new-theme.css )

Rails.application.config.assets.paths << Rails.root.join("webpack", "rails-shared")
