##########################################################################
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
##########################################################################

module ApiV1
  module Shared
    module ConfigOrigin
      class ConfigRepoSummaryRepresenter < BaseRepresenter
        alias_method :config_repo, :represented

        link :self do |opts|
          opts[:url_builder].apiv1_admin_config_repo_url(id: config_repo.getId)
        end

        link :doc do |opts|
          'https://api.gocd.org/#config-repos'
        end

        link :find do |opts|
          opts[:url_builder].apiv1_admin_config_repo_url(id: '__id__').gsub(/__id__/, ':id')
        end

        property :id
      end
    end
  end
end
