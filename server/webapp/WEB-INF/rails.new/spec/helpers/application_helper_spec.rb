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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')

describe ApplicationHelper do
  include ApplicationHelper, RailsLocalizer

  it "should generate a label tag with required asterisk" do
    mock_form = mock(:form)
    mock_form.should_receive(:label).with("name", "value<span class='asterisk'>*</span>")
    required_label(mock_form, "name", "value")
  end

  it "should respect anchor classes provided irrespective of tab being current" do
    stub!(:url_for).and_return("/go/quux")
    tab_for("quux", :class => "foo bar", :anchor_class => "skip_dirty_stop").should == "<li id='cruise-header-tab-quux' class=' foo bar'>\n<a class=\"skip_dirty_stop\" href=\"/go/quux\">QUUX</a>\n</li>"
  end

  it "url_for_path should handle default_url_options" do
    stub!(:root_path).and_return("/go/quux?x")
    url = url_for_path("/foo")
    url.should == "/go/quux/foo?x"
  end

  it "url_for_path should handle default_url_options" do
    stub!(:root_path).and_return("/go/quux?x")
    url = url_for_path("/foo?bar=blah")
    url.should == "/go/quux/foo?bar=blah&x"
  end

  it "url_for_login should give the url for login" do
    stub!(:root_path).and_return("/go/quux?x")
    url_for_login.should == "/go/quux/auth/login?x"
  end

  it "url_for_path should handle query params" do
    stub!(:root_path).and_return("/go/quux/?x")
    url = url_for_path("/foo")
    url.should == "/go/quux/foo?x"
  end

  it "url_for_path should handle url without params" do
    stub!(:root_path).and_return("/go/quux")
    url = url_for_path("/foo")
    url.should == "/go/quux/foo"
  end

  it "url_for_path should handle root url with trailing slash and provided sub path with leading slash" do
    stub!(:root_path).and_return("/go/quux/")
    url = url_for_path("/foo")
    url.should == "/go/quux/foo"
  end

  it "should give the server version" do
    version == "N/A"
  end

  it "should generate hidden field for config_md5" do
    stub!(:cruise_config_md5).and_return("foo_bar_baz")
    config_md5_field.should == '<input type="hidden" name="cruise_config_md5" value="foo_bar_baz"/>'
  end

  describe :tab_for do
    before do
      stub!(:url_for).and_return("/go/quux")
      stub!(:root_path).and_return("/go/quux")
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
    SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, false.to_s)
    use_compressed_js?.should be_false
    SystemEnvironment.new.setProperty(GoConstants::USE_COMPRESSED_JAVASCRIPT, true.to_s)
    use_compressed_js?.should be_true
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

    it "return the before and completed options for a form remote action" do
      should_receive(:form_remote_tag).with(:url => "url", :success => "whatever", 202 => "do something here", :before => "AjaxRefreshers.disableAjax();interesting one", 401 => "redirectToLoginPage('/auth/login');", :complete => "AjaxRefreshers.enableAjax();alert(0);")
      blocking_form_remote_tag(:url => "url", :success => "whatever", 202 => "do something here", :before => "interesting one", :complete=> "alert(0);")
    end

    it "return the before and completed options when not defined" do
      should_receive(:form_remote_tag).with(:url => "url", :success => "whatever", 202 => "do something here", 401 => "redirectToLoginPage('/auth/login');", :before => "AjaxRefreshers.disableAjax();", :complete => "AjaxRefreshers.enableAjax();")
      blocking_form_remote_tag(:url => "url", :success => "whatever", 202 => "do something here")
    end

    it "should append 401 handler to form" do
      should_receive(:form_remote_tag).with(:url => "url", 401 => "redirectToLoginPage('/auth/login');", :before => "AjaxRefreshers.disableAjax();", :complete => "AjaxRefreshers.enableAjax();")
      blocking_form_remote_tag(:url => "url")
    end

    it "should append 401 handler to link" do
      should_receive(:link_to_remote).with("lkb", {:url => "url", :success => "whatever", 202 => "do something here", 401 => "redirectToLoginPage('/auth/login');", :before => "AjaxRefreshers.disableAjax();", :complete => "AjaxRefreshers.enableAjax();"}, nil)
      blocking_link_to_remote("lkb", :url => "url", :success => "whatever", 202 => "do something here")
    end

    it "return the before and completed options for a link remote action" do
      should_receive(:link_to_remote).with("doDDamma", {:url => "url", :success => "whatever", 202 => "do something here", 401 => "redirectToLoginPage('/auth/login');", :before => "AjaxRefreshers.disableAjax();interesting one", :complete => "AjaxRefreshers.enableAjax();alert(0);"}, nil)
      blocking_link_to_remote("doDDamma", :url => "url", :success => "whatever", 202 => "do something here", :before => "interesting one", :complete=> "alert(0);")
    end

    it "return the before and completed options when not defined for a link remote action" do
      should_receive(:link_to_remote).with("lkb", {:url => "url", :success => "whatever", 202 => "do something here", 401 => "redirectToLoginPage('/auth/login');", :before => "AjaxRefreshers.disableAjax();", :complete => "AjaxRefreshers.enableAjax();"}, nil)
      blocking_link_to_remote("lkb", :url => "url", :success => "whatever", 202 => "do something here")
    end
  end

  describe 'submit button' do

    it "should have class 'image' and type 'submit' for image button" do
      submit_button("name", :type => 'image', :id=> 'id', :class=> "class", :onclick => "onclick", :disabled => "true").should ==
              "<button class=\"class image submit disabled\" disabled=\"disabled\" id=\"id\" onclick=\"onclick\" title=\"name\" type=\"submit\" value=\"name\">" +
                      "<span title=\"name\"> </span>" +
                      "</button>"
    end

    it "should have class 'select' and image for 'select type' button" do
      submit_button("name", :type => 'select', :id=> 'id', :name => "name", :class=> "class", :onclick => "onclick").should == "<button class=\"class select submit button\" id=\"id\" name=\"name\" onclick=\"onclick\" type=\"button\" value=\"name\">" +
              "<span>" +
              "NAME<img src=\"/images/g9/button_select_icon.png?N/A\" />" +
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
              "NAME<img src=\"/images/g9/button_select_icon.png?N/A\" />" +
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

  it "should convert urls to https" do
    should_receive(:system_environment).and_return(env = mock('sys_env'))
    env.should_receive(:getSslServerPort).and_return(8154)
    make_https("http://user:loser@google.com/hello?world=foo").should == "https://user:loser@google.com:8154/hello?world=foo"
  end

  it "should identify flash messages as session[:notice]" do
    stub!(:session).and_return({})

    session_has(:notice).should be_false

    stub!(:flash).and_return({:error => "i errored"})
    session_has(:notice).should be_true

    stub!(:flash).and_return({:notice => "some notice"})
    session_has(:notice).should be_true

    stub!(:flash).and_return({:success => "is success"})
    session_has(:notice).should be_true

    session_has(:foo).should be_false

    stub!(:session).and_return({:foo => "foo"})
    session_has(:foo).should be_true
  end

  it "should return FlashMessageModel from flash[key]='string'" do
    service = stub("flash_message_service", :get => nil)
    stub!(:flash_message_service).and_return(service)

    service.should_receive("get").with("quux").and_return("bang")
    load_flash_message(:quux).should == "bang"

    stub!(:session).and_return(session = {:foo => "bar"})
    load_flash_message(:foo).should == "bar"
    session.should be_empty

    stub!(:session).and_return(session = {})
    load_flash_message(:foo).should be_nil
    session.should be_empty

    stub!(:flash).and_return(flash = {:error => "i errored"})
    load_flash_message(:notice).should == FlashMessageModel.new("i errored", "error")
    flash[:error].should == "i errored"

    stub!(:flash).and_return({:notice => "some notice"})
    load_flash_message(:notice).should == FlashMessageModel.new("some notice", "notice")

    stub!(:flash).and_return({:success => "is success"})
    load_flash_message(:notice).should == FlashMessageModel.new("is success", "success")
  end

  it "should create stage_identifier for given locator string" do
    stage_identifier_for_locator("foo/10/bar/2").should == com.thoughtworks.go.domain.StageIdentifier.new("foo", 10, "bar", "2")
  end

  describe "load_from_flash" do
    it "should render multiple flash errors seperated by a period" do
      stub!(:flash).and_return({:error => ["I errored", "You errored", "We all errored"]})
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
      stub!(:view_rendering_service).and_return(renderer = mock("rendering-service"))
      view_model = TaskViewModel.new(ExecTask.new(), "new", "erb")
      renderer.should_receive(:render).with(view_model, :view => self, "view" => self).and_return("view")
      render_pluggable_template(view_model).should == "view"
    end

    it "should render the right form plugin template" do
      stub!(:view_rendering_service).and_return(renderer = mock("rendering-service"))
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
      @security_service = mock('security service')
    end
    it 'should check with security service if user is a template admin' do
      should_receive(:security_service).and_return(@security_service)
      stub!(:current_user).and_return(:template_admin_user)
      @security_service.should_receive(:isAuthorizedToViewAndEditTemplates).with(:template_admin_user).and_return(true)
      is_user_a_template_admin?.should == true
    end
  end
end
