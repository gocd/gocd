require File.dirname(__FILE__) + '/../../../spec_helper'

module Spec
  module Example
    describe ExampleGroupFactory do
      it "should return a ModelExampleGroup when given :type => :model" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :type => :model
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ModelExampleGroup
      end

      it "should return a ModelExampleGroup when given :location => '/blah/spec/models/'" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '/blah/spec/models/blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ModelExampleGroup
      end

      it "should return a ModelExampleGroup when given :location => '\\blah\\spec\\models\\' (windows format)" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '\\blah\\spec\\models\\blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ModelExampleGroup
      end

      it "should return an ActiveSupport::TestCase when given :location => '/blah/spec/foo/' (anything other than controllers, views and helpers)" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '/blah/spec/foo/blah.rb'
        ) {}
        example_group.superclass.should == ActiveSupport::TestCase
      end

      it "should return an ActiveSupport::TestCase when given :location => '\\blah\\spec\\foo\\' (windows format)  (anything other than controllers, views and helpers)" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '\\blah\\spec\\foo\\blah.rb'
        ) {}
        example_group.superclass.should == ActiveSupport::TestCase
      end

      it "should return a ViewExampleGroup when given :type => :view" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :type => :view
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ViewExampleGroup
      end

      it "should return a ViewExampleGroup when given :location => '/blah/spec/views/'" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '/blah/spec/views/blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ViewExampleGroup
      end

      it "should return a ModelExampleGroup when given :location => '\\blah\\spec\\views\\' (windows format)" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '\\blah\\spec\\views\\blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ViewExampleGroup
      end

      it "should return a HelperExampleGroup when given :type => :helper" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :type => :helper
        ) {}
        example_group.superclass.should == Spec::Rails::Example::HelperExampleGroup
      end

      it "should return a HelperExampleGroup when given :location => '/blah/spec/helpers/'" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '/blah/spec/helpers/blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::HelperExampleGroup
      end

      it "should return a ModelExampleGroup when given :location => '\\blah\\spec\\helpers\\' (windows format)" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '\\blah\\spec\\helpers\\blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::HelperExampleGroup
      end

      it "should return a ControllerExampleGroup when given :type => :controller" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :type => :controller
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ControllerExampleGroup
      end

      it "should return a ControllerExampleGroup when given :location => '/blah/spec/controllers/'" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '/blah/spec/controllers/blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ControllerExampleGroup
      end

      it "should return a ModelExampleGroup when given :location => '\\blah\\spec\\controllers\\' (windows format)" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '\\blah\\spec\\controllers\\blah.rb'
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ControllerExampleGroup
      end

      it "should favor the :type over the :location" do
        example_group = Spec::Example::ExampleGroupFactory.create_example_group(
          "name", :location => '/blah/spec/models/blah.rb', :type => :controller
        ) {}
        example_group.superclass.should == Spec::Rails::Example::ControllerExampleGroup
      end
    end
  end
end
