##########################GO-LICENSE-START################################
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
##########################GO-LICENSE-END##################################

require 'spec_helper'
require File.join(File.dirname(__FILE__), "..", "auto_refresh_examples")

describe 'pipelines/_pipeline_stage_bar.html.erb' do
  include JobMother, GoUtil, ReflectiveUtil

  before :each do
    @sim = double('stage_instance_model')
  end

  it "should show cancel button" do
    @sim.stub(:isRunning).and_return(true)
    @sim.stub(:canRun).and_return(false)
    @sim.stub(:getState).and_return(StageState::Building)
    @sim.stub(:getName).and_return("stage_name")
    @sim.stub(:getIdentifier).and_return(StageIdentifier.new)
    @sim.stub(:isScheduled).and_return(false)
    @sim.stub(:hasOperatePermission).and_return(true)
    @sim.stub(:getId).and_return(42)
    @sim.stub(:getApprovedBy).and_return("admin")
    @sim.stub(:isAutoApproved).and_return(true)

    render :partial => 'pipelines/pipeline_stage_bar', :locals => {:scope => {:stage_in_status_bar => @sim, :idx_in_status_bar => 1, :stage_name => 'stage_name'}}
    Capybara.string(response.body).find("#operate_stage_name").tap do |f|
      expect(f).to have_selector("a[onclick=\"AjaxRefreshers.disableAjax();spinny('operate_stage_name'); new Ajax.Request('/api/stages/42/cancel', {asynchronous:true, evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, requestHeaders:{'Confirm':'true'}}); return false;\"]")
    end
  end
end
