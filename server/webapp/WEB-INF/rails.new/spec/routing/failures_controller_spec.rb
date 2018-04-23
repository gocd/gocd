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

require 'rails_helper'

describe FailuresController do
  it "should resolve the route to show action" do
    expect({:get => "/failures/foo_pipeline/10/bar_stage/5/baz_job/quux_suite/bang_test"}).to route_to(:controller => "failures", :action => "show", :pipeline_name => "foo_pipeline", :pipeline_counter => "10", :stage_name => "bar_stage", :stage_counter => "5", :job_name => "baz_job", :suite_name => "quux_suite", :test_name => "bang_test", :no_layout => true)
  end
end
