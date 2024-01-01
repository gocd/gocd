#
# Copyright 2024 Thoughtworks, Inc.
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

describe ApplicationHelper do
  include ApplicationHelper

  describe "url_for_path" do

    before :each do
      allow(controller).to receive(:root_path).and_return("/go/quux?x")
    end

    it "should handle default_url_options" do
      url = url_for_path("/foo")
      expect(url).to eq("/go/quux/foo?x")
    end

    it "should handle default_url_options" do
      url = url_for_path("/foo?bar=blah")
      expect(url).to eq("/go/quux/foo?bar=blah&x")
    end

    it "should handle query params" do
      url = url_for_path("/foo")
      expect(url).to eq("/go/quux/foo?x")
    end

    it "should handle url without params" do
      allow(controller).to receive(:root_path).and_return("/go/quux")
      url = url_for_path("/foo")
      expect(url).to eq("/go/quux/foo")
    end

    it "should handle root url with trailing slash and provided sub path with leading slash" do
      allow(controller).to receive(:root_path).and_return("/go/quux/")
      url = url_for_path("/foo")
      expect(url).to eq("/go/quux/foo")
    end
  end

  it "should give the server version" do
    version == "N/A"
  end

  it "should ask security service whether user is an admin" do
    expect(self).to receive(:security_service).and_return(security_service = double("security_service"))
    expect(self).to receive(:current_user).and_return(:user)
    expect(security_service).to receive(:canViewAdminPage).with(:user).and_return(:is_admin?)
    expect(can_view_admin_page?).to eq(:is_admin?)
  end

  it "should generate object_id based dom id" do
    obj = Object.new
    expect(id_for(obj)).to eq("Object_#{obj.object_id}")
  end

  it "should use prefix given for dom id" do
    obj = Object.new
    expect(id_for(obj, "prefix")).to eq("prefix_#{obj.object_id}")
  end

  describe 'render_json' do

    it "should escape html for json" do
      random_html = <<-end
      <div id="something">
        <p>This should be 'arbitrary' "html"</p>
      </div>
      end
      expect(self).to receive(:render).and_return random_html
      json = JSON.parse("{\"result\":" + render_json() + "}")
      expect(json["result"]).to eq(random_html)
    end

    it "should include locals by default" do
      expect(self).to receive(:render).with({ :locals => {:scope => {}} }).and_return "foo"
      json = JSON.parse("{\"result\":" + render_json() + "}")
      expect(json["result"]).to eq("foo")
    end
  end

  describe 'form remote add on' do

    it "should create a blocking link to a remote location" do
      actual = link_blocking_post_to_server :name => "&nbsp;",
                                            :url => com.thoughtworks.go.spark.Routes::Pipeline.schedule('SOME_NAME'),
                                            :html => {},
                                            :idForSpinner => "schedule"

      exp = %q|<a href="#"  onclick="Util.ajaxUpdate('/api/pipelines/SOME_NAME/schedule', 'schedule'); return false;">&nbsp;</a>|
      expect(actual).to eq(exp)
    end

    it "should create a blocking link to a remote location with extra HTML provided" do
      actual = link_blocking_post_to_server :name => "&nbsp;",
                                            :url => com.thoughtworks.go.spark.Routes::Pipeline.schedule('SOME_NAME'),
                                            :html => {:class => "ABC", :title => "TITLE", :id => "SOME-ID" },
                                            :idForSpinner => "schedule"

      exp = %q|<a href="#"  class="ABC" title="TITLE" id="SOME-ID" onclick="Util.ajaxUpdate('/api/pipelines/SOME_NAME/schedule', 'schedule'); return false;">&nbsp;</a>|
      expect(actual).to eq(exp)
    end
  end

  describe 'submit button' do
    it "should not have image or class 'select' for a type 'button'" do
      expect(submit_button("name", :type => 'button', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick")).to eq("<button type=\"button\" id=\"id\" name=\"name\" class=\"class submit button\" onclick=\"onclick\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end

    it "should respect disabled flag for type 'button'" do
      expect(submit_button("name", :type => 'button', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick", :disabled => true)).to eq("<button type=\"button\" id=\"id\" name=\"name\" class=\"class submit button disabled\" onclick=\"onclick\" disabled=\"disabled\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end

    it "should accept either symbol or string as option keys" do
      expect(submit_button("name", 'type' => 'button', 'id'=> 'id', 'name' => "name", 'class' => "class", 'onclick' => "onclick", 'disabled' => true)).to eq("<button type=\"button\" id=\"id\" name=\"name\" class=\"class submit button disabled\" onclick=\"onclick\" disabled=\"disabled\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end

    it "should not generate blank attribute value for enabled button" do
      expect(submit_button("name")).to eq("<button type=\"submit\" value=\"name\" class=\"submit\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end

    it "should not generate blank attribute value for disabled button" do
      expect(submit_button("name", :disabled => true)).to eq("<button type=\"submit\" disabled=\"disabled\" value=\"name\" class=\"submit disabled\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end
  end

  it "should return FlashMessageModel from flash[key]='string'" do
    service = double("flash_message_service")
    expect(service).to receive(:get).with("quux").and_return("bang")
    allow(self).to receive(:flash_message_service).and_return(service)
    expect(load_flash_message(:quux)).to eq("bang")

    service = double("flash_message_service")
    allow(service).to receive(:get).with(anything).and_return(nil)
    allow(self).to receive(:flash_message_service).and_return(service)
    allow(self).to receive(:session).and_return(session = {:foo => "bar"})
    expect(load_flash_message(:foo)).to eq("bar")
    expect(session).to be_empty

    allow(self).to receive(:session).and_return(session = {})
    expect(load_flash_message(:foo)).to be_nil
    expect(session).to be_empty

    allow(self).to receive(:flash).and_return(flash = {:error => "i errored"})
    expect(load_flash_message(:notice)).to eq(FlashMessageModel.new("i errored", "error"))
    expect(flash[:error]).to eq("i errored")

    allow(self).to receive(:flash).and_return({:notice => "some notice"})
    expect(load_flash_message(:notice)).to eq(FlashMessageModel.new("some notice", "notice"))

    allow(self).to receive(:flash).and_return({:success => "is success"})
    expect(load_flash_message(:notice)).to eq(FlashMessageModel.new("is success", "success"))
  end

  describe "load_from_flash" do
    it "should render multiple flash errors seperated by a period" do
      allow(self).to receive(:flash).and_return({:error => ["I errored", "You errored", "We all errored"]})
      flash_model = load_from_flash
      expect(flash_model.toString()).to eq("I errored. You errored. We all errored")
    end
  end

  describe "unauthorized_access" do

    it "should return true if status is 403" do
      @status = 403
      expect(access_forbidden).to eq(true)
    end
    it "should return false if status is not true" do
      @status = 200
      expect(access_forbidden).to eq(false)
    end

  end

  describe "vsm_analytics" do
    before :each do
      @default_plugin_info_finder = double('default_plugin_info_finder')
      vendor = GoPluginDescriptor::Vendor.new('bob', 'https://bob.example.com')
      about = GoPluginDescriptor::About.builder
                .name("Foo plugin")
                .version("1.2.3")
                .targetGoVersion("17.2.0")
                .description("Does foo")
                .vendor(vendor)
                .targetOperatingSystems(["Linux"])
                .build
      descriptor = proc do |id| GoPluginDescriptor.builder.id(id).version("1.0").about(about).build end

      supports_analytics = proc do |supports_vsm_analytics|
        supported = []
        supported << com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics.new("vsm", "id1", "title1") if supports_vsm_analytics
        supported << com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics.new("vsm", "id2", "title2") if supports_vsm_analytics
        com.thoughtworks.go.plugin.domain.analytics.Capabilities.new(supported)
      end

      @plugin_info1 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin1'), nil, supports_analytics.call(true), nil))
      @plugin_info2 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin2'), nil, supports_analytics.call(true), nil))
      @plugin_info3 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin3'), nil, supports_analytics.call(false), nil))
      @plugin_info4 = CombinedPluginInfo.new(AnalyticsPluginInfo.new(descriptor.call('plugin3'), nil, supports_analytics.call(false), nil))

    end

    describe "vsm_analytics_chart_info" do
      it "should return info of first plugin which supports vsm analytics" do
        def default_plugin_info_finder; @default_plugin_info_finder; end
        def is_user_an_admin?; true; end

        allow(@default_plugin_info_finder).to receive('allPluginInfos').with(PluginConstants::ANALYTICS_EXTENSION).and_return([@plugin_info1, @plugin_info2, @plugin_info3, @plugin_info4])

        expected = {
          "id"        => "id1",
          "plugin_id" => "plugin1",
          "type"      => "vsm",
          "url"       => "/analytics/plugin1/vsm/id1",
          "title"     => "title1"
        }
        expect(vsm_analytics_chart_info).to eq(expected)
      end
    end

    describe "supports_vsm_analytics?" do
      it "should support vsm analytics if there is atleast one analytics plugin which supports vsm analytics" do
        def default_plugin_info_finder; @default_plugin_info_finder; end
        def is_user_an_admin?; true; end

        allow(@default_plugin_info_finder).to receive('allPluginInfos').with(PluginConstants::ANALYTICS_EXTENSION).and_return([@plugin_info1, @plugin_info2, @plugin_info3, @plugin_info4])

        expect(supports_vsm_analytics?).to eq(true)
      end

      it "should support vsm analytics for all users" do
        def default_plugin_info_finder; @default_plugin_info_finder; end
        def is_user_an_admin?; false; end

        allow(@default_plugin_info_finder).to receive('allPluginInfos').with(PluginConstants::ANALYTICS_EXTENSION).and_return([@plugin_info1, @plugin_info2, @plugin_info3, @plugin_info4])

        expect(supports_vsm_analytics?).to eq(true)
      end

      it "should support vsm analytics only to admins" do
        def default_plugin_info_finder; @default_plugin_info_finder; end
        def is_user_an_admin?; false; end
        def show_analytics_only_for_admins?; true; end

        expect(supports_vsm_analytics?).to eq(false)
      end

      it "should not support vsm analytics in absence of a analytics plugin which supports vsm analytics" do
        def default_plugin_info_finder; @default_plugin_info_finder; end
        def is_user_an_admin?; true; end

        allow(@default_plugin_info_finder).to receive('allPluginInfos').with(PluginConstants::ANALYTICS_EXTENSION).and_return([@plugin_info3, @plugin_info4])

        expect(supports_vsm_analytics?).to eq(false)
      end
    end
  end

  describe "site_footer" do
    it 'should return maintenance mode update time when server is in maintenance mode' do
      expect(maintenance_mode_service).to receive(:isMaintenanceMode).and_return(true)
      expect(maintenance_mode_service).to receive(:updatedOn).and_return("date")

      expected = "date"
      expect(maintenance_mode_updated_on).to eq(expected)
    end

    it 'should not return maintenance mode update time when server is in maintenance mode' do
      expect(maintenance_mode_service).to receive(:isMaintenanceMode).and_return(false)

      expected = nil
      expect(maintenance_mode_updated_on).to eq(expected)
    end

    it 'should return maintenance approver when server is in maintenance mode' do
      expect(maintenance_mode_service).to receive(:isMaintenanceMode).and_return(true)
      expect(maintenance_mode_service).to receive(:updatedBy).and_return("bob")

      expected = "bob"
      expect(maintenance_mode_updated_by).to eq(expected)
    end

    it 'should not return maintenance approver when server is in maintenance mode' do
      expect(maintenance_mode_service).to receive(:isMaintenanceMode).and_return(false)

      expected = nil
      expect(maintenance_mode_updated_by).to eq(expected)
    end
  end

  it 'should render duration to string' do
    expect(duration_to_string(org.joda.time.Duration.new(1230 * 1000))).to eq('20 minutes and 30 seconds')
  end

  describe :stage_width_percent do
    it "should return percent" do
      expect(stage_width_percent(3, true, 20.0)).to eq("6.6667%")
    end
  end
end
