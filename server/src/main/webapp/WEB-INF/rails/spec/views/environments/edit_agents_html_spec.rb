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

require 'rails_helper'

describe "environments/edit_agents.html.erb" do
  include GoUtil
  include FormUI
  include ReflectiveUtil

  before do
    @environment = EnvironmentConfigMother.environment("env")
    @environment.addEnvironmentVariable("plain_name", "plain_value")
    assign(:environment, @environment)

    allow(view).to receive(:cruise_config_md5).and_return("foo_bar_baz")
  end

  it "should have cruise_config_md5 as part of output" do
    stub_template "environments/_edit_agents.html.erb" => "DUMMY"
    render
    expect(response.body).to have_selector("form input[type='hidden'][name='cruise_config_md5'][value='foo_bar_baz']", visible: :hidden)
  end
end
