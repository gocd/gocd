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

class PackagePropertyModel
  attr_accessor :display_name, :value, :name, :is_mandatory, :is_secure

  def initialize(package_configuration, config_property)
    @display_name = package_configuration.getOption(com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration::DISPLAY_NAME).to_s.empty? ? package_configuration.getKey() : package_configuration.getOption(com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration::DISPLAY_NAME)
    @is_mandatory = package_configuration.getOption(com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration::REQUIRED)
    @is_secure = package_configuration.getOption(com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration::SECURE)
    if config_property
      if (@is_secure)
        @value = config_property.getEncryptedValue()
      else
        @value = config_property.getConfigValue()
      end
    end
    @name = package_configuration.getKey()
  end
end
