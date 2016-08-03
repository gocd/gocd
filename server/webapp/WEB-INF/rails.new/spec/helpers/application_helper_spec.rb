##########################GO-LICENSE-START################################
# Copyright 2016 ThoughtWorks, Inc.
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

describe ApplicationHelper do
  include ApplicationHelper, RailsLocalizer

  it "should generate a label tag with required asterisk" do
    mock_form = double(:form)
    mock_form.should_receive(:label).with("name", "value<span class='asterisk'>*</span>")
    required_label(mock_form, "name", "value")
  end

  it "should respect anchor classes provided irrespective of tab being current" do
    allow(self).to receive(:url_for).and_return("/go/quux")
    tab_for("quux", :class => "foo bar", :anchor_class => "skip_dirty_stop").should == "<li id='cruise-header-tab-quux' class=' foo bar'>\n<a class=\"skip_dirty_stop\" href=\"/go/quux\">QUUX</a>\n</li>"
  end

  describe "url_for_path" do

    before :each do
      main_app = double('main app')
      allow(controller).to receive(:main_app).and_return(main_app)
      allow(main_app).to receive(:root_path).and_return("/go/quux?x")
    end

    it "should handle default_url_options" do
      url = url_for_path("/foo")
      url.should == "/go/quux/foo?x"
    end

    it "should handle default_url_options" do
      url = url_for_path("/foo?bar=blah")
      url.should == "/go/quux/foo?bar=blah&x"
    end

    it "should handle query params" do
      url = url_for_path("/foo")
      url.should == "/go/quux/foo?x"
    end

    it "should handle url without params" do
      allow(main_app).to receive(:root_path).and_return("/go/quux")
      url = url_for_path("/foo")
      url.should == "/go/quux/foo"
    end

    it "should handle root url with trailing slash and provided sub path with leading slash" do
      allow(main_app).to receive(:root_path).and_return("/go/quux/")
      url = url_for_path("/foo")
      url.should == "/go/quux/foo"
    end
  end

  describe "url_for_login" do
    before :each do
      main_app = double('main app')
      allow(controller).to receive(:main_app).and_return(main_app)
      allow(main_app).to receive(:root_path).and_return("/go/quux?x")
    end

    it "should give the url for login" do
      url_for_login.should == "/go/quux/auth/login?x"
    end
  end

  it "should give the server version" do
    version == "N/A"
  end

  it "should generate hidden field for config_md5" do
    allow(self).to receive(:cruise_config_md5).and_return("foo_bar_baz")
    config_md5_field.should == '<input id="cruise_config_md5" name="cruise_config_md5" type="hidden" value="foo_bar_baz" />'
  end

  describe :tab_for do
    before do
      allow(self).to receive(:url_for).and_return("/go/quux")
      allow(self).to receive(:root_path).and_return("/go/quux")
    end

    describe 'with link enabled' do
      before do
        should_receive(:url_for_path).with("baz").and_return(baz_url = "http://foo.bar:8153/go/baz")
        should_receive(:link_to).with("BAZ", baz_url, { :target => nil, :class => "" }).and_return("link_to_baz")
      end

      describe "with current" do
        before do
          should_receive(:url_for_path).with("quux").and_return(quux_url = "http://foo.bar:8153/go/quux")
          should_receive(:link_to).with("QUUX", quux_url, { :target => nil, :class => "" }).and_return("link_to_quux")
        end

        it "should understand current tab" do
          tab_for("quux").should == "<li id='cruise-header-tab-quux' class='current '>\nlink_to_quux\n</li>"
          tab_for("baz").should == "<li id='cruise-header-tab-baz' class=' '>\nlink_to_baz\n</li>"
        end

        it "should respect classes provided irrespective of tab being current" do
          tab_for("quux", :class => "foo bar").should == "<li id='cruise-header-tab-quux' class='current foo bar'>\nlink_to_quux\n</li>"
          tab_for("baz",  :class => "foo bar").should == "<li id='cruise-header-tab-baz' class=' foo bar'>\nlink_to_baz\n</li>"
        end
      end

      it "should honor current tab override" do
        @current_tab_name = 'baz'
        tab_for("baz").should == "<li id='cruise-header-tab-baz' class='current '>\nlink_to_baz\n</li>"
      end
    end

    it "should respect option :link" do
      tab_for("quux", :link => :disabled).should == "<li id='cruise-header-tab-quux' class=' '>\n<span>QUUX</span>\n</li>"
    end

    it "should respect option :target" do
      main_app = double('main app')
      allow(controller).to receive(:main_app).and_return(main_app)
      allow(main_app).to receive(:root_path).and_return("/go/quux")
      should_receive(:link_to).with("QUUX", "/go/quux/quux", { :target => 'foo', :class => "" }).and_return("link_to_quux")
      tab_for("quux", :target => 'foo').should == "<li id='cruise-header-tab-quux' class=' '>\nlink_to_quux\n</li>"
    end

    it "should substitute ' ' with - for generating css class name" do
      tab_for("foo bar", :link => :disabled).should == "<li id='cruise-header-tab-foo-bar' class=' '>\n<span>FOO BAR</span>\n</li>"
    end

    it "should honor url if provided with one" do
      should_receive(:url_for_path).with("foo/bar/baz").and_return(quux_url = "http://foo.bar:8153/go/foo/bar/baz")
      tab_for("foo bar", :url => 'foo/bar/baz').should == "<li id='cruise-header-tab-foo-bar' class=' '>\n<a class=\"\" href=\"/go/quux\">FOO BAR</a>\n</li>"
    end
  end

  it "should understand if mycruise link tab is supposed to be enabled" do
    should_receive(:go_config_service).and_return(go_config_service = Object.new)
    go_config_service.should_receive(:isSecurityEnabled).and_return(true)
    mycruise_available?.should == true
  end

  it "should ask security service whether user is an admin" do
    should_receive(:security_service).and_return(security_service = Object.new)
    should_receive(:current_user).and_return(:user)
    security_service.should_receive(:canViewAdminPage).with(:user).and_return(:is_admin?)
    can_view_admin_page?.should == :is_admin?
  end

  it "should ask security service whether user has agent operate permission" do
    should_receive(:security_service).and_return(security_service = Object.new)
    should_receive(:current_user).and_return(:user)
    security_service.should_receive(:hasOperatePermissionForAgents).with(:user).and_return(:is_admin?)
    has_operate_permission_for_agents?.should == :is_admin?
  end

  it "should find out using system environment whether the plugin framework is enabled" do
    should_receive(:system_environment).twice.and_return(SystemEnvironment.new)

    SystemEnvironment.new.setProperty(com.thoughtworks.go.util.SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED.propertyName(), "Y")
    is_plugins_enabled?.should be true

    SystemEnvironment.new.setProperty(com.thoughtworks.go.util.SystemEnvironment.PLUGIN_FRAMEWORK_ENABLED.propertyName(), "N")
    is_plugins_enabled?.should be false
  end

  it "should honor system property to choose between compressed js or individual files" do
    original_value = SystemEnvironment.new.getPropertyImpl(GoConstants::USE_COMPRESSED_JAVASCRIPT)
    begin
      SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, false.to_s)
      use_compressed_js?.should be_false
      SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, true.to_s)
      use_compressed_js?.should be_true
    ensure
      SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, original_value)
    end
  end

  it "should generate object_id based dom id" do
    obj = Object.new
    id_for(obj).should == "Object_#{obj.object_id}"
  end

  it "should use prefix given for dom id" do
    obj = Object.new
    id_for(obj, "prefix").should == "prefix_#{obj.object_id}"
  end

  it "should yield text if autoRefresh enabled" do
    params.delete(:autoRefresh)
    auto_refresh?.should == true
    params[:autoRefresh] = 'true'
    auto_refresh?.should == true
  end

  it "should not yield text if autoRefresh disabled" do
    params[:autoRefresh] = 'false'
    auto_refresh?.should == false
  end

  describe 'render_json' do

    it "should escape html for json" do
      random_html = <<-end
      <div id="something">
        <p>This should be 'arbitrary' "html"</p>
      </div>
      end
      should_receive(:render).and_return random_html
      json = JSON.parse("{\"result\":" + render_json() + "}")
      json["result"].should == random_html
    end

    it "should include locals by default" do
      should_receive(:render).with(:locals => {:scope => {}}).and_return "foo"
      json = JSON.parse("{\"result\":" + render_json() + "}")
      json["result"].should =="foo"
    end
  end

  describe 'form remote add on' do
    it "should return the before and completed options for a form remote action" do
      expected = %q|<form accept-charset="UTF-8" action="url" method="post" onsubmit="AjaxRefreshers.disableAjax();interesting one; new Ajax.Request('url', {asynchronous:true, evalScripts:true, on202:function(request){do something here}, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();alert(0);}, onSuccess:function(request){whatever}, parameters:Form.serialize(this)}); return false;"><div style="margin:0;padding:0;display:inline"><input name="utf8" type="hidden" value="&#x2713;" /></div>|

      actual = blocking_form_remote_tag(:url => "url", :success => "whatever", 202 => "do something here", :before => "interesting one", :complete => "alert(0);")

      expect(actual).to eq(expected)
    end

    it "should return the before and completed options when not defined" do
      expected = %q|<form accept-charset="UTF-8" action="url" method="post" onsubmit="AjaxRefreshers.disableAjax();; new Ajax.Request('url', {asynchronous:true, evalScripts:true, on202:function(request){do something here}, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){whatever}, parameters:Form.serialize(this)}); return false;"><div style="margin:0;padding:0;display:inline"><input name="utf8" type="hidden" value="&#x2713;" /></div>|

      actual = blocking_form_remote_tag(:url => "url", :success => "whatever", 202 => "do something here")

      expect(actual).to eq(expected)
    end

    it "should resolve URL for AJAX Request URL" do
      expected = %q|<form accept-charset="UTF-8" action="/pipelines/show" method="post" onsubmit="AjaxRefreshers.disableAjax();; new Ajax.Request('/pipelines/show', {asynchronous:true, evalScripts:true, on202:function(request){do something here}, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){whatever}, parameters:Form.serialize(this)}); return false;"><div style="margin:0;padding:0;display:inline"><input name="utf8" type="hidden" value="&#x2713;" /></div>|

      actual = blocking_form_remote_tag(:url => {:controller => 'pipelines', :action => 'show'}, :success => "whatever", 202 => "do something here")

      expect(actual).to eq(expected)
    end

    it "should append 401 handler to form" do
      expected = %q|<form accept-charset="UTF-8" action="url" method="post" onsubmit="AjaxRefreshers.disableAjax();; new Ajax.Request('url', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, parameters:Form.serialize(this)}); return false;"><div style="margin:0;padding:0;display:inline"><input name="utf8" type="hidden" value="&#x2713;" /></div>|

      actual = blocking_form_remote_tag(:url => "url")

      expect(actual).to eq(expected)
    end

    it "should create a blocking link to a remote location" do
      actual = blocking_link_to_remote_new :name => "&nbsp;",
                                           :url => api_pipeline_action_path(:pipeline_name => "SOME_NAME", :action => 'releaseLock'),
                                           :update => {:failure => "message_pane", :success => 'function(){}'},
                                           :html => {},
                                           :headers => {Confirm: 'true'},
                                           :before => "spinny('unlock');"

      exp = %q|<a href="#"  onclick="AjaxRefreshers.disableAjax();spinny('unlock');; new Ajax.Updater({success:'function(){}',failure:'message_pane'}, '/api/pipelines/SOME_NAME/releaseLock', {asynchronous:true, evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, requestHeaders:{'Confirm':'true'}}); return false;">&nbsp;</a>|
      expect(actual).to eq(exp)
    end

    it "should create a blocking link to a remote location with extra HTML provided" do
      actual = blocking_link_to_remote_new :name => "&nbsp;",
                                           :url => api_pipeline_action_path(:pipeline_name => "SOME_NAME", :action => 'releaseLock'),
                                           :headers => {Confirm: 'true'},
                                           :update => {:failure => "message_pane", :success => 'function(){}'},
                                           :html => {:class => "ABC", :title => "TITLE", :id => "SOME-ID" },
                                           :before => "spinny('unlock');"

      exp = %q|<a href="#"  class="ABC" id="SOME-ID" title="TITLE" onclick="AjaxRefreshers.disableAjax();spinny('unlock');; new Ajax.Updater({success:'function(){}',failure:'message_pane'}, '/api/pipelines/SOME_NAME/releaseLock', {asynchronous:true, evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, requestHeaders:{'Confirm':'true'}}); return false;">&nbsp;</a>|
      expect(actual).to eq(exp)
    end
  end

  describe 'submit button' do
    before :each do
      should_receive(:system_environment).and_return(env = double('sys_env'))
      env.should_receive(:isServerActive).and_return(true)
    end

    it "should have class 'image' and type 'submit' for image button" do
      submit_button("name", :type => 'image', :id=> 'id', :class=> "class", :onclick => "onclick", :disabled => "true").should ==
              "<button class=\"class image submit disabled\" disabled=\"disabled\" id=\"id\" onclick=\"onclick\" title=\"name\" type=\"submit\" value=\"name\">" +
                      "<span title=\"name\"> </span>" +
                      "</button>"
    end

    it "should have class 'select' and image for 'select type' button" do
      submit_button("name", :type => 'select', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick").should == "<button class=\"class select submit button\" id=\"id\" name=\"name\" onclick=\"onclick\" type=\"button\" value=\"name\">" +
              "<span>" +
              "NAME<img src=\"/images/g9/button_select_icon.png\" />" +
              "</span>" +
              "</button>"
    end

    it "should not have image or class 'select' for a type 'button'" do
      submit_button("name", :type => 'button', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick").should == "<button class=\"class submit button\" id=\"id\" name=\"name\" onclick=\"onclick\" type=\"button\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>"
    end

    it "should respect disabled flag for type 'select'" do
      submit_button("name", :type => 'select', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick", :disabled => true).should == "<button class=\"class select submit button disabled\" disabled=\"disabled\" id=\"id\" name=\"name\" onclick=\"onclick\" type=\"button\" value=\"name\">" +
              "<span>" +
              "NAME<img src=\"/images/g9/button_select_icon.png\" />" +
              "</span>" +
              "</button>"
    end

    it "should respect disabled flag for type 'button'" do
      submit_button("name", :type => 'button', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick", :disabled => true).should == "<button class=\"class submit button disabled\" disabled=\"disabled\" id=\"id\" name=\"name\" onclick=\"onclick\" type=\"button\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>"
    end

    it "should accept either symbol or string as option keys" do
      submit_button("name", 'type' => 'button', 'id'=> 'id', 'name' => "name", 'class' => "class", 'onclick' => "onclick", 'disabled' => true).should == "<button class=\"class submit button disabled\" disabled=\"disabled\" id=\"id\" name=\"name\" onclick=\"onclick\" type=\"button\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>"
    end

    it "should not generate blank attribute value for enabled button" do
      submit_button("name").should == "<button class=\"submit\" type=\"submit\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>"
    end

    it "should not generate blank attribute value for disabled button" do
      submit_button("name", :disabled => true).should == "<button class=\"submit disabled\" disabled=\"disabled\" type=\"submit\" value=\"name\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>"
    end

    it "should add onclick handler with the passed in onclick_lambda" do
      submit_button("name", :onclick_lambda => 'pavan_likes_this').should =~ /.*?function\(evt\) \{ pavan_likes_this\(evt\).*/
    end
  end

  describe 'disable submit button' do
    before :each do
      should_receive(:system_environment).and_return(env = double('sys_env'))
      env.should_receive(:isServerActive).and_return(false)
    end

    it "should make submit button disabled if server is in passive state" do
      submit_button("name").should == "<button class=\"submit disabled\" disabled=\"disabled\" type=\"submit\" value=\"name\">" +
          "<span>" +
          "NAME" +
          "</span>" +
          "</button>"
    end
  end

  it "should convert urls to https" do
    should_receive(:system_environment).and_return(env = double('sys_env'))
    env.should_receive(:getSslServerPort).and_return(8154)
    make_https("http://user:loser@google.com/hello?world=foo").should == "https://user:loser@google.com:8154/hello?world=foo"
  end

  it "should identify flash messages as session[:notice]" do
    allow(self).to receive(:session).and_return({})

    session_has(:notice).should be_false

    allow(self).to receive(:flash).and_return({:error => "i errored"})
    session_has(:notice).should be_true

    allow(self).to receive(:flash).and_return({:notice => "some notice"})
    session_has(:notice).should be_true

    allow(self).to receive(:flash).and_return({:success => "is success"})
    session_has(:notice).should be_true

    session_has(:foo).should be_false

    allow(self).to receive(:session).and_return({:foo => "foo"})
    session_has(:foo).should be_true
  end

  it "should return FlashMessageModel from flash[key]='string'" do
    service = double("flash_message_service")
    service.should_receive(:get).with("quux").and_return("bang")
    allow(self).to receive(:flash_message_service).and_return(service)
    load_flash_message(:quux).should == "bang"

    service = double("flash_message_service")
    service.stub(:get).with(anything).and_return(nil)
    allow(self).to receive(:flash_message_service).and_return(service)
    allow(self).to receive(:session).and_return(session = {:foo => "bar"})
    load_flash_message(:foo).should == "bar"
    session.should be_empty

    allow(self).to receive(:session).and_return(session = {})
    load_flash_message(:foo).should be_nil
    session.should be_empty

    allow(self).to receive(:flash).and_return(flash = {:error => "i errored"})
    load_flash_message(:notice).should == FlashMessageModel.new("i errored", "error")
    flash[:error].should == "i errored"

    allow(self).to receive(:flash).and_return({:notice => "some notice"})
    load_flash_message(:notice).should == FlashMessageModel.new("some notice", "notice")

    allow(self).to receive(:flash).and_return({:success => "is success"})
    load_flash_message(:notice).should == FlashMessageModel.new("is success", "success")
  end

  it "should create stage_identifier for given locator string" do
    stage_identifier_for_locator("foo/10/bar/2").should == com.thoughtworks.go.domain.StageIdentifier.new("foo", 10, "bar", "2")
  end

  describe "load_from_flash" do
    it "should render multiple flash errors seperated by a period" do
      allow(self).to receive(:flash).and_return({:error => ["I errored", "You errored", "We all errored"]})
      flash_model = load_from_flash
      flash_model.toString().should == "I errored. You errored. We all errored"
    end
  end


  it "should parse selections" do
    params[:selections] = [["foo", "add"], ["bar", "remove"]]
    selections.should == [TriStateSelection.new("foo", "add"), TriStateSelection.new("bar", "remove")]
  end

  it "should return empty selections when no selections submitted" do
    params[:selections] = []
    selections.should == []
    params.delete(:selections)
    selections.should == []
  end

  it "should mark defaultable field by adding a hidden input" do
    assert_dom_equal(register_defaultable_list("foo>bar>baz"), '<input type="hidden" name="default_as_empty_list[]" value="foo&gt;bar&gt;baz"/>')
  end

  describe "unauthorized_access" do

    it "should return true if status is 401" do
      @status = 401
      unauthorized_access.should == true
    end
    it "should return false if status is not true" do
      @status = 200
      unauthorized_access.should == false
    end

  end

  describe "plugins" do
    it "should render the right plugin template" do
      allow(self).to receive(:view_rendering_service).and_return(renderer = double("rendering-service"))
      view_model = TaskViewModel.new(ExecTask.new(), "new", "erb")
      renderer.should_receive(:render).with(view_model, :view => self, "view" => self).and_return("view")
      render_pluggable_template(view_model).should == "view"
    end

    it "should render the right form plugin template" do
      allow(self).to receive(:view_rendering_service).and_return(renderer = double("rendering-service"))
      view_model = TaskViewModel.new(ExecTask.new(), "new", "erb")
      form_name_provider = Object.new()
      renderer.should_receive(:render).with(view_model, {"formNameProvider" => form_name_provider, :view => self, "view" => self, "foo" => "bar" }).and_return("view")
      render_pluggable_form_template(view_model, form_name_provider, "foo" => "bar").should == "view"
    end

    it "should return a NameProvider which uses the object name" do
      form = Class.new do
        def initialize
        # This is where the Rails FormBuilder object stores the name
          @object_name = "foo"
        end

      end.new

      provider = form_name_provider(form)

      provider.name("simple").should == "foo[simple]"
    end
  end

  describe 'is_ie8?' do
    it "should return true when IE8" do
      user_agent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)"
      is_ie8?(user_agent).should == true
    end

    it "should return false when IE9" do
      user_agent = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)"
      is_ie8?(user_agent).should == false
    end

    it "should return false when Firefox" do
      user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:21.0) Gecko/20100101 Firefox/21.0"
      is_ie8?(user_agent).should == false
    end

    it "should return false when Chrome" do
      user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.93 Safari/537.36"
      is_ie8?(user_agent).should == false
    end
  end

  describe :is_user_a_template_admin do
    before :each do
      @security_service = double('security service')
    end

    it 'should check with security service if user is a template admin' do
      should_receive(:security_service).and_return(@security_service)
      allow(self).to receive(:current_user).and_return(:template_admin_user)
      @security_service.should_receive(:isAuthorizedToViewAndEditTemplates).with(:template_admin_user).and_return(true)
      is_user_a_template_admin?.should == true
    end
  end

  describe :link_to_remote_new do
    it 'should return anchor tag with on success function' do
      expected = %q|<a href="#"  class="link_as_button" onclick="new Ajax.Request('url', {asynchronous:true, evalScripts:true, method:'get', onSuccess:function(request){Modalbox.show(alert('hi')}}); return false;">link name</a>|
      actual = link_to_remote_new('link name',{:method=>:get, :url => "url", :success=>"Modalbox.show(alert('hi')"},{:class => "link_as_button"})
      expect(actual).to eq(expected)
    end

    it 'should return anchor tag without optional params' do
      expected = %q|<a href="#"  onclick="new Ajax.Request('url', {asynchronous:true, evalScripts:true, method:'get', onSuccess:function(request){}}); return false;">link name</a>|
      actual = link_to_remote_new('link name',{:method=>:get, :url => "url"})
      expect(actual).to eq(expected)
    end

    it 'should raise exception when link name not provided' do
      begin
        link_to_remote_new(nil,{:method=>:get, :url => "url", :success=>"Modalbox.show(alert('hi')"},{:class => "link_as_button"})
        fail "should have raised exception"
      rescue => e
        expect(e.message).to eq("Expected link name. Didn't find it.")
      end
    end

    it 'should raise exception when method not provided in options' do
      begin
        link_to_remote_new("link name",{ :url => "url", :success=>"Modalbox.show(alert('hi')"},{:class => "link_as_button"})
        fail "should have raised exception"
      rescue => e
        expect(e.message).to eq("Expected key: method. Didn't find it. Found: [:url, :success]")
      end
    end

    it 'should raise exception when method not provided in options' do
      begin
        link_to_remote_new("link name",{:method=> :url, :success=>"Modalbox.show(alert('hi')"},{:class => "link_as_button"})
        fail "should have raised exception"
      rescue => e
        expect(e.message).to eq("Expected key: url. Didn't find it. Found: [:method, :success]")
      end
    end

  end

  describe :form_remote_tag_new do

   it 'should generate form tag with on complete for ajax update' do
     expected = %q|<form accept-charset="UTF-8" action="/admin/users/search" method="post" onsubmit="jQuery('#search_id').addClass('ac_loading'); new Ajax.Updater({success:'search_results_container'}, '/admin/users/search', {asynchronous:true, evalScripts:true, onComplete:function(request){jQuery('#search_id').removeClass('ac_loading');}, parameters:Form.serialize(this)}); return false;"><div style="margin:0;padding:0;display:inline"><input name="utf8" type="hidden" value="&#x2713;" /></div>|
     actual = form_remote_tag_new(
         :url => users_search_path,
         :update => {:success => "search_results_container"},
         :before => "jQuery('#search_id').addClass('ac_loading');",
         :complete => "jQuery('#search_id').removeClass('ac_loading');"
     )
     expect(actual).to eq(expected)
   end

   it 'should generate form tag with on success and failure for ajax update' do
     expected = %q|<form accept-charset="UTF-8" action="/admin/users/create" method="post" onsubmit="new Ajax.Updater({success:'tab-content-of-user-listing'}, '/admin/users/create', {asynchronous:true, evalScripts:true, onFailure:function(request){Util.refresh_child_text('add_error_message', request.responseText, 'error');}, onSuccess:function(request){Modalbox.hide();Util.refresh_child_text('message_pane', 'Added user successfully', 'success');}, parameters:Form.serialize(this)}); return false;"><div style="margin:0;padding:0;display:inline"><input name="utf8" type="hidden" value="&#x2713;" /></div>|
     actual = form_remote_tag_new(
         :url => users_create_path,
         :update => {:success => "tab-content-of-user-listing"},
         :failure => "Util.refresh_child_text('add_error_message', request.responseText, 'error');",
         :success => "Modalbox.hide();Util.refresh_child_text('message_pane', 'Added user successfully', 'success');"
     )
     expect(actual).to eq(expected)
   end
  end

  describe :go_update do
    it 'should fetch the new go release' do
      version_info_service.should_receive(:getGoUpdate).and_return("1.2.3-1")

      expect(go_update).to eq("1.2.3-1")
    end
  end

  describe :check_go_updates? do
    it 'should return true if go version update check is enabled' do
      version_info_service.should_receive(:isGOUpdateCheckEnabled).and_return(true)

      expect(check_go_updates?).to be_true
    end
  end

  it 'should encode cruise-config-md5 before allowing it to be displayed.' do
    allow(self).to receive(:cruise_config_md5).and_return("<foo>")
    config_md5_field.should == '<input id="cruise_config_md5" name="cruise_config_md5" type="hidden" value="&lt;foo&gt;" />'
  end
end
