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

# applications can change these to suit your needs
if Gadgets.enabled?
  Gadgets.init do |config|
    config.application_name = 'Go'
    config.application_base_url = proc { Thread.current[:base_url].gsub(/\/+$/, '') + "/go" }
    config.ssl_base_url = proc { Thread.current[:ssl_base_url].gsub(/\/+$/, '') + "/go" }
    if Rails.env == 'test'
      config.truststore_path = File.join(Rails.root, 'tmp', "gadget_truststore.jks")
    else
      config.truststore_path = File.join(com.thoughtworks.go.util.SystemEnvironment.new.configDir().getAbsolutePath(), "gadget_truststore.jks")
    end
  end
end
