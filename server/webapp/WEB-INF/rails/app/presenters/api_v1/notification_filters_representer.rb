#
# Copyright 2019 ThoughtWorks, Inc.
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
#

module ApiV1
  class NotificationFiltersRepresenter < ApiV1::BaseRepresenter

    FilterStruct = Struct.new(:id, :pipelineName, :stageName, :event, :myCheckin)

    def initialize(filters)
      @represented = Struct.new(:filters).new filters.to_a.map { |nf| FilterStruct.new(*Hash(nf.toMap).symbolize_keys.values_at(*FilterStruct.members)) }
    end

    collection :filters do
      property :id
      property :pipelineName, as: :pipeline
      property :stageName, as: :stage
      property :event
      property :myCheckin, as: :match_commits
    end

  end
end