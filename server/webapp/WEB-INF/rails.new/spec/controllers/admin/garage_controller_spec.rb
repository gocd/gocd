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

describe Admin::GarageController do
  describe :route do
    it "should resolve index" do
      {:get => "/admin/garage"}.should route_to(:controller => "admin/garage", :action => "index")
      garage_index_path.should == "/admin/garage"
    end

    it "should resolve gc" do
      {:post => "/admin/garage/gc"}.should route_to(:controller => "admin/garage", :action => "gc")
      garage_gc_path.should == "/admin/garage/gc"
    end
  end

  describe :index do
    before :each do
      @garage_service = double('garage service')
      @controller.stub(:garage_service).and_return(@garage_service)
    end

    it 'should query garage service for data' do
      @garage_service.should_receive(:getData).and_return('data')

      get :index

      assigns[:garage_data].should == 'data'
      assert_template layout: false
    end
  end

  describe :gc do
    before :each do
      @garage_service = double('garage service')
      @controller.stub(:garage_service).and_return(@garage_service)
      @result = stub_localized_result
    end

    it 'should successfully call garage service to perform gc' do
      @result.should_receive(:isSuccessful).and_return(true)
      @result.should_receive(:message).and_return('message')
      @garage_service.should_receive(:gc).with(@result)

      post :gc

      flash[:notice][:gc].should == 'message'
      assert_redirect garage_index_path
    end

    it 'should populate error when gc fails' do
      @result.should_receive(:isSuccessful).and_return(false)
      @result.should_receive(:message).and_return('message')
      @garage_service.should_receive(:gc).with(@result)

      post :gc

      flash[:error][:gc].should == 'message'
      assert_redirect garage_index_path
    end
  end
end
