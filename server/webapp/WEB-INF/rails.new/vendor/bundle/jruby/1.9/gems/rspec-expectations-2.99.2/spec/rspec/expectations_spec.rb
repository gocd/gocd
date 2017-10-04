require 'spec_helper'

module RSpec
  describe Expectations do
    def file_contents_for(lib, filename)
      # http://rubular.com/r/HYpUMftlG2
      path = $LOAD_PATH.find { |p| p.match(/\/rspec-#{lib}(-[a-f0-9]+)?\/lib/) }
      file = File.join(path, filename)
      File.read(file)
    end

    it 'has an up-to-date caller_filter file' do
      expectations = file_contents_for("expectations", "rspec/expectations/caller_filter.rb")
      core         = file_contents_for("core",         "rspec/core/caller_filter.rb")

      expect(expectations).to eq(core)
    end

    it 'prints a deprecation warning when the root file is loaded' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /rspec-expectations/)
      load "rspec-expectations.rb"
    end

    describe '.method_handle_for(object, method_name)' do

      class UntamperedClass
        def foo
          :bar
        end
      end

      class ClassWithMethodOverridden < UntamperedClass
        def method(name)
          :baz
        end
      end

      class ProxyClass < Struct.new(:original)
        undef :=~, :method
        def method_missing(name, *args, &block)
          original.__send__(name, *args, &block)
        end
      end

      if RUBY_VERSION.to_f > 1.8
        class BasicClass < BasicObject
          def foo
            :bar
          end
        end

        class BasicClassWithKernel < BasicClass
          include ::Kernel
        end
      end

      it 'fetches method definitions for vanilla objects' do
        object = UntamperedClass.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end

      it 'fetches method definitions for objects with method redefined' do
        object = ClassWithMethodOverridden.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end

      it 'fetches method definitions for proxy objects' do
        object = ProxyClass.new([])
        expect(Expectations.method_handle_for(object, :=~)).to be_a Method
      end

      it 'fails with `NameError` when an error is raised when fetching a method from an object that has overriden `method`' do
        object = double
        allow(object).to receive(:method).and_raise(Exception)
        expect {
          Expectations.method_handle_for(object, :some_undefined_method)
        }.to raise_error(NameError)
      end

      it 'fails with `NameError` when a method is fetched from an object that has overriden `method` and is not a method' do
        object = ProxyClass.new(double(:method => :baz))
        expect {
          Expectations.method_handle_for(object, :=~)
        }.to raise_error(NameError)
      end

      it 'fetches method definitions for basic objects', :if => (RUBY_VERSION.to_i >= 2 && RUBY_ENGINE != 'rbx') do
        object = BasicClass.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end

      it 'fetches method definitions for basic objects with kernel mixed in', :if => RUBY_VERSION.to_f > 1.8 do
        object = BasicClassWithKernel.new
        expect(Expectations.method_handle_for(object, :foo).call).to eq :bar
      end
    end
  end
end
