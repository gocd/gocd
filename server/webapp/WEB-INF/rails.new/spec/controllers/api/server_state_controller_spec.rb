##########################GO-LICENSE-START################################
# Copyright 2015 ThoughtWorks, Inc.
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

describe Api::ServerStateController do

  before :each do
    @system_environment = double('system_environment')
    allow(controller).to receive(:system_environment).and_return(@system_environment)
  end

  it 'should return server state status as active' do
    expect(@system_environment).to receive(:isServerActive).and_return(true)
    get :status
    expected_state('active')
    expect_content_type_json
  end

  it 'should return server state status as passive' do
    expect(@system_environment).to receive(:isServerActive).and_return(false)
    get :status
    expected_state('passive')
    expect_content_type_json
  end

  it 'should update server state to passive' do
    expect(@system_environment).to receive(:isServerActive).and_return(false)
    expect(@system_environment).to receive(:switchToPassiveState)
    post :to_passive
    expected_state('passive')
    expect_content_type_json
  end

  it 'should update server state to active' do
    expect(@system_environment).to receive(:isServerActive).and_return(true)
    expect(@system_environment).to receive(:switchToActiveState)
    post :to_active
    expected_state('active')
    expect_content_type_json
  end

  def expected_state(state)
    expect(JSON.parse(response.body)).to eq({'state' => state})
  end

  def expect_content_type_json
    expect(response.header['Content-Type']).to eq('application/json; charset=utf-8')
  end
end
