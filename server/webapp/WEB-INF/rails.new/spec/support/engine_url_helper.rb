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
module EngineUrlHelper
  def stub_routes_for_main_app main_app
    allow(main_app).to receive(:root_path).and_return("/")
    allow(main_app).to receive(:user_listing_path).and_return("/path/for/user/listing")
    allow(main_app).to receive(:backup_server_path).and_return("admin/backup")
    allow(main_app).to receive(:pipelines_snippet_path).and_return("admin/pipelines/snippet")
    allow(main_app).to receive(:pipeline_dashboard_path).and_return("/path/to/pipeline/dashboard")
    allow(main_app).to receive(:environments_path).and_return("/path/to/environments")
    allow(main_app).to receive(:agents_path).and_return("/path/to/agents")
    allow(main_app).to receive(:pipeline_groups_path).and_return("/path/to/pipeline/groups")
    allow(main_app).to receive(:templates_path).and_return("/path/to/templates")
    allow(main_app).to receive(:config_view_path).and_return("/path/to/config/view")
    allow(main_app).to receive(:edit_server_config_path).and_return("/path/to/server/edit")
    allow(main_app).to receive(:plugins_listing_path).and_return("/path/to/plugins/listing")
    allow(main_app).to receive(:package_repositories_new_path).and_return("/path/to/package/repo")
    allow(main_app).to receive(:package_repositories_list_path).and_return("/path/to/package/listing")
    allow(main_app).to receive(:global_message_path).and_return("/path/to/global/message")
  end

  def stub_oauth2_provider_engine oauth_engine
    allow(oauth_engine).to receive(:clients_path).and_return("/path/for/oauth/clients")
  end
end