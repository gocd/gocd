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
require_relative '../auto_refresh_examples'

describe 'pipelines/_pipeline_stage_bar.html.erb' do
  include JobMother
  include GoUtil
  include ReflectiveUtil

  before :each do
    @sim = double('stage_instance_model')
  end

  it "should show cancel button" do
    params[:pipeline_name] = 'pipeline_name'
    params[:stage_name] = 'stage_name'
    params[:pipeline_counter] = 111
    params[:stage_counter] = 222
    allow(@sim).to receive(:isRunning).and_return(true)
    allow(@sim).to receive(:canRun).and_return(false)
    allow(@sim).to receive(:getState).and_return(StageState::Building)
    allow(@sim).to receive(:getName).and_return("stage_name")
    allow(@sim).to receive(:getIdentifier).and_return(StageIdentifier.new)
    allow(@sim).to receive(:isScheduled).and_return(false)
    allow(@sim).to receive(:hasOperatePermission).and_return(true)
    allow(@sim).to receive(:getId).and_return(42)
    allow(@sim).to receive(:getApprovedBy).and_return("admin")
    allow(@sim).to receive(:isAutoApproved).and_return(true)

    render :partial => 'pipelines/pipeline_stage_bar', :locals => {:scope => {:stage_in_status_bar => @sim, :idx_in_status_bar => 1, :stage_name => 'stage_name'}}
    Capybara.string(response.body).find("#operate_stage_name").tap do |f|
      expect(f).to have_selector("a[onclick=\"AjaxRefreshers.disableAjax();spinny('operate_stage_name'); new Ajax.Request('/go/api/stages/pipeline_name/111/stage_name/222/cancel', {asynchronous:true, contentType:'application/json', evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, requestHeaders:{'X-GoCD-Confirm':'true', 'Accept':'application/vnd.go.cd+json'}}); return false;\"]")
    end
  end
end
