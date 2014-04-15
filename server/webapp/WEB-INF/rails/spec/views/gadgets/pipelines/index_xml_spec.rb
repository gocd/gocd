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

require File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper")

describe '/gadgets/pipelines/index' do

  describe 'Module Preferences' do
    it "should return gadget spec" do
      render '/gadgets/pipeline/index.xml'
      response.body.should have_tag("Module")
    end

    it "should return an gadget which requires oauthpopup" do
      render '/gadgets/pipeline/index.xml'
      response.body.should have_tag("ModulePrefs[title='Pipeline Status']") do |module_prefs|
        module_prefs.should have_tag("Require[feature='oauthpopup']")
        module_prefs.should have_tag("Require[feature='setprefs']")
      end
    end

    it "should return an oauth gadget" do
      render '/gadgets/pipeline/index.xml'

      response.body.should have_tag("Module ModulePrefs[title='Pipeline Status']") do |module_prefs|
        module_prefs.should have_tag("OAuth") do |oauth|
          oauth.should have_tag("Service[name='Go']") do |service|
            service.should have_tag("Request[method='GET'][url='http://test.host/oauth/token']")
            service.should have_tag("Access[method='GET'][url='http://test.host/oauth/token']")
            service.should have_tag("Authorization[url='http://test.host/oauth/authorize']")
          end
        end
      end
    end
  end

  describe 'User Preference' do

#    <UserPref name="accountNumber" datatype="string" default_value="-1" />

    it "should render content of type html" do
      render '/gadgets/pipeline/index.xml', :pipeline_name => "mingle"

      response.body.should have_tag("Module UserPref[name='pipelineName'][datatype='string'][default_value='']")
    end
  end

  describe 'Content' do
#     <Content type="html">
#        <![CDATA[
#            content
#    ]]>
#      </Content>

    it "should render content of type html" do
      render '/gadgets/pipeline/index.xml'

      response.body.should have_tag("Module Content[type='html']")
    end
  end
end