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

describe Api::StagesController do

  describe "index" do
    before :each do
      allow(controller).to receive(:stage_service).and_return(@stage_service = double('stage_service'))
      # allow(controller).to receive(:set_locale)
      allow(controller).to receive(:populate_config_validity)
    end

    it "should return a 404 HTTP response when id is not a number" do
      get 'index', params:{:id => "does-not-exist", :format => "xml", :no_layout => true}
      expect(response.status).to eq(404)
    end

    it "should return a 404 HTTP response when stage cannot be loaded" do
      expect(@stage_service).to receive(:stageById).with(99).and_throw(Exception.new("foo"))
      get 'index', params:{:id => "99", :format => "xml", :no_layout => true}
      expect(response.status).to eq(404)
    end

    it "should load stage data" do
      updated_date = java.util.Date.new
      stage = StageMother.create_passed_stage("pipeline_name", 100, "blah-stage", 12, "dev", updated_date)
      expect(@stage_service).to receive(:stageById).with(99).and_return(stage)
      get 'index', params:{:id => "99", :format => "xml", :no_layout => true}

      context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, SystemEnvironment.new)
      expect(assigns[:doc].asXML()).to eq(StageXmlViewModel.new(stage).toXml(context).asXML())
    end
  end
end
