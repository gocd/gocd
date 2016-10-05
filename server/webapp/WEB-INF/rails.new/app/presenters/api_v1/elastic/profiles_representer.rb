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
  module Elastic
    class ProfilesRepresenter < ApiV1::BaseRepresenter
      alias_method :profiles, :represented
      link :self do |opts|
        opts[:url_builder].apiv1_elastic_profiles_url
      end

      link :doc do |opts|
        'https://api.go.cd/#elastic-agent-profiles'
      end

      link :find do |opts|
        opts[:url_builder].apiv1_elastic_profile_url(profile_id: '__profile_id__').gsub('__profile_id__', ':profile_id')
      end

      collection :profiles, embedded: true, exec_context: :decorator, decorator: ProfileRepresenter
    end
  end
end
