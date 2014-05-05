require File.dirname(__FILE__) + '/../../spec_helper.rb'

module Spec
  module Runner
    describe Configuration do
      with_sandboxed_options do
        with_sandboxed_config do
          
          describe "#mock_with" do
            it "should default mock framework to rspec" do
              config.mock_framework.should =~ /\/spec\/adapters\/mock_frameworks\/rspec$/
            end

            it "should set rspec mocking explicitly" do
              config.mock_with(:rspec)
              config.mock_framework.should =~ /\/spec\/adapters\/mock_frameworks\/rspec$/
            end

            it "should set mocha" do
              config.mock_with(:mocha)
              config.mock_framework.should =~ /\/spec\/adapters\/mock_frameworks\/mocha$/
            end

            it "should set flexmock" do
              config.mock_with(:flexmock)
              config.mock_framework.should =~ /\/spec\/adapters\/mock_frameworks\/flexmock$/
            end

            it "should set rr" do
              config.mock_with(:rr)
              config.mock_framework.should =~ /\/spec\/adapters\/mock_frameworks\/rr$/
            end

            it "should set an arbitrary adapter module" do
              adapter = Module.new
              config.mock_with(adapter)
              config.mock_framework.should == adapter
            end
          end

          describe "#include" do
          
            before(:each) do
              @example_group_class = Class.new(::Spec::Example::ExampleGroupDouble) {}
              Spec::Example::ExampleGroupFactory.register(:foobar, @example_group_class)
            end

            it "should include the submitted module in ExampleGroup subclasses" do
              mod = Module.new
              config.include mod
              Class.new(@example_group_class).included_modules.should include(mod)
            end

            it "should scope modules to be included for a specific type" do
              mod = Module.new
              config.include mod, :type => :foobar
              Class.new(@example_group_class).included_modules.should include(mod)
            end

            it "should not include modules in a type they are not intended for" do
              mod = Module.new
              @other_example_group_class = Class.new(::Spec::Example::ExampleGroupDouble)
              Spec::Example::ExampleGroupFactory.register(:baz, @other_example_group_class)

              config.include mod, :type => :foobar

              Class.new(@other_example_group_class).included_modules.should_not include(mod)
            end
            
            it "accepts an Array of types" do
              mod = Module.new
              @other_example_group_class = Class.new(::Spec::Example::ExampleGroupDouble)
              Spec::Example::ExampleGroupFactory.register(:baz, @other_example_group_class)

              config.include mod, :type => [:foobar, :baz]

              Class.new(@example_group_class).included_modules.should include(mod)
              Class.new(@other_example_group_class).included_modules.should include(mod)
            end

          end
      
          describe "#extend" do
        
            before(:each) do
              @example_group_class = Class.new(::Spec::Example::ExampleGroupDouble) {}
              Spec::Example::ExampleGroupFactory.register(:foobar, @example_group_class)
            end

            it "should extend all groups" do
              mod = Module.new
              ExampleGroup.should_receive(:extend).with(mod)
              Spec::Runner.configuration.extend mod
            end
      
            it "should extend specified groups" do
              mod = Module.new
              @example_group_class.should_receive(:extend).with(mod)
              Spec::Runner.configuration.extend mod, :type => :foobar
            end
      
            it "should not extend non-specified groups" do
              @other_example_group_class = Class.new(::Spec::Example::ExampleGroupDouble)
              Spec::Example::ExampleGroupFactory.register(:baz, @other_example_group_class)

              mod = Module.new
              @other_example_group_class.should_not_receive(:extend)          

              Spec::Runner.configuration.extend mod, :type => :foobar
            end
        
          end
        end
      
        describe "ordering methods: " do
        
          before(:each) do
            @special_example_group = Class.new(::Spec::Example::ExampleGroupDouble).describe("special_example_group")
            @special_child_example_group = Class.new(@special_example_group).describe("special_child_example_group")
            @nonspecial_example_group = Class.new(::Spec::Example::ExampleGroupDouble).describe("nonspecial_example_group")
            Spec::Example::ExampleGroupFactory.register(:special, @special_example_group)
            Spec::Example::ExampleGroupFactory.register(:special_child, @special_child_example_group)
            Spec::Example::ExampleGroupFactory.register(:non_special, @nonspecial_example_group)
            @example_group = @special_child_example_group.describe "Special Example Group"
            @unselected_example_group = Class.new(@nonspecial_example_group).describe "Non Special Example Group"
          end

          describe "#prepend_before" do
            it "prepends the before block on all instances of the passed in type" do
              order = []
              config.prepend_before(:all) do
                order << :prepend__before_all
              end
              config.prepend_before(:all, :type => :special) do
                order << :special_prepend__before_all
              end
              config.prepend_before(:all, :type => :special_child) do
                order << :special_child_prepend__before_all
              end
              config.prepend_before(:each) do
                order << :prepend__before_each
              end
              config.prepend_before(:each, :type => :special) do
                order << :special_prepend__before_each
              end
              config.prepend_before(:each, :type => :special_child) do
                order << :special_child_prepend__before_each
              end
              config.prepend_before(:all, :type => :non_special) do
                order << :special_prepend__before_all
              end
              config.prepend_before(:each, :type => :non_special) do
                order << :special_prepend__before_each
              end
              @example_group.it "calls prepend_before" do
              end
      
              @example_group.run(options)
              order.should == [
                :prepend__before_all,
                :special_prepend__before_all,
                :special_child_prepend__before_all,
                :prepend__before_each,
                :special_prepend__before_each,
                :special_child_prepend__before_each
              ]
            end
          end

          describe "#append_before" do

            it "calls append_before on the type" do
              order = []
              config.append_before(:all) do
                order << :append_before_all
              end
              config.append_before(:all, :type => :special) do
                order << :special_append_before_all
              end
              config.append_before(:all, :type => :special_child) do
                order << :special_child_append_before_all
              end
              config.append_before do # default is :each
                order << :append_before_each
              end
              config.append_before(:each, :type => :special) do
                order << :special_append_before_each
              end
              config.append_before(:each, :type => :special_child) do
                order << :special_child_append_before_each
              end
              config.append_before(:all, :type => :non_special) do
                order << :special_append_before_all
              end
              config.append_before(:each, :type => :non_special) do
                order << :special_append_before_each
              end
              @example_group.it "calls append_before" do
              end

              @example_group.run(options)
              order.should == [
                :append_before_all,
                :special_append_before_all,
                :special_child_append_before_all,
                :append_before_each,
                :special_append_before_each,
                :special_child_append_before_each
              ]
            end
          end

          describe "#prepend_after" do

            it "prepends the after block on all instances of the passed in type" do
              order = []
              config.prepend_after(:all) do
                order << :prepend__after_all
              end
              config.prepend_after(:all, :type => :special) do
                order << :special_prepend__after_all
              end
              config.prepend_after(:all, :type => :special) do
                order << :special_child_prepend__after_all
              end
              config.prepend_after(:each) do
                order << :prepend__after_each
              end
              config.prepend_after(:each, :type => :special) do
                order << :special_prepend__after_each
              end
              config.prepend_after(:each, :type => :special) do
                order << :special_child_prepend__after_each
              end
              config.prepend_after(:all, :type => :non_special) do
                order << :special_prepend__after_all
              end
              config.prepend_after(:each, :type => :non_special) do
                order << :special_prepend__after_each
              end
              @example_group.it "calls prepend_after" do
              end

              @example_group.run(options)
              order.should == [
                :special_child_prepend__after_each,
                :special_prepend__after_each,
                :prepend__after_each,
                :special_child_prepend__after_all,
                :special_prepend__after_all,
                :prepend__after_all
              ]
            end
          end

          describe "#append_after" do

            it "calls append_after on the type" do
              order = []
              config.append_after(:all) do
                order << :append__after_all
              end
              config.append_after(:all, :type => :special) do
                order << :special_append__after_all
              end
              config.append_after(:all, :type => :special_child) do
                order << :special_child_append__after_all
              end
              config.append_after(:each) do
                order << :append__after_each
              end
              config.append_after(:each, :type => :special) do
                order << :special_append__after_each
              end
              config.append_after(:each, :type => :special_child) do
                order << :special_child_append__after_each
              end
              config.append_after(:all, :type => :non_special) do
                order << :non_special_append_after_all
              end
              config.append_after(:each, :type => :non_special) do
                order << :non_special_append_after_each
              end
              @example_group.it "calls append_after" do
              end

              @example_group.run(options)
              order.should == [
                :special_child_append__after_each,
                :special_append__after_each,
                :append__after_each,
                :special_child_append__after_all,
                :special_append__after_all,
                :append__after_all
              ]
            end

          end
          
          describe "#predicate_matchers (DEPRECATED)" do
            it "is deprecated" do
              Spec.should_receive(:deprecate)
              config.predicate_matchers[:foo] = :bar?
            end
          end

        end
      end
    end
  end
end
