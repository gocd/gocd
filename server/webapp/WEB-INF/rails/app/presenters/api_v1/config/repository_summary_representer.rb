##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
  module Config
    class RepositorySummaryRepresenter < ApiV1::BaseRepresenter
      alias_method :package_repo, :represented

      link :self do |opts|
        opts[:url_builder].apiv1_admin_repository_url(repo_id: package_repo.id) unless package_repo.blank? && package_repo.id.blank?
      end

      link :doc do |opts|
        'https://api.gocd.org/#package-repositories'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_admin_repository_url(repo_id: '__repo_id__').gsub(/__repo_id__/, ':repo_id')
      end

      property :id
      property :name, skip_parse: true
    end
  end
end
