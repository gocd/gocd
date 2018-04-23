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

describe Admin::StatusReportsController do
  it 'should route to show' do
    expect({:get => '/admin/status_reports/pluginId'}).to route_to(:controller => 'admin/status_reports', :action => 'show', :plugin_id => 'pluginId')
    expect(admin_status_report_path(:plugin_id => 'com.tw.myplugin')).to eq('/admin/status_reports/com.tw.myplugin')
  end
end