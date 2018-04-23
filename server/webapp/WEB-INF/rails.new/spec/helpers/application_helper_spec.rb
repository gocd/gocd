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

require 'rails_helper'

describe ApplicationHelper do
  include RailsLocalizer
  include ApplicationHelper

  it "should generate a label tag with required asterisk" do
    mock_form = double(:form)
    expect(mock_form).to receive(:label).with("name", "value<span class='asterisk'>*</span>")
    required_label(mock_form, "name", "value")
  end

  it "should respect anchor classes provided irrespective of tab being current" do
    allow(self).to receive(:url_for).and_return("/go/quux")
    expect(tab_for("quux", :class => "foo bar", :anchor_class => "skip_dirty_stop")).to eq("<li id='cruise-header-tab-quux' class=' foo bar'>\n<a class=\"skip_dirty_stop\" href=\"/go/quux\">QUUX</a>\n</li>")
  end

  describe "url_for_path" do

    before :each do
      main_app = double('main app')
      allow(controller).to receive(:main_app).and_return(main_app)
      allow(main_app).to receive(:root_path).and_return("/go/quux?x")
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
      allow(main_app).to receive(:root_path).and_return("/go/quux")
      url = url_for_path("/foo")
      expect(url).to eq("/go/quux/foo")
    end

    it "should handle root url with trailing slash and provided sub path with leading slash" do
      allow(main_app).to receive(:root_path).and_return("/go/quux/")
      url = url_for_path("/foo")
      expect(url).to eq("/go/quux/foo")
    end
  end

  describe "url_for_login" do
    before :each do
      main_app = double('main app')
      allow(controller).to receive(:main_app).and_return(main_app)
      allow(main_app).to receive(:root_path).and_return("/go/quux?x")
    end

    it "should give the url for login" do
      expect(url_for_login).to eq("/go/quux/auth/login?x")
    end
  end

  it "should give the server version" do
    version == "N/A"
  end

  it "should generate hidden field for config_md5" do
    allow(self).to receive(:cruise_config_md5).and_return("foo_bar_baz")
    expect(config_md5_field).to eq("<input type=\"hidden\" name=\"cruise_config_md5\" id=\"cruise_config_md5\" value=\"foo_bar_baz\" />")
  end

  describe "tab_for" do
    def root_path
      "/go/quux"
    end
    def url_for(opts=nil)
      "/go/quux"
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
          expect(tab_for("quux")).to eq("<li id='cruise-header-tab-quux' class='current '>\nlink_to_quux\n</li>")
          expect(tab_for("baz")).to eq("<li id='cruise-header-tab-baz' class=' '>\nlink_to_baz\n</li>")
        end

        it "should respect classes provided irrespective of tab being current" do
          expect(tab_for("quux", :class => "foo bar")).to eq("<li id='cruise-header-tab-quux' class='current foo bar'>\nlink_to_quux\n</li>")
          expect(tab_for("baz",  :class => "foo bar")).to eq("<li id='cruise-header-tab-baz' class=' foo bar'>\nlink_to_baz\n</li>")
        end
      end

      it "should honor current tab override" do
        @current_tab_name = 'baz'
        expect(tab_for("baz")).to eq("<li id='cruise-header-tab-baz' class='current '>\nlink_to_baz\n</li>")
      end
    end

    it "should respect option :link" do
      expect(tab_for("quux", :link => :disabled)).to eq("<li id='cruise-header-tab-quux' class=' '>\n<span>QUUX</span>\n</li>")
    end

    it "should respect option :target" do
      main_app = double('main app')
      allow(controller).to receive(:main_app).and_return(main_app)
      allow(main_app).to receive(:root_path).and_return("/go/quux")
      should_receive(:link_to).with("QUUX", "/go/quux/quux", { :target => 'foo', :class => "" }).and_return("link_to_quux")
      expect(tab_for("quux", :target => 'foo')).to eq("<li id='cruise-header-tab-quux' class=' '>\nlink_to_quux\n</li>")
    end

    it "should substitute ' ' with - for generating css class name" do
      expect(tab_for("foo bar", :link => :disabled)).to eq("<li id='cruise-header-tab-foo-bar' class=' '>\n<span>FOO BAR</span>\n</li>")
    end

    it "should honor url if provided with one" do
      should_receive(:url_for_path).with("foo/bar/baz").and_return(quux_url = "http://foo.bar:8153/go/foo/bar/baz")
      expect(tab_for("foo bar", :url => 'foo/bar/baz')).to eq("<li id='cruise-header-tab-foo-bar' class=' '>\n<a class=\"\" href=\"/go/quux\">FOO BAR</a>\n</li>")
    end
  end

  it "should understand if mycruise link tab is supposed to be enabled" do
    should_receive(:go_config_service).and_return(go_config_service = double("go_config_service"))
    expect(go_config_service).to receive(:isSecurityEnabled).and_return(true)
    expect(mycruise_available?).to eq(true)
  end

  it "should ask security service whether user is an admin" do
    should_receive(:security_service).and_return(security_service = double("security_service"))
    should_receive(:current_user).and_return(:user)
    expect(security_service).to receive(:canViewAdminPage).with(:user).and_return(:is_admin?)
    expect(can_view_admin_page?).to eq(:is_admin?)
  end

  it "should ask security service whether user has agent operate permission" do
    should_receive(:security_service).and_return(security_service = double("security_service"))
    should_receive(:current_user).and_return(:user)
    expect(security_service).to receive(:hasOperatePermissionForAgents).with(:user).and_return(:is_admin?)
    expect(has_operate_permission_for_agents?).to eq(:is_admin?)
  end

  it "should honor system property to choose between compressed js or individual files" do
    original_value = SystemEnvironment.new.getPropertyImpl(GoConstants::USE_COMPRESSED_JAVASCRIPT)
    begin
      SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, false.to_s)
      expect(use_compressed_js?).to be_falsey
      SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, true.to_s)
      expect(use_compressed_js?).to be_truthy
    ensure
      SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, original_value)
    end
  end

  it "should generate object_id based dom id" do
    obj = Object.new
    expect(id_for(obj)).to eq("Object_#{obj.object_id}")
  end

  it "should use prefix given for dom id" do
    obj = Object.new
    expect(id_for(obj, "prefix")).to eq("prefix_#{obj.object_id}")
  end

  it "should yield text if autoRefresh enabled" do
    params.delete(:autoRefresh)
    expect(auto_refresh?).to eq(true)
    params[:autoRefresh] = 'true'
    expect(auto_refresh?).to eq(true)
  end

  it "should not yield text if autoRefresh disabled" do
    params[:autoRefresh] = 'false'
    expect(auto_refresh?).to eq(false)
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
      expect(json["result"]).to eq(random_html)
    end

    it "should include locals by default" do
      should_receive(:render).with(:locals => {:scope => {}}).and_return "foo"
      json = JSON.parse("{\"result\":" + render_json() + "}")
      expect(json["result"]).to eq("foo")
    end
  end

  describe 'form remote add on' do
    it "should return the before and completed options for a form remote action" do
      expected = %q|<form onsubmit="AjaxRefreshers.disableAjax();interesting one; new Ajax.Request('url', {asynchronous:true, evalScripts:true, on202:function(request){do something here}, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();alert(0);}, onSuccess:function(request){whatever}, parameters:Form.serialize(this)}); return false;" action="url" accept-charset="UTF-8" method="post"><input name="utf8" type="hidden" value="&#x2713;" />|

      actual = blocking_form_remote_tag(:url => "url", :success => "whatever", 202 => "do something here", :before => "interesting one", :complete => "alert(0);")


      expect(actual).to eq(expected)
    end

    it "should return the before and completed options when not defined" do
      expected = %q|<form onsubmit="AjaxRefreshers.disableAjax();; new Ajax.Request('url', {asynchronous:true, evalScripts:true, on202:function(request){do something here}, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){whatever}, parameters:Form.serialize(this)}); return false;" action="url" accept-charset="UTF-8" method="post"><input name="utf8" type="hidden" value="&#x2713;" />|

      actual = blocking_form_remote_tag(:url => "url", :success => "whatever", 202 => "do something here")


      expect(actual).to eq(expected)
    end

    it "should resolve URL for AJAX Request URL" do
      expected = %q|<form onsubmit="AjaxRefreshers.disableAjax();; new Ajax.Request('/pipelines/show', {asynchronous:true, evalScripts:true, on202:function(request){do something here}, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, onSuccess:function(request){whatever}, parameters:Form.serialize(this)}); return false;" action="/pipelines/show" accept-charset="UTF-8" method="post"><input name="utf8" type="hidden" value="&#x2713;" />|

      actual = blocking_form_remote_tag(:url => {:controller => 'pipelines', :action => 'show'}, :success => "whatever", 202 => "do something here")


      expect(actual).to eq(expected)
    end

    it "should append 401 handler to form" do
      expected = %q|<form onsubmit="AjaxRefreshers.disableAjax();; new Ajax.Request('url', {asynchronous:true, evalScripts:true, on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, parameters:Form.serialize(this)}); return false;" action="url" accept-charset="UTF-8" method="post"><input name="utf8" type="hidden" value="&#x2713;" />|

      actual = blocking_form_remote_tag(:url => "url")


      expect(actual).to eq(expected)
    end

    it "should create a blocking link to a remote location" do
      actual = blocking_link_to_remote_new :name => "&nbsp;",
                                           :url => api_pipeline_releaseLock_path(:pipeline_name => "SOME_NAME"),
                                           :update => {:failure => "message_pane", :success => 'function(){}'},
                                           :html => {},
                                           :headers => {Confirm: 'true'},
                                           :before => "spinny('unlock');"

      exp = %q|<a href="#"  onclick="AjaxRefreshers.disableAjax();spinny('unlock');; new Ajax.Updater({success:'function(){}',failure:'message_pane'}, '/api/pipelines/SOME_NAME/releaseLock', {asynchronous:true, evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, requestHeaders:{'Confirm':'true'}}); return false;">&nbsp;</a>|
      expect(actual).to eq(exp)
    end

    it "should create a blocking link to a remote location with extra HTML provided" do
      actual = blocking_link_to_remote_new :name => "&nbsp;",
                                           :url => api_pipeline_releaseLock_path(:pipeline_name => "SOME_NAME"),
                                           :headers => {Confirm: 'true'},
                                           :update => {:failure => "message_pane", :success => 'function(){}'},
                                           :html => {:class => "ABC", :title => "TITLE", :id => "SOME-ID" },
                                           :before => "spinny('unlock');"

      exp = %q|<a href="#"  class="ABC" title="TITLE" id="SOME-ID" onclick="AjaxRefreshers.disableAjax();spinny('unlock');; new Ajax.Updater({success:'function(){}',failure:'message_pane'}, '/api/pipelines/SOME_NAME/releaseLock', {asynchronous:true, evalScripts:true, method:'post', on401:function(request){redirectToLoginPage('/auth/login');}, onComplete:function(request){AjaxRefreshers.enableAjax();}, requestHeaders:{'Confirm':'true'}}); return false;">&nbsp;</a>|
      expect(actual).to eq(exp)
    end
  end

  describe 'submit button' do
    before :each do
      should_receive(:system_environment).and_return(env = double('sys_env'))
      expect(env).to receive(:isServerActive).at_most(:twice).and_return(true)
    end

    it "should have class 'image' and type 'submit' for image button" do
      expect(submit_button("name", :type => 'image', :id=> 'id', :class=> "class", :onclick => "onclick", :disabled => "true")).to eq(
              %q{<button type="submit" id="id" class="class image submit disabled" onclick="onclick" disabled="disabled" value="name" title="name">} +
                      "<span title=\"name\"> </span>" +
                      "</button>"
      )
    end

    it "should have class 'select' and image for 'select type' button" do
      expect(submit_button("name", :type => 'select', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick")).to eq(%q{<button type="button" id="id" name="name" class="class select submit button" onclick="onclick" value="name">} +
              "<span>" +
              "NAME<img src=\"/images/g9/button_select_icon.png\" />" +
              "</span>" +
              "</button>")
    end

    it "should not have image or class 'select' for a type 'button'" do
      expect(submit_button("name", :type => 'button', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick")).to eq(%q{<button type="button" id="id" name="name" class="class submit button" onclick="onclick" value="name">} +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end

    it "should respect disabled flag for type 'select'" do
      expect(submit_button("name", :type => 'select', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick", :disabled => true)).to eq(%q|<button type="button" id="id" name="name" class="class select submit button disabled" onclick="onclick" disabled="disabled" value="name"><span>NAME<img src="/images/g9/button_select_icon.png" /></span></button>|)
    end

    it "should respect disabled flag for type 'button'" do
      expect(submit_button("name", :type => 'button', :id => 'id', :name => "name", :class => "class", :onclick => "onclick", :disabled => true)).to eq(%q|<button type="button" id="id" name="name" class="class submit button disabled" onclick="onclick" disabled="disabled" value="name"><span>NAME</span></button>|)
    end

    it "should accept either symbol or string as option keys" do
      expect(submit_button("name", 'type' => 'button', 'id' => 'id', 'name' => "name", 'class' => "class", 'onclick' => "onclick", 'disabled' => true)).to eq(%q|<button type="button" id="id" name="name" class="class submit button disabled" onclick="onclick" disabled="disabled" value="name"><span>NAME</span></button>|)
    end

    it "should not generate blank attribute value for enabled button" do
      expect(submit_button("name")).to eq("<button type=\"submit\" value=\"name\" class=\"submit\">" +
              "<span>" +
              "NAME" +
              "</span>" +
              "</button>")
    end

    it "should not generate blank attribute value for disabled button" do
      expect(submit_button("name", :disabled => true)).to eq(%q|<button type="submit" disabled="disabled" value="name" class="submit disabled"><span>NAME</span></button>|)
    end

    it "should add onclick handler with the passed in onclick_lambda" do
      expect(submit_button("name", :onclick_lambda => 'pavan_likes_this')).to match(/.*?function\(evt\) \{ pavan_likes_this\(evt\).*/)
    end
  end

  describe 'disable submit button' do
    before :each do
      should_receive(:system_environment).and_return(env = double('sys_env'))
      expect(env).to receive(:isServerActive).and_return(false)
    end

    it "should make submit button disabled if server is in passive state" do
      expect(submit_button("name")).to eq(%q|<button type="submit" disabled="disabled" value="name" class="submit disabled"><span>NAME</span></button>|)
    end
  end

  it "should identify flash messages as session[:notice]" do
    allow(self).to receive(:session).and_return({})

    expect(session_has(:notice)).to be_falsey

    allow(self).to receive(:flash).and_return({:error => "i errored"})
    expect(session_has(:notice)).to be_truthy

    allow(self).to receive(:flash).and_return({:notice => "some notice"})
    expect(session_has(:notice)).to be_truthy

    allow(self).to receive(:flash).and_return({:success => "is success"})
    expect(session_has(:notice)).to be_truthy

    expect(session_has(:foo)).to be_falsey

    allow(self).to receive(:session).and_return({:foo => "foo"})
    expect(session_has(:foo)).to be_truthy
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

  it "should create stage_identifier for given locator string" do
    expect(stage_identifier_for_locator("foo/10/bar/2")).to eq(com.thoughtworks.go.domain.StageIdentifier.new("foo", 10, "bar", "2"))
  end

  describe "load_from_flash" do
    it "should render multiple flash errors seperated by a period" do
      allow(self).to receive(:flash).and_return({:error => ["I errored", "You errored", "We all errored"]})
      flash_model = load_from_flash
      expect(flash_model.toString()).to eq("I errored. You errored. We all errored")
    end
  end


  it "should parse selections" do
    params[:selections] = [["foo", "add"], ["bar", "remove"]]
    expect(selections).to eq([TriStateSelection.new("foo", "add"), TriStateSelection.new("bar", "remove")])
  end

  it "should return empty selections when no selections submitted" do
    params[:selections] = []
    expect(selections).to eq([])
    params.delete(:selections)
    expect(selections).to eq([])
  end

  it "should mark defaultable field by adding a hidden input" do
    assert_dom_equal(register_defaultable_list("foo>bar>baz"), '<input type="hidden" name="default_as_empty_list[]" value="foo&gt;bar&gt;baz"/>')
  end

  describe "unauthorized_access" do

    it "should return true if status is 401" do
      @status = 401
      expect(unauthorized_access).to eq(true)
    end
    it "should return false if status is not true" do
      @status = 200
      expect(unauthorized_access).to eq(false)
    end

  end

  describe "plugins" do
    it "should render the right plugin template" do
      view_model = TaskViewModel.new(ExecTask.new(), "new")
      expect(self).to receive(:render).with(file: view_model.getTemplatePath(), locals: {})
      render_pluggable_template(view_model)
    end

    it "should render the right form plugin template" do
      view_model = TaskViewModel.new(ExecTask.new(), "new")
      form_name_provider = Object.new()
      expect(self).to receive(:render).with(file: view_model.getTemplatePath(), locals: {"formNameProvider" => form_name_provider, "foo" => "bar"})
      render_pluggable_form_template(view_model, form_name_provider, "foo" => "bar")
    end

    it "should return a NameProvider which uses the object name" do
      form = Class.new do
        def initialize
        # This is where the Rails FormBuilder object stores the name
          @object_name = "foo"
        end

      end.new

      provider = form_name_provider(form)

      expect(provider.name("simple")).to eq("foo[simple]")
    end
  end

  describe 'is_ie8?' do
    it "should return true when IE8" do
      user_agent = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)"
      expect(is_ie8?(user_agent)).to eq(true)
    end

    it "should return false when IE9" do
      user_agent = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)"
      expect(is_ie8?(user_agent)).to eq(false)
    end

    it "should return false when Firefox" do
      user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:21.0) Gecko/20100101 Firefox/21.0"
      expect(is_ie8?(user_agent)).to eq(false)
    end

    it "should return false when Chrome" do
      user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/27.0.1453.93 Safari/537.36"
      expect(is_ie8?(user_agent)).to eq(false)
    end
  end

  describe "is_user_a_template_admin" do
    before :each do
      @security_service = double('security service')
    end

    it 'should check with security service if user is a template admin' do
      should_receive(:security_service).and_return(@security_service)
      allow(self).to receive(:current_user).and_return(:template_admin_user)
      expect(@security_service).to receive(:isAuthorizedToViewAndEditTemplates).with(:template_admin_user).and_return(true)
      expect(is_user_a_template_admin?).to eq(true)
    end
  end

  describe "link_to_remote_new" do
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

  describe "form_remote_tag_new" do

   it 'should generate form tag with on complete for ajax update' do
     expected = %q|<form onsubmit="jQuery('#search_id').addClass('ac_loading'); new Ajax.Updater({success:'search_results_container'}, '/admin/users/search', {asynchronous:true, evalScripts:true, onComplete:function(request){jQuery('#search_id').removeClass('ac_loading');}, parameters:Form.serialize(this)}); return false;" action="/admin/users/search" accept-charset="UTF-8" method="post"><input name="utf8" type="hidden" value="&#x2713;" />|
     actual = form_remote_tag_new(
         :url => users_search_path,
         :update => {:success => "search_results_container"},
         :before => "jQuery('#search_id').addClass('ac_loading');",
         :complete => "jQuery('#search_id').removeClass('ac_loading');"
     )

     expect(actual).to eq(expected)
   end

   it 'should generate form tag with on success and failure for ajax update' do
     expected = %q|<form onsubmit="new Ajax.Updater({success:'tab-content-of-user-listing'}, '/admin/users/create', {asynchronous:true, evalScripts:true, onFailure:function(request){Util.refresh_child_text('add_error_message', request.responseText, 'error');}, onSuccess:function(request){Modalbox.hide();Util.refresh_child_text('message_pane', 'Added user successfully', 'success');}, parameters:Form.serialize(this)}); return false;" action="/admin/users/create" accept-charset="UTF-8" method="post"><input name="utf8" type="hidden" value="&#x2713;" />|
     actual = form_remote_tag_new(
         :url => users_create_path,
         :update => {:success => "tab-content-of-user-listing"},
         :failure => "Util.refresh_child_text('add_error_message', request.responseText, 'error');",
         :success => "Modalbox.hide();Util.refresh_child_text('message_pane', 'Added user successfully', 'success');"
     )

     expect(actual).to eq(expected)
   end
  end

  describe "go_update" do
    it 'should fetch the new go release' do
      expect(version_info_service).to receive(:getGoUpdate).and_return("1.2.3-1")

      expect(go_update).to eq("1.2.3-1")
    end
  end

  describe "check_go_updates?" do
    it 'should return true if go version update check is enabled' do
      expect(version_info_service).to receive(:isGOUpdateCheckEnabled).and_return(true)

      expect(check_go_updates?).to be_truthy
    end
  end

  it 'should encode cruise-config-md5 before allowing it to be displayed.' do
    allow(self).to receive(:cruise_config_md5).and_return("<foo>")
    expect(config_md5_field).to eq("<input type=\"hidden\" name=\"cruise_config_md5\" id=\"cruise_config_md5\" value=\"&lt;foo&gt;\" />")
  end
end
