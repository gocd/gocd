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
    class ArtifactRepresenter < ApiV2::BaseRepresenter
      alias_method :artifact, :represented

      error_representer({"src" => "source", "dest" => "destination"})

      ARTIFACT_TYPE_TO_STRING_TYPE_MAP = {
        ArtifactType::unit => 'test',
        ArtifactType::file => 'build'
      }

      ARTIFACT_TYPE_TO_ARTIFACT_CLASS_MAP = {
        'test'  => TestArtifactPlan,
        'build' => ArtifactPlan
      }

      property :src, as: :source
      property :dest, as: :destination
      property :type, exec_context: :decorator, skip_parse: true

      def type
        ARTIFACT_TYPE_TO_STRING_TYPE_MAP[artifact.getArtifactType]
      end

      class << self
        def get_class_for_artifact_type(type)
          ARTIFACT_TYPE_TO_ARTIFACT_CLASS_MAP[type] || (raise ApiV2::UnprocessableEntity, "Invalid Artifact type: '#{type}'. It has to be one of #{ARTIFACT_TYPE_TO_ARTIFACT_CLASS_MAP.keys.join(', ')}")
        end
      end
    end
  end
end
