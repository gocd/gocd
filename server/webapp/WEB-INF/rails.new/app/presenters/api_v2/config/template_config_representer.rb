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

module ApiV2
  module Config
    class TemplateConfigRepresenter < ApiV2::BaseRepresenter
      alias_method :template, :represented

      error_representer

      link :self do |opts|
        opts[:url_builder].apiv2_admin_template_url(template_name: template.name.to_s) unless template.name.blank?
      end

      link :doc do |opts|
        'https://api.go.cd/#template-config'
      end

      link :find do |opts|
        opts[:url_builder].apiv2_admin_template_url(template_name: '__template_name__').gsub(/__template_name__/, ':template_name')
      end


      property :errors, exec_context: :decorator, decorator: ApiV2::Config::ErrorRepresenter, skip_parse: true, skip_render: lambda { |object, options| object.empty? }
      property :name, case_insensitive_string: true

      collection :stages,
                 exec_context: :decorator,
                 decorator:    ApiV2::Config::StageRepresenter,
                 expect_hash:  true,
                 class:        com.thoughtworks.go.config.StageConfig

      def stages
        template.getStages() unless template.getStages().isEmpty
      end

      def stages=(value)
        template.getStages().clear()
        value.each { |stage| template.addStageWithoutValidityAssertion(stage) }
      end
    end
  end
end
