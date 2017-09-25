require 'spec_helper'
require 'rspec/mocks'

module RSpec
  module Mocks
    describe Space do
      let(:space) { RSpec::Mocks::Space.new }
      let(:dbl_1) { Object.new }
      let(:dbl_2) { Object.new }

      before do
        space.ensure_registered(dbl_1)
        space.ensure_registered(dbl_2)
      end

      it "verifies all mocks within" do
        verifies = []

        space.proxy_for(dbl_1).stub(:verify) { verifies << :dbl_1 }
        space.proxy_for(dbl_2).stub(:verify) { verifies << :dbl_2 }

        space.verify_all

        expect(verifies).to match_array([:dbl_1, :dbl_2])
      end

      def define_singleton_method_on_recorder_for(klass, name, &block)
        recorder = space.any_instance_recorder_for(klass)
        (class << recorder; self; end).send(:define_method, name, &block)
      end

      it 'verifies all any_instance recorders within' do
        klass_1, klass_2 = Class.new, Class.new

        verifies = []

        # We can't `stub` a method on the recorder because it defines its own `stub`...
        define_singleton_method_on_recorder_for(klass_1, :verify) { verifies << :klass_1 }
        define_singleton_method_on_recorder_for(klass_2, :verify) { verifies << :klass_2 }

        space.verify_all

        expect(verifies).to match_array([:klass_1, :klass_2])
      end

      it "resets all mocks within" do
        resets = []

        space.proxy_for(dbl_1).stub(:reset) { resets << :dbl_1 }
        space.proxy_for(dbl_2).stub(:reset) { resets << :dbl_2 }

        space.reset_all

        expect(resets).to match_array([:dbl_1, :dbl_2])
      end

      it "does not leak mock proxies between examples" do
        expect {
          space.reset_all
        }.to change { space.proxies.size }.to(0)
      end

      it 'does not leak any instance recorders between examples' do
        space.any_instance_recorder_for(Class.new)
        expect {
          space.reset_all
        }.to change { space.any_instance_recorders.size }.to(0)
      end

      it "resets the ordering" do
        space.expectation_ordering.register :some_expectation

        expect {
          space.reset_all
        }.to change { space.expectation_ordering.empty? }.from(false).to(true)
      end

      it "only adds an instance once" do
        m1 = double("mock1")

        expect {
          space.ensure_registered(m1)
        }.to change { space.proxies }

        expect {
          space.ensure_registered(m1)
        }.not_to change { space.proxies }
      end

      it 'returns a consistent any_instance_recorder for a given class' do
        klass_1, klass_2 = Class.new, Class.new

        r1 = space.any_instance_recorder_for(klass_1)
        r2 = space.any_instance_recorder_for(klass_1)
        r3 = space.any_instance_recorder_for(klass_2)

        expect(r1).to be(r2)
        expect(r1).not_to be(r3)
      end
    end
  end
end

