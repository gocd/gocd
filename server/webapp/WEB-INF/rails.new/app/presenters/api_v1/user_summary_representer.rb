##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
  class UserSummaryRepresenter < ApiV1::BaseRepresenter

    alias_method :login_name, :represented

    link :doc do
      'https://api.go.cd/current/#users'
    end

    link :self do |opts|
      opts[:url_builder].apiv1_user_url(login_name: login_name)
    end

    link :find do |opts|
      opts[:url_builder].apiv1_user_url(login_name: '__login_name__').gsub(/__login_name__/, ':login_name')
    end

    property :login_name, exec_context: :decorator

  end
end
