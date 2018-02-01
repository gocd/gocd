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

module ApiV5
  module Shared
    module Stages
      class ArtifactRepresenter < BaseRepresenter
        alias_method :artifact, :represented

        error_representer({"src" => "source", "dest" => "destination"})

        ARTIFACT_TYPE_TO_STRING_TYPE_MAP = {
          ArtifactType::unit => 'test',
          ArtifactType::file => 'build'
        }

        STRING_TYPE_TO_ARTIFACT_TYPE_MAP = {
          'test' => ArtifactType::unit,
          'build' => ArtifactType::file
        }

        property :source
        property :destination
        property :type, exec_context: :decorator

        def type
          ARTIFACT_TYPE_TO_STRING_TYPE_MAP[artifact.getArtifactType]
        end

        def type=(value)
          unless (value == 'build' || value == 'test')
            raise UnprocessableEntity, "Invalid Artifact type: '#{value}'. It has to be one of test, build"
          end
          artifact.setArtifactType(STRING_TYPE_TO_ARTIFACT_TYPE_MAP[value])
        end
      end
    end
  end
end