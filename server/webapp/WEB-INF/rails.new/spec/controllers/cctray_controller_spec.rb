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

require 'spec_helper'

describe CctrayController do
  describe "routes" do
    it "should resolve its route" do
      expect(:get => '/cctray.xml').to route_to(:controller => "cctray", :action => "index", :format => "xml")
    end
  end

  context "cctray" do
    describe "index" do
      after do
        $servlet_context = nil
      end

      it "should use current request details to get correct url prefix" do
        expected_prefix = "http://my.site.url:8153/context_path"

        $servlet_context = double("servlet_context")
        expect($servlet_context).to receive(:getContextPath).and_return("/context_path")

        server_config_service = stub_service(:server_config_service)
        expect(server_config_service).to receive(:siteUrlFor).with("http://test.host/context_path", false).and_return(expected_prefix)

        cc_tray_service = stub_service(:cc_tray_service)
        expect(cc_tray_service).to receive(:getCcTrayXml).with(expected_prefix).and_return("RESPONSE_FOR_THIS_URL_PREFIX")

        get :index

        expect(response.body).to eq("RESPONSE_FOR_THIS_URL_PREFIX")
      end
    end
  end
end
