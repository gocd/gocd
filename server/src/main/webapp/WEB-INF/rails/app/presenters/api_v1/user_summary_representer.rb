#
# Copyright 2019 ThoughtWorks, Inc.
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

module ApiV1
  class UserSummaryRepresenter < ApiV1::BaseRepresenter

    alias_method :login_name, :represented

    link :doc do
      CurrentGoCDVersion.api_docs_url('#users')
    end

    link :current_user do |opts|
      spark_url_for(opts, SparkRoutes::CurrentUser::BASE)
    end

    link :self do |opts|
      spark_url_for(opts, SparkRoutes::UserSummary.self(login_name))
    end

    link :find do |opts|
      spark_url_for(opts, SparkRoutes::UserSummary.find())
    end

    property :login_name, exec_context: :decorator

  end
end
