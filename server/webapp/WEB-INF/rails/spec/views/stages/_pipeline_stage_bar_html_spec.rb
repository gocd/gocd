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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")
require File.join(File.dirname(__FILE__), "..",  "auto_refresh_examples")

describe 'stages/_pipeline_stage_bar.html.erb' do
  include JobMother, GoUtil, ReflectiveUtil

  before :each do
    @sim = mock('stage_instance_model')
  end

  it "should show cancel button" do
    @sim.should_receive(:isRunning).any_number_of_times.and_return(true)
    @sim.should_receive(:canRun).any_number_of_times.and_return(false)
    @sim.should_receive(:getState).any_number_of_times.and_return(StageState::Building)
    @sim.should_receive(:getName).any_number_of_times.and_return("stage_name")
    @sim.should_receive(:getIdentifier).any_number_of_times.and_return(StageIdentifier.new)
    @sim.should_receive(:isScheduled).any_number_of_times.and_return(false)
    @sim.should_receive(:hasOperatePermission).any_number_of_times.and_return(true)
    @sim.should_receive(:getId).any_number_of_times.and_return(42)
    @sim.should_receive(:getApprovedBy).any_number_of_times.and_return("admin")
    @sim.should_receive(:isAutoApproved).any_number_of_times.and_return(true)

    render :partial => 'pipelines/pipeline_stage_bar.html', :locals => {:scope => {:stage_in_status_bar => @sim, :idx_in_status_bar => 1, :stage_name => 'stage_name'}}

    response.body.should have_tag("#operate_stage_name") do
      with_tag("a[onclick=?]", "AjaxRefreshers.disableAjax();spinny('operate_stage_name'); new Ajax.Request('/api/stages/42/cancel', {asynchronous:true, evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}}); return false;")
    end
  end

end