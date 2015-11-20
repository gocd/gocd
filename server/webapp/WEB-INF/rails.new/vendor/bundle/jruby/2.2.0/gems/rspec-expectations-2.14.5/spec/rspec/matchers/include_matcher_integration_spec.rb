require 'spec_helper'

module RSpec
  module Matchers
    describe "include() interaction with built-in matchers" do
      it "works with be_within(delta).of(expected)" do
        expect([10, 20, 30]).to include( be_within(5).of(24) )
        expect([10, 20, 30]).not_to include( be_within(3).of(24) )
      end

      it "works with be_instance_of(klass)" do
        expect(["foo", 123, {:foo => "bar"}]).to include( be_instance_of(Hash) )
        expect(["foo", 123, {:foo => "bar"}]).not_to include( be_instance_of(Range) )
      end

      it "works with be_kind_of(klass)" do
        class StringSubclass < String; end
        class NotHashSubclass; end

        expect([StringSubclass.new("baz")]).to include( be_kind_of(String) )
        expect([NotHashSubclass.new]).not_to include( be_kind_of(Hash) )
      end

      it "works with be_[some predicate]" do
        expect([double("actual", :happy? => true)]).to include( be_happy )
        expect([double("actual", :happy? => false)]).not_to include( be_happy )
      end
    end
  end
end
