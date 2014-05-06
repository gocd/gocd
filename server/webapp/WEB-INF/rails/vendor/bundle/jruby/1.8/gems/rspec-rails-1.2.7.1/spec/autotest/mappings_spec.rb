require File.dirname(__FILE__) + '/../spec_helper'
require File.dirname(__FILE__) + '/../../lib/autotest/rails_rspec'
require File.dirname(__FILE__) + '/../../../rspec/spec/autotest/autotest_matchers'

describe Autotest::RailsRspec, "file mapping" do
  before(:each) do
    @autotest = Autotest::RailsRspec.new
    @autotest.hook :initialize
  end
  
  it "should map model example to model" do
    @autotest.should map_specs(['spec/models/thing_spec.rb']).
                            to('app/models/thing.rb')
  end
  
  it "should map controller example to controller" do
    @autotest.should map_specs(['spec/controllers/things_controller_spec.rb']).
                            to('app/controllers/things_controller.rb')
  end
  
  it "should map view.rhtml" do
    @autotest.should map_specs(['spec/views/things/index.rhtml_spec.rb']).
                            to('app/views/things/index.rhtml')
  end
  
  it "should map view.rhtml with underscores in example filename" do
    @autotest.should map_specs(['spec/views/things/index_rhtml_spec.rb']).
                            to('app/views/things/index.rhtml')
  end
  
  it "should map view.html.erb" do
    @autotest.should map_specs(['spec/views/things/index.html.erb_spec.rb']).
                            to('app/views/things/index.html.erb')
  end
  
  describe "between routes.rb and things which depend on routes" do
    it "should map routes.rb to controllers" do
      @autotest.should map_specs(['spec/controllers/things_controller_spec.rb']).
                              to('config/routes.rb')
    end
    
    it "should map routes.rb to views" do
      @autotest.should map_specs(['spec/views/things/action.html.erb_spec.rb']).
                              to('config/routes.rb')
    end
    
    it "should map routes.rb to helpers" do
      @autotest.should map_specs(['spec/helpers/things_helper_spec.rb']).
                              to('config/routes.rb')
    end
    
    it "should map routing example to routes" do
      @autotest.should map_specs(['spec/routing/thing_spec.rb']).
                              to('config/routes.rb')
    end
  end  
  
  describe "between the config and specs" do
    ['spec/controllers/things_controller_spec.rb', 
     'spec/views/things/action.html.erb_spec.rb', 
     'spec/helpers/things_helper_spec.rb', 
     'spec/routing/thing_spec.rb', 
     'spec/models/thing_spec.rb'].each do |file_path|
    
      it "should map environment.rb to #{file_path}" do
        @autotest.should map_specs([file_path]).
                                to('config/environment.rb')
      end
      
      it "should map environments/test.rb to #{file_path}" do
        @autotest.should map_specs([file_path]).
                                to('config/environments/test.rb')
      end
      
      it "should map boot.rb to #{file_path}" do
        @autotest.should map_specs([file_path]).
                                to('config/boot.rb')
      end
      
      it "should map spec_helper.rb to #{file_path}" do
        @autotest.should map_specs([file_path]).
                                to('spec/spec_helper.rb')
      end
    end
  end
end
