require File.dirname(__FILE__) + '/../../../spec_helper'
require 'controller_spec_controller'

['integration', 'isolation'].each do |mode|
  describe "A controller example running in #{mode} mode", :type => :controller do
    controller_name :controller_spec
    integrate_views if mode == 'integration'
    
    describe "without use_rails_error_handling!" do
      describe "with an error that is *not* rescued" do
        it "raises the error" do
          lambda do
            get 'un_rescued_error_action'
          end.should raise_error(ControllerSpecController::UnRescuedError)
        end
      end
      describe "with an error that *is* rescued" do
        it "returns a 200" do
          get 'rescued_error_action'
          response.response_code.should == 200
        end
      end
    end

    describe "with deprecated use_rails_error_handling!" do
      before(:each) do
        Kernel.stub!(:warn)
      end

      it "warns of deprecation" do
        Kernel.should_receive(:warn).with(/DEPRECATION NOTICE/)
        controller.use_rails_error_handling!
      end

      describe "with an error that is *not* rescued" do
        it "returns the error code" do
          controller.use_rails_error_handling!
          get 'un_rescued_error_action'
          response.response_code.should == 500
        end
      end

      describe "with an error that *is* rescued" do
        it "returns a 200" do
          controller.use_rails_error_handling!
          get 'rescued_error_action'
          response.response_code.should == 200
        end
      end
    end

    describe "with rescue_action_in_public!" do
      describe "with an error that is *not* rescued" do
        it "returns the error code" do
          rescue_action_in_public!
          get 'un_rescued_error_action'
          response.response_code.should == 500
        end
      end

      describe "with an error that *is* rescued" do
        it "returns a 200" do
          rescue_action_in_public!
          get 'rescued_error_action'
          response.response_code.should == 200
        end
      end
    end
    
    describe "with bypass_rescue" do
      describe "with an error that is *not* rescued" do
        it "raises the error" do
          bypass_rescue
          lambda do
            get 'un_rescued_error_action'
          end.should raise_error(ControllerSpecController::UnRescuedError)
        end
      end

      describe "with an error that *is* rescued" do
        it "raises the error" do
          bypass_rescue
          lambda do
            get 'rescued_error_action'
          end.should raise_error(ControllerSpecController::RescuedError)
        end
      end
    end
  end
end