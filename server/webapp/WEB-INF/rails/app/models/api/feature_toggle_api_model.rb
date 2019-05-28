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

module Api
  class FeatureToggleAPIModel
    attr_reader :key, :description, :value, :has_changed

    def initialize(feature_toggle)
      @key = feature_toggle.key()
      @description = feature_toggle.description()
      @value = feature_toggle.isOn()
      @has_changed = feature_toggle.hasBeenChangedFromDefault()
    end
  end
end