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
  module Config
    class PipelineConfigWithMinimalAttributesRepresenter < ApiV1::BaseRepresenter
      alias_method :pipeline, :represented

      link :self do |opts|
        opts[:url_builder].apiv1_admin_pipeline_url(pipeline_name: pipeline.name)
      end

      property   :name, exec_context: :decorator
      collection :stages,
                 exec_context: :decorator,
                 decorator:    ApiV1::Config::StageWithMinimalAttributesRepresenter,
                 expect_hash:  true,
                 class:        com.thoughtworks.go.config.StageConfig

      def stages
        pipeline.getStages
      end

      def name
        pipeline.name.toString
      end
    end
  end
end
