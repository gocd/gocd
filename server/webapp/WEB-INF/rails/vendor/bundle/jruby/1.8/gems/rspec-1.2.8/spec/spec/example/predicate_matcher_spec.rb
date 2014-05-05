require File.dirname(__FILE__) + '/../../spec_helper'

module Spec
  module Example
    class Fish
      def can_swim?(distance_in_yards)
        distance_in_yards < 1000
      end
    end
    
    describe "predicate_matcher[method_on_object] = matcher_method" do
      before(:each) do
        Spec.stub!(:deprecate)
      end
      
      it "is deprecated" do
        Spec.should_receive(:deprecate)
        group = ExampleGroupDouble.describe("foo") do
          predicate_matchers[:swim] = :can_swim?
        end
        group.run(Spec::Runner::Options.new(StringIO.new, StringIO.new))
      end

      it "should match matcher_method if method_on_object returns true" do
        group = ExampleGroupDouble.describe(Fish) do
          predicate_matchers[:swim] = :can_swim?
          it { should swim(100) }
        end
        group.run(Spec::Runner::Options.new(StringIO.new, StringIO.new))
      end

      it "should not match matcher_method if method_on_object returns false" do
        group = ExampleGroupDouble.describe(Fish) do
          predicate_matchers[:swim] = :can_swim?
          it { should_not swim(1000) }
        end
        group.run(Spec::Runner::Options.new(StringIO.new, StringIO.new))
      end
    end
  end
end
