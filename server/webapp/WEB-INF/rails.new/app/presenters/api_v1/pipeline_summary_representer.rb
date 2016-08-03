##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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
  class PipelineSummaryRepresenter < ApiV1::BaseRepresenter
    alias_method :pipeline, :represented

    link :doc do |opts|
      'https://api.go.cd/#pipelines'
    end

    property :pipeline_name, as: :name
    property :pipeline_counter, as: :counter
    property :pipeline_label, as: :label

  end
end
