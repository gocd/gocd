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

describe 'gadgets/pipeline/index.xml.erb' do
  describe 'Module Preferences' do
    it "should return gadget spec" do
      render

      expect(Nokogiri::XML(response.body).xpath("/Module")).to_not be_nil_or_empty
    end

    it "should return a gadget which requires oauthpopup" do
      render

      Nokogiri::XML(response.body).xpath("//ModulePrefs[@title='Pipeline Status']").tap do |module_prefs|
        expect(module_prefs.xpath("Require[@feature='oauthpopup']")).to_not be_nil_or_empty
        expect(module_prefs.xpath("Require[@feature='setprefs']")).to_not be_nil_or_empty
      end
    end

    it "should return an oauth gadget" do
      render

      Nokogiri::XML(response.body).xpath("/Module/ModulePrefs[@title='Pipeline Status']").tap do |module_prefs|
        module_prefs.xpath("OAuth").tap do |oauth|
          oauth.xpath("Service[@name='Go']").tap do |service|
            expect(service.xpath("Request[@method='GET'][@url='http://test.host/oauth/token']")).to_not be_nil_or_empty
            expect(service.xpath("Access[@method='GET'][@url='http://test.host/oauth/token']")).to_not be_nil_or_empty
            expect(service.xpath("Authorization[@url='http://test.host/oauth/authorize']")).to_not be_nil_or_empty
          end
        end
      end
    end
  end

  describe 'User Preference' do

#    <UserPref name="accountNumber" datatype="string" default_value="-1" />

    it "should render content of type html" do
      render :template => 'gadgets/pipeline/index.xml.erb', :pipeline_name => "mingle"

      expect(Nokogiri::XML(response.body).xpath("/Module/UserPref[@name='pipelineName'][@datatype='string'][@default_value='']")).to_not be_nil_or_empty
    end
  end

  describe 'Content' do
#     <Content type="html">
#        <![CDATA[
#            content
#    ]]>
#      </Content>

    it "should render content of type html" do
      render

      expected_content = %q^
<script type="text/javascript">
    gadgets.util.registerOnLoadHandler(function() {
      var contentUrl = 'http://test.host/gadgets/pipeline/content';
      var pipelineName = new gadgets.Prefs().getString("pipelineName");
      var requestUrl = contentUrl + "?pipeline_name=" + pipelineName;

      gadget_toolkit.makeOAuthRequest(requestUrl, {
        view:             "gadget_content",
        gadgetTitle:      'Pipeline Status',
        serviceName:      'Go',
        autoRefresh:      new gadgets.Prefs().getBool("autoRefresh"),
        refreshInterval:  60000
      });
    });
</script>
<div id="gadget_content">
</div>^

      expect(Nokogiri::XML(response.body).xpath("/Module/Content[@type='html']").text.strip).to include(expected_content.strip)
    end
  end
end
