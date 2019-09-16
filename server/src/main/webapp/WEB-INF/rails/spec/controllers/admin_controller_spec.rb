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

    expect(controller.instance_variable_get('@cruise_config')).to eq(@cruise_config)
    expect(controller.instance_variable_get('@node')).to eq(@cruise_config.pipelineConfigByName(CaseInsensitiveString.new('pipeline1')))
    expect(controller.instance_variable_get('@subject')).to eq(@cruise_config.pipelineConfigByName(CaseInsensitiveString.new('pipeline1')))
  end

  it "should capture config before and after update" do
    stub_save_for_success
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", proc {}) {}

    expect(controller.instance_variable_get('@config_after').pipelineConfigByName(CaseInsensitiveString.new("pipeline1")).getLabelTemplate()).to eq("foo-${COUNT}")
  end

  describe "assert_load" do
    it "should assign variable" do
      expect(controller.send(:assert_load, :junk, "junk_value")).to be_truthy
      expect(controller.instance_variable_get('@junk')).to eq("junk_value")
    end

    it "should error out on null assignment" do
      expect(controller).to receive(:action_has_layout?).and_return(true)
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
      expect(controller.send(:assert_load, :junk, nil)).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("Error occurred while trying to complete your request.")
    end

    it "should not render error page layout when action doesn't have one" do
      expect(controller).to receive(:action_has_layout?).and_return(false)
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => nil, :status => 404})
      expect(controller.send(:assert_load, :junk, nil)).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("Error occurred while trying to complete your request.")
    end

    it "should allow caller to use custom error message and status" do
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 409})
      expect(controller.send(:assert_load, :junk, nil, "callers custom message", 409)).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("callers custom message")
    end

    it "should load the result of evaluation" do
      expect(controller.send(:assert_load_eval, :junk) do
        "junk_value"
      end).to be_truthy
      expect(controller.instance_variable_get('@junk')).to eq("junk_value")
    end

    it "should render error when result of evaluation is null" do
      expect(controller).to receive(:action_has_layout?).and_return(true)
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
      expect(controller.send(:assert_load_eval, :junk) do
        nil
      end).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("Error occurred while trying to complete your request.")
    end

    it "should NOT render error twice in same flow, when an error occurs" do
      expect(controller).to receive(:action_has_layout?).and_return(true)

      expect(controller).to receive(:render).once.with({:template => "shared/config_error.html", :layout => "application", :status => 404})

      controller.send(:assert_load, :foo, nil)
      controller.send(:assert_load, :bar, nil)

      expect(controller.instance_variable_get('@message')).to eq("Error occurred while trying to complete your request.")
    end

    it "should catch exceptions and render error when eval_loading fails" do
      expect(controller).to receive(:action_has_layout?).and_return(true)
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
      expect(controller.send(:assert_load_eval, :junk) do
        raise "foo bar"
      end).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("Error occurred while trying to complete your request.")
    end

    it "should use custom message and status when evaluation is null" do
      expect(controller).to receive(:action_has_layout?).and_return(true)
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 407})
      expect(controller.send(:assert_load_eval, :junk, "custom message", 407) do
        nil
      end).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("custom message")
    end

    it "should not render layout for error page if should_not_render_layout is explicitly set on the controller" do
      expect(controller).to receive(:action_has_layout?).and_return(true)
      controller.instance_variable_set(:@should_not_render_layout, true)
      expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => nil, :status => 404})
      expect(controller.send(:assert_load, :junk, nil)).to be_falsey
      expect(controller.instance_variable_get('@message')).to eq("Error occurred while trying to complete your request.")
    end
  end

  it "should render error response for exceptions in after update block" do
    stub_save_for_success
    exception = nil
    expect(Rails.logger).to receive(:error) do |ex|
      exception = ex
    end
    expect(controller).to receive(:render_assertion_failure).with({})
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", proc do
      raise "random exception"
    end) {}
    expect(exception.message).to eq("random exception")
  end

  it "should use config_errors page for rendering errors" do
    expect(controller._process_action_callbacks.select { |c| c.kind == :before }.map(&:filter)).to include(:enable_admin_error_template)
    controller.send(:enable_admin_error_template)
    expect(controller.error_template_for_request).to eq("shared/config_error")
  end

  it "should report 'Bad Request' when no status override given" do
    expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 404})
    controller.send(:assert_load, :foo, nil)
  end

  it "should use update_result http code if available when assert_load fails" do
    stub_save_for_validation_error do |result, _, _|
      result.conflict("Save failed. message")
    end
    allow(controller).to receive(:response).and_return(response = double('response'))
    allow(response).to receive(:headers).and_return({})
    expect(controller).to receive_render_with({:template => "shared/config_error.html", :layout => "application", :status => 409})

    controller.send(:save_page, "md5", "url", {:action => "foo", :controller => "bar"}, UpdateCommand.new) do
      assert_load(:foo, nil)
    end
    expect(response.headers["Go-Config-Error"]).to eq "Save failed. message"
  end

  it "should NOT continue and do render or redirect when assert_load fails during save_page but update was successful" do
    stub_save_for_success
    allow(controller).to receive(:response).and_return(response = double('response'))

    expect(response).not_to receive(:headers)
    expect(controller).to receive(:render).once.with({:template => "shared/config_error.html", :layout => "application", :status => 200})
    expect(controller).not_to receive(:redirect_to)

    controller.send(:save_page, "md5", "url", {:action => "foo", :controller => "bar"}, UpdateCommand.new) do
      assert_load(:foo, nil)
      assert_load(:bar, nil)
    end
  end

  it "should NOT continue and do render or redirect when assert_load fails during save_popup but update was successful" do
    stub_save_for_success
    allow(controller).to receive(:response).and_return(response = double('response'))

    expect(response).not_to receive(:headers)
    expect(controller).to receive(:render).once.with({:template => "shared/config_error.html", :layout => "application", :status => 200})
    expect(controller).not_to receive(:render).with(:text => "Saved successfully")

    controller.send(:save_popup, "md5", UpdateCommand.new, {:action => "foo", :controller => "bar"}) do
      assert_load(:foo, nil)
      assert_load(:bar, nil)
    end
  end

  it "should use flash-message to report successful save" do
    stub_save_for_success
    expect(controller).to receive_redirect_to(/http:\/\/foo.bar\?fm=#{uuid_pattern}/)
    controller.send(:save_page, "md5", "http://foo.bar", {:action => "foo", :controller => "bar"}, UpdateCommand.new) {}
  end

  it "should append to save successful message when configuration is merged" do
    stub_save_for_success @cruise_config, {:config_save_state => com.thoughtworks.go.config.ConfigSaveState::MERGED}
    final_success_message = ""
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", Proc.new{}) do |message|
      final_success_message = message
    end
    expect(final_success_message).to eq("Saved successfully. The configuration was modified by someone else, but your changes were merged successfully.")
  end

  it "should retain successful message when configuration is only updated" do
    stub_save_for_success @cruise_config, {:config_save_state => com.thoughtworks.go.config.ConfigSaveState::UPDATED}
    final_success_message = ""
    controller.send(:save, "md5", {:action => "foo", :controller => "bar"}, UpdateCommand.new, "Saved successfully.", Proc.new{}) do |message|
      final_success_message = message
    end
    expect(final_success_message).to eq("Saved successfully.")
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
    expect(flattened_errors.size()).to eq(2)
    expect(flattened_errors).to include("m1", "m2")
  end
end
