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

module ApiV4
  module Config
    module TemplateStagesRepresenterHelper
      def self.included(klass)
        klass.class_eval do
          collection :stages,
                     exec_context: :decorator,
                     decorator: ApiV4::Config::StageRepresenter,
                     expect_hash: true,
                     class: com.thoughtworks.go.config.StageConfig

          def stages
            represented.getStages() unless represented.getStages().isEmpty
          end

          def stages=(value)
            represented.getStages().clear()
            value.each { |stage| represented.addStageWithoutValidityAssertion(stage) }
          end
        end
      end
    end
  end
end