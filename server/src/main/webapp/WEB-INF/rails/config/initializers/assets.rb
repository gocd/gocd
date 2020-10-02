#
# Copyright 2020 ThoughtWorks, Inc.
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
#

# Be sure to restart your server when you modify this file.

# Version of your assets, change this if you want to expire all your assets.
Rails.application.config.assets.version = '1.0'

# Add Yarn node_modules folder to the asset load path.
Rails.application.config.assets.paths << Rails.root.join('node_modules')

Rails.application.config.assets.precompile = %w(lib/d3-3.1.5.min.js)

# Used by vm templates `_header.vm`
Rails.application.config.assets.precompile += %w(application.css patterns/application.css css/application.css vm/application.css new-theme.css)
Rails.application.config.assets.precompile += %w(application.js)

Rails.application.config.assets.precompile += %w(frameworks.css single_page_apps/agents.css single_page_apps/elastic_profiles.css single_page_apps/artifact_stores.css single_page_apps/preferences.css single_page_apps/analytics.css single_page_apps/auth_configs.css single_page_apps/roles.css single_page_apps/plugins.css single_page_apps/new_dashboard.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
Rails.application.config.assets.precompile += %w( new-theme.css )

Rails.application.config.assets.paths << Rails.root.join("webpack", "rails-shared")
