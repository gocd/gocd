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

module ::GoSslHelper
  def self.included base
    base.alias_method_chain :mandatory_ssl, :config_enforcement
  end

  def mandatory_ssl_with_config_enforcement
    if Thread.current[:ssl_base_url].nil?
      @message = l.string("SSL_ENFORCED_BUT_BASE_NOT_FOUND")
      @status = 404
      render 'shared/ssl_not_configured_error', :status => @status, :layout => true
      return false
    end
    mandatory_ssl_without_config_enforcement
  end
end

Oauth2Provider::SslHelper.class_eval { include ::GoSslHelper }

