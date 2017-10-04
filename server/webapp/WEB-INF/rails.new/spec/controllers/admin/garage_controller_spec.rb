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

require 'rails_helper'

describe Admin::GarageController do
  before :each do
    @garage_service = double('garage service')
    allow(@controller).to receive(:garage_service).and_return(@garage_service)
  end

  describe :route do
    it "should resolve index" do
      expect({:get => "/admin/garage"}).to route_to(:controller => "admin/garage", :action => "index")
      expect(garage_index_path).to eq("/admin/garage")
    end

    it "should resolve gc" do
      expect({:post => "/admin/garage/gc"}).to route_to(:controller => "admin/garage", :action => "gc")
      expect(garage_gc_path).to eq("/admin/garage/gc")
    end
  end

  describe :index do
    it 'should query garage service for data' do
      expect(@garage_service).to receive(:getData).and_return('data')

      get :index

      expect(assigns[:garage_data]).to eq('data')
      assert_template layout: false
    end
  end

  describe :gc do
    before :each do
      @result = stub_localized_result
    end

    it 'should successfully call garage service to perform gc' do
      expect(@result).to receive(:isSuccessful).and_return(true)
      expect(@result).to receive(:message).and_return('message')
      expect(@garage_service).to receive(:gc).with(@result)

      post :gc

      expect(flash[:notice][:gc]).to eq('message')
      assert_redirect garage_index_path
    end

    it 'should populate error when gc fails' do
      expect(@result).to receive(:isSuccessful).and_return(false)
      expect(@result).to receive(:message).and_return('message')
      expect(@garage_service).to receive(:gc).with(@result)

      post :gc

      expect(flash[:error][:gc]).to eq('message')
      assert_redirect garage_index_path
    end
  end
end
