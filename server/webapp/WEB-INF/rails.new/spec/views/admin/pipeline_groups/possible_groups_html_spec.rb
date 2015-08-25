##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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

require 'spec_helper'

describe "admin/pipeline_groups/possible_groups.html.erb" do
  it "should render _possible_groups_popup.html.erb" do
    @possible_groups = []
    @pipeline_name = "test"
    @md5_match = false

    stub_template "possible_groups_popup" => "possible group popup"

    render

    assert_template partial: "possible_groups_popup", :locals => {:scope => {:possible_groups => @possible_groups, :pipeline_name => @pipeline_name, :md5_match => @md5_match}}
  end
end
