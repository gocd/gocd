##########################################################################
# Copyright 2017 ThoughtWorks, Inc.
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
  module Admin
    module MergedEnvironments
      class PipelineConfigSummaryRepresenter < ApiV1::BaseRepresenter
        def initialize(options)
          @environment = options[:environment]
          @pipeline = options[:pipeline]

          super(@pipeline)
        end

        link :self do |opts|
          opts[:url_builder].apiv3_admin_pipeline_url(@pipeline.getName())
        end

        link :doc do |opts|
          'https://api.gocd.org/#pipeline-config'
        end

        link :find do |opts|
          opts[:url_builder].apiv3_admin_pipeline_url(pipeline_name: '__pipeline_name__').gsub(/__pipeline_name__/, ':pipeline_name')
        end

        property :name,
                 exec_context: :decorator

        property :origin,
                 exec_context: :decorator,
                 decorator: lambda {|origin, *|
                   (origin.instance_of? FileConfigOrigin) ?
                     Shared::ConfigOrigin::ConfigXmlOriginRepresenter :
                     Shared::ConfigOrigin::ConfigRepoOriginRepresenter
                 }

        def origin
          @environment.isLocal ? FileConfigOrigin.new : @environment.getOriginForPipeline(@pipeline.name)
        end

        def name
          @pipeline.getName.to_s
        end
      end
    end
  end
end