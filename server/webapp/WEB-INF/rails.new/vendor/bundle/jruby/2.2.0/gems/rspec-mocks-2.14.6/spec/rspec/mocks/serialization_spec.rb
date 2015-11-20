require 'spec_helper'

module RSpec
  module Mocks
    describe "Serialization of mocked objects" do

      class SerializableObject < Struct.new(:foo, :bar); end

      def self.with_yaml_loaded(&block)
        context 'with YAML loaded' do
          module_eval(&block)
        end
      end

      def self.without_yaml_loaded(&block)
        context 'without YAML loaded' do
          before do
            # We can't really unload yaml, but we can fake it here...
            hide_const("YAML")
            Struct.class_eval do
              alias __old_to_yaml to_yaml
              undef to_yaml
            end
          end

          module_eval(&block)

          after do
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
          expect(serializable_object).not_to respond_to(:to_yaml)
          set_stub
          expect(serializable_object).not_to respond_to(:to_yaml)
        end
      end

      it 'marshals the same with and without stubbing' do
        expect { set_stub }.to_not change { Marshal.dump(serializable_object) }
      end
    end
  end
end
