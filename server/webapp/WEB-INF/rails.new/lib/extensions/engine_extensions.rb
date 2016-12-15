##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

Oauth2Provider::Configuration.ssl_base_url = proc { Spring.bean('go_config_service'.camelize(:lower)).getCurrentConfig.server.getSecureSiteUrl.getUrl }
Oauth2Provider::Configuration.ssl_not_configured_message = "Please set the secureSiteURL attribute in the configuration file."

Oauth2Provider::ModelBase.instance_eval do
  def datasource
    @go_oauth_provider_datasource ||= Spring.bean("oauthRepository")
  end
end
