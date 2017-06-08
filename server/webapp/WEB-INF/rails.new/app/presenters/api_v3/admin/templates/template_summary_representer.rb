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

module ApiV3
  module Admin
    module Templates
      class TemplateSummaryRepresenter < BaseRepresenter

        link :self do |opts|
          opts[:url_builder].apiv3_admin_template_url(template_name: name.to_s) unless name.to_s.blank?
        end

        link :doc do |opts|
          'https://api.gocd.org/#template-config'
        end

        link :find do |opts|
          opts[:url_builder].apiv3_admin_template_url(template_name: '__template_name__').gsub(/__template_name__/, ':template_name')
        end

        property :name, case_insensitive_string: true, exec_context: :decorator
        collection :pipelines,
                   embedded: true,
                   exec_context: :decorator,
                   decorator: Admin::Templates::PipelineConfigSummaryRepresenter

        private

        def pipelines
          represented.getPipelines()
        end

        def name
          represented.getTemplateName()
        end
      end
    end
  end
end