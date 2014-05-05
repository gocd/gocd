require File.dirname(__FILE__) + '/../../spec_helper'

class Thing
  attr_reader :arg
  def initialize(arg=nil)
    @arg = arg || :default
  end
  def ==(other)
    @arg == other.arg
  end
  def eql?(other)
    @arg == other.arg
  end
end

module Spec
  module Example
    describe ExampleMethods do
      module ModuleThatIsReopened; end

      module Spec::Example::ExampleMethods
        include ModuleThatIsReopened
      end

      module ModuleThatIsReopened
        def module_that_is_reopened_method; end
      end
      
      describe "with an included module that is reopened" do
        it "should have reopened methods" do
          method(:module_that_is_reopened_method).should_not be_nil
        end
      end

      describe "#should" do
        before(:each) do
          @example_group = Class.new(ExampleGroupDouble)
          @options = ::Spec::Runner::Options.new(StringIO.new, StringIO.new)
        end
        
        context "in an ExampleGroup with an implicit subject" do
          it "delegates matcher to the implied subject" do
            @example_group.describe(::Thing)
            @example_group.example { should == ::Thing.new(:default) }
            @example_group.example { should eql(::Thing.new(:default)) }
            @example_group.run(@options).should be_true
          end
        end
        
        context "in an ExampleGroup using an explicit subject" do
          it "delegates matcher to the declared subject" do
            @example_group.describe(::Thing)
            @example_group.subject { ::Thing.new(:other) }
            @example_group.example { should == ::Thing.new(:other) }
            @example_group.example { should eql(::Thing.new(:other)) }
            @example_group.run(@options).should be_true
          end
        end
		
        context "in an ExampleGroup using 'self' as an explicit subject" do
          it "delegates matcher to the ExampleGroup" do
            @example_group.describe(::Thing)
            @example_group.subject { self }
            @example_group.example { should == self }
            @example_group.example { should eql(self) }
            @example_group.example do
              self.instance_eval("def method_ok?; true end")
              should be_method_ok
            end
            @example_group.run(@options).should be_true
          end
        end
      end

      describe "#should_not" do
        before(:each) do
          @example_group = Class.new(ExampleGroupDouble)
          @options = ::Spec::Runner::Options.new(StringIO.new, StringIO.new)
        end

        context "in an ExampleGroup with an implicit subject" do
          it "delegates matcher to the implied subject" do
            @example_group.describe(::Thing)
            @example_group.example { should_not == ::Thing.new(:other) }
            @example_group.example { should_not eql(::Thing.new(:other)) }
            @example_group.run(@options).should be_true
          end
        end
        
        context "in an ExampleGroup using an explicit subject" do
          it "delegates matcher to the declared subject" do
            @example_group.describe(::Thing)
            @example_group.subject { ::Thing.new(:other) }
            @example_group.example { should_not == ::Thing.new(:default) }
            @example_group.example { should_not eql(::Thing.new(:default)) }
            @example_group.run(@options).should be_true
          end
        end
		
        context "in an ExampleGroup using 'self' as an explicit subject" do
          it "delegates matcher to the ExampleGroup" do
            @example_group.describe(::Thing)
            @example_group.subject { self }
            @example_group.example { should_not == ::Thing.new(:default) }
            @example_group.example { should_not eql(::Thing.new(:default)) }
            @example_group.example do
              self.instance_eval("def method_ok?; false end")
              should_not be_method_ok
            end
            @example_group.run(@options).should be_true
          end
        end
      end
    end

    describe "#options" do
      it "should expose the options hash" do
        example = ExampleGroupDouble.new ExampleProxy.new("name", :this => 'that') do; end
        example.options[:this].should == 'that'
      end
    end
    
    describe "#set_instance_variables_from_hash" do
      it "preserves the options" do
        example = ExampleGroupDouble.new ExampleProxy.new("name", :this => 'that') do; end
        example.set_instance_variables_from_hash({:@_options => {}})
        example.options[:this].should == 'that'
      end
    end
    
    describe "#description" do
      it "returns the supplied description" do
        example = ExampleGroupDouble.new ExampleProxy.new("name") do; end
        example.description.should == "name"
      end
      it "returns the generated description if there is no description supplied" do
        example = ExampleGroupDouble.new ExampleProxy.new do; end
        Spec::Matchers.stub!(:generated_description).and_return('this message')
        example.description.should == "this message"
      end
      it "warns if there is no supplied or generated description" do
        example = ExampleGroupDouble.new ExampleProxy.new(nil, {}, "this backtrace") do; end
        Spec::Matchers.stub!(:generated_description).and_return(nil)
        Spec.should_receive(:warn).with("No description supplied for example declared on this backtrace")
        example.description
      end
    end
    
    describe "#expect" do
      it "aliases #should with #to on the proc" do
        a = 3
        expect { a += 1 }.to change{a}.from(3).to(4)
      end

      it "aliases #should_not with #to_not on the proc" do
        a = 3
        expect { nil }.to_not change{a}
      end
    end

  end
end
