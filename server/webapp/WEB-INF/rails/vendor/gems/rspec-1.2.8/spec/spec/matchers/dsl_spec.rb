require File.dirname(__FILE__) + '/../../spec_helper.rb'

module Spec
  module Matchers
    module DSL
      describe "#create" do
        it "is deprecated" do
          Spec.should_receive(:deprecate)
          mod = Module.new
          mod.extend Spec::Matchers::DSL
          mod.create(:foo)
        end
      end
      
      describe "#define" do
        it "creates a method that initializes a new matcher with the submitted name and expected arg" do
          # FIXME - this expects new to be called, but we need something
          # more robust - that expects new to be called with a specific
          # block (lambda, proc, whatever)
          mod = Module.new
          mod.extend Spec::Matchers::DSL
          mod.define(:foo)
    
          obj = Object.new
          obj.extend mod
    
          Spec::Matchers::Matcher.should_receive(:new).with(:foo, 3)
    
          obj.foo(3)
        end
      end
    end
  end
end
