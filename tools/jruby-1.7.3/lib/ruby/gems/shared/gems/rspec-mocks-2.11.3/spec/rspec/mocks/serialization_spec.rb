require 'spec_helper'

module RSpec
  module Mocks
    describe Serialization do

      class SerializableObject < Struct.new(:foo, :bar); end

      class SerializableMockProxy
        attr_reader :mock_proxy

        def initialize(mock_proxy)
          @mock_proxy = mock_proxy
        end

        def ==(other)
          other.class == self.class && other.mock_proxy == mock_proxy
        end
      end

      def self.with_yaml_loaded(&block)
        context 'with YAML loaded' do
          module_eval(&block)
        end
      end

      def self.without_yaml_loaded(&block)
        context 'without YAML loaded' do
          before do
            # We can't really unload yaml, but we can fake it here...
            @orig_yaml_constant = Object.send(:remove_const, :YAML)
            Struct.class_eval do
              alias __old_to_yaml to_yaml
              undef to_yaml
            end
          end

          module_eval(&block)

          after do
            Object.const_set(:YAML, @orig_yaml_constant)
            Struct.class_eval do
              alias to_yaml __old_to_yaml
              undef __old_to_yaml
            end
          end
        end
      end

      let(:serializable_object) { RSpec::Mocks::SerializableObject.new(7, "something") }

      def set_stub
        serializable_object.stub(:bazz => 5)
      end

      shared_examples_for 'normal YAML serialization' do
        it 'serializes to yaml the same with and without stubbing, using #to_yaml' do
          expect { set_stub }.to_not change { serializable_object.to_yaml }
        end

        it 'serializes to yaml the same with and without stubbing, using YAML.dump' do
          expect { set_stub }.to_not change { ::YAML.dump(serializable_object) }
        end
      end

      with_yaml_loaded do
        compiled_with_psych = begin
          require 'psych'
          true
        rescue LoadError
          false
        end

        if compiled_with_psych
          context 'using Syck as the YAML engine' do
            before(:each) { ::YAML::ENGINE.yamler = 'syck' }
            it_behaves_like 'normal YAML serialization'
          end

          context 'using Psych as the YAML engine' do
            before(:each) { ::YAML::ENGINE.yamler = 'psych' }
            it_behaves_like 'normal YAML serialization'
          end
        else
          it_behaves_like 'normal YAML serialization'
        end
      end

      without_yaml_loaded do
        it 'does not add #to_yaml to the stubbed object' do
          serializable_object.should_not respond_to(:to_yaml)
          set_stub
          serializable_object.should_not respond_to(:to_yaml)
        end
      end

      it 'marshals the same with and without stubbing' do
        expect { set_stub }.to_not change { Marshal.dump(serializable_object) }
      end

      describe "an object that has its own mock_proxy instance variable" do
        let(:serializable_object) { RSpec::Mocks::SerializableMockProxy.new(:my_mock_proxy) }

        it 'does not interfere with its marshalling' do
          marshalled_copy = Marshal.load(Marshal.dump(serializable_object))
          marshalled_copy.should eq serializable_object
        end
      end
    end
  end
end
