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

describe AdminController do
  include ConfigSaveStubbing

  before do
    @go_config_service = stub_service(:go_config_service)
    mother = GoConfigMother.new()
    @cruise_config = mother.cruiseConfigWithPipelineUsingTwoMaterials()
    @user = current_user
  end

  after(:each) do
    controller.instance_variable_set(:@should_not_render_layout, false)
  end

  class UpdateCommand
    include com.thoughtworks.go.config.update.UpdateConfigFromUI

    def checkPermission(cruiseConfig, result)
      true
    end

    def node(cruiseConfig)
      cruiseConfig.pipelineConfigByName(CaseInsensitiveString.new("pipeline1"))
    end

    def upatedNode(cruiseConfig)
      node(cruiseConfig)
    end

    def update(pipeline)
      pipeline.setLabelTemplate("foo-${COUNT}")
    end

    def subject(pipeline)
      pipeline
    end

    def updatedSubject(pipeline)
      subject(pipeline)
    end
  end

  it "should capture cruise_config, node and subject for an update operation" do
    stub_save_for_success
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.",proc {}) {}

    controller.instance_variable_get('@cruise_config').should == @cruise_config
    controller.instance_variable_get('@node').should == @cruise_config.pipelineConfigByName(CaseInsensitiveString.new('pipeline1'))
    controller.instance_variable_get('@subject').should == @cruise_config.pipelineConfigByName(CaseInsensitiveString.new('pipeline1'))
  end

  it "should capture config before and after update" do
    stub_save_for_success
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", proc {}) {}

    controller.instance_variable_get('@config_after').pipelineConfigByName(CaseInsensitiveString.new("pipeline1")).getLabelTemplate().should == "foo-${COUNT}"
  end

  describe "assert_load" do
    it "should assign variable" do
      controller.send(:assert_load, :junk, "junk_value").should be_true
      controller.instance_variable_get('@junk').should == "junk_value"
    end

    it "should error out on null assignment" do
      controller.should_receive(:action_has_layout?).and_return(true)
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
      controller.send(:assert_load, :junk, nil).should be_false
      controller.instance_variable_get('@message').should == "Error occurred while trying to complete your request."
    end

    it "should not render error page layout when action doesn't have one" do
      controller.should_receive(:action_has_layout?).and_return(false)
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => nil, :status => 404})
      controller.send(:assert_load, :junk, nil).should be_false
      controller.instance_variable_get('@message').should == "Error occurred while trying to complete your request."
    end

    it "should allow caller to use custom error message and status" do
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 409})
      controller.send(:assert_load, :junk, nil, "callers custom message", 409).should be_false
      controller.instance_variable_get('@message').should == "callers custom message"
    end

    it "should load the result of evaluation" do
      controller.send(:assert_load_eval, :junk) do
        "junk_value"
      end.should be_true
      controller.instance_variable_get('@junk').should == "junk_value"
    end

    it "should render error when result of evaluation is null" do
      controller.should_receive(:action_has_layout?).and_return(true)
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
      controller.send(:assert_load_eval, :junk) do
        nil
      end.should be_false
      controller.instance_variable_get('@message').should == "Error occurred while trying to complete your request."
    end

    it "should NOT render error twice in same flow, when an error occurs" do
      controller.should_receive(:action_has_layout?).and_return(true)

      controller.should_receive(:render).once.with({:template => "shared/config_error.html", :layout => "application", :status => 404})

      controller.send(:assert_load, :foo, nil)
      controller.send(:assert_load, :bar, nil)

      controller.instance_variable_get('@message').should == "Error occurred while trying to complete your request."
    end

    it "should catch exceptions and render error when eval_loading fails" do
      controller.should_receive(:action_has_layout?).and_return(true)
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
      controller.send(:assert_load_eval, :junk) do
        raise "foo bar"
      end.should be_false
      controller.instance_variable_get('@message').should == "Error occurred while trying to complete your request."
    end

    it "should use custom message and status when evaluation is null" do
      controller.should_receive(:action_has_layout?).and_return(true)
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 407})
      controller.send(:assert_load_eval, :junk, "custom message", 407) do
        nil
      end.should be_false
      controller.instance_variable_get('@message').should == "custom message"
    end

    it "should not render layout for error page if should_not_render_layout is explicitly set on the controller" do
      controller.should_receive(:action_has_layout?).and_return(true)
      controller.instance_variable_set(:@should_not_render_layout, true)
      controller.should_receive_render_with({:template => "shared/config_error.html", :layout => nil, :status => 404})
      controller.send(:assert_load, :junk, nil).should be_false
      controller.instance_variable_get('@message').should == "Error occurred while trying to complete your request."
    end
  end

  it "should render error response for exceptions in after update block" do
    stub_save_for_success
    exception = nil
    Rails.logger.should_receive(:error) do |ex|
      exception = ex
    end
    controller.should_receive(:render_assertion_failure).with({})
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", proc do
      raise "random exception"
    end) {}
    exception.message.should == "random exception"
  end

  it "should use config_errors page for rendering errors" do
    controller._process_action_callbacks.select { |c| c.kind == :before }.map(&:filter).should include(:enable_admin_error_template)
    controller.send(:enable_admin_error_template)
    controller.error_template_for_request.should == "shared/config_error"
  end

  it "should report 'Bad Request' when no status override given" do
    controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
    controller.send(:assert_load, :foo, nil)
  end

  it "should use update_result http code if available when assert_load fails" do
    stub_save_for_validation_error do |result, _, _|
      result.conflict(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", ["message"].to_java(java.lang.String)))
    end
    controller.stub(:response).and_return(response = double('response'))
    response.stub(:headers).and_return({})
    controller.should_receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 409})

    controller.send(:save_page, "md5", "url", {:action => "foo", :controller => "bar"}, UpdateCommand.new) do
      assert_load(:foo, nil)
    end
    expect(response.headers["Go-Config-Error"]).to eq "Save failed. message"
  end

  it "should NOT continue and do render or redirect when assert_load fails during save_page but update was successful" do
    stub_save_for_success
    controller.stub(:response).and_return(response = double('response'))

    response.should_not_receive(:headers)
    controller.should_receive(:render).once.with({:template => "shared/config_error.html", :layout => "application", :status => 200})
    controller.should_not_receive(:redirect_to)

    controller.send(:save_page, "md5", "url", {:action => "foo", :controller => "bar"}, UpdateCommand.new) do
      assert_load(:foo, nil)
      assert_load(:bar, nil)
    end
  end

  it "should NOT continue and do render or redirect when assert_load fails during save_popup but update was successful" do
    stub_save_for_success
    controller.stub(:response).and_return(response = double('response'))

    response.should_not_receive(:headers)
    controller.should_receive(:render).once.with({:template => "shared/config_error.html", :layout => "application", :status => 200})
    controller.should_not_receive(:render).with(:text => "Saved successfully")

    controller.send(:save_popup, "md5", UpdateCommand.new, {:action => "foo", :controller => "bar"}) do
      assert_load(:foo, nil)
      assert_load(:bar, nil)
    end
  end

  it "should use flash-message to report successful save" do
    stub_save_for_success
    controller.should_receive_redirect_to(/http:\/\/foo.bar\?fm=#{uuid_pattern}/)
    controller.send(:save_page, "md5", "http://foo.bar", {:action => "foo", :controller => "bar"}, UpdateCommand.new) {}
  end

  it "should append to save successful message when configuration is merged" do
    stub_save_for_success @cruise_config, {:config_save_state => com.thoughtworks.go.config.ConfigSaveState::MERGED}
    final_success_message = ""
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", Proc.new{}) do |message|
      final_success_message = message
    end
    final_success_message.should == "Saved successfully. The configuration was modified by someone else, but your changes were merged successfully."
  end

  it "should retain successful message when configuration is only updated" do
    stub_save_for_success @cruise_config, {:config_save_state => com.thoughtworks.go.config.ConfigSaveState::UPDATED}
    final_success_message = ""
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", Proc.new{}) do |message|
      final_success_message = message
    end
    final_success_message.should == "Saved successfully."
  end

  it "should flatten and give unique error messages" do
    errors = ArrayList.new
    e1 = ConfigErrors.new
    e1.add("f1", "m1")
    e2 = ConfigErrors.new
    e2.add("f1", "m1")
    e3 = ConfigErrors.new
    e3.add("f2", "m2")
    errors.add(e1)
    errors.add(e2)
    errors.add(e3)
    flattened_errors = controller.send(:flatten_all_errors, errors)
    flattened_errors.size().should == 2
    flattened_errors.should include("m1", "m2")
  end
end
