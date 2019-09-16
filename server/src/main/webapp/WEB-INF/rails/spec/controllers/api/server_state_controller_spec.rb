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

describe Api::ServerStateController do

  before :each do
    @system_environment = double('system_environment')
    allow(controller).to receive(:system_environment).and_return(@system_environment)
  end

  it 'should answer to /api/state/status' do
    expect(get: '/api/state/status').to route_to(action: 'status', controller: 'api/server_state', no_layout: true)
  end

  it 'should answer to /api/state/active' do
    expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
    expect(post: '/api/state/active').to route_to(action: 'to_active', controller: 'api/server_state', no_layout: true)
  end

  it 'should answer to /api/state/passive' do
    expect_any_instance_of(HeaderConstraint).to receive(:matches?).with(any_args).and_return(true)
    expect(post: '/api/state/passive').to route_to(action: 'to_passive', controller: 'api/server_state', no_layout: true)
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
