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
  module Scms
    class PluggableScmRepresenter < BaseRepresenter
      alias_method :scm, :represented

      error_representer

      link :self do |opts|
        opts[:url_builder].apiv1_admin_scm_url(material_name: scm.getName) if scm.getName
      end

      link :doc do
        'http://api.go.cd/#pluggable-scms'
      end
      property :errors, exec_context: :decorator, decorator: ApiV1::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }
      property :id
      property :name
      property :auto_update
      property :plugin_configuration,
               decorator: ApiV1::Scms::PluginConfigurationRepresenter,
               class: com.thoughtworks.go.domain.config.PluginConfiguration

      collection :configuration,
                 decorator: ApiV1::Scms::ConfigurationPropertyRepresenter,
                 class: com.thoughtworks.go.domain.config.ConfigurationProperty,
                 setter: lambda { |value, options|
                   self.setConfiguration(Configuration.new(value.to_java(ConfigurationProperty)))
                 }
    end
  end
end
