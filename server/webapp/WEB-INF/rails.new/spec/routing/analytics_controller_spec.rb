##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'rails_helper'

describe AnalyticsController do

  describe 'routes' do
    it 'should resolve the path' do
      expect(:get => '/analytics/plugin_id/pipeline_name').to route_to(controller: 'analytics', action: 'pipeline', plugin_id: 'plugin_id', pipeline_name: 'pipeline_name')
      expect(pipeline_analytics_path(plugin_id: 'test_plugin', pipeline_name: 'test_pipeline')).to eq('/analytics/test_plugin/test_pipeline')
    end
  end

end
