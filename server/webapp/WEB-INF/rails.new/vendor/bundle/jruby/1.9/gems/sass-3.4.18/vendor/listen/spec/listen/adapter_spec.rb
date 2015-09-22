require 'spec_helper'

describe Listen::Adapter do
  subject { described_class.new('dir') }

  describe '#initialize' do
    it 'sets the latency to the default one' do
      subject.latency.should eq described_class::DEFAULT_LATENCY
    end

    it 'accepts a single directory to watch' do
      described_class.new('dir').directories = %w{dir}
    end

    it 'accepts multiple directories to watch' do
      described_class.new(%w{dir1 dir2}).directories.should eq %w{dir1 dir2}
    end
  end

  describe ".select_and_initialize" do
    before do
      Listen::Adapter::OPTIMIZED_ADAPTERS.each do |adapter|
        Listen::Adapters.const_get(adapter).stub(:usable_and_works?) { false }
      end
    end

    context "with force_adapter option" do
      it "returns an instance of the given adapter class" do
        expected_adapter = Object.new
        adapter_class = double('adapter_class')
        options = {:force_adapter => adapter_class}

        adapter_class.should_receive(:load_dependent_adapter)
        adapter_class.should_receive(:new).with('dir', options) { expected_adapter }

        adapter = described_class.select_and_initialize('dir', options)
        adapter.should be(expected_adapter)
      end
    end

    context "with no specific adapter usable" do
      it "returns Listen::Adapters::Polling instance" do
        Kernel.stub(:warn)
        Listen::Adapters::Polling.should_receive(:new).with('dir', {})
        described_class.select_and_initialize('dir')
      end

      it 'warns with the default polling fallback message' do
        Kernel.should_receive(:warn).with(/#{Listen::Adapter::POLLING_FALLBACK_MESSAGE}/)
        described_class.select_and_initialize('dir')
      end

      context "with custom polling_fallback_message option" do
        it "warns with the custom polling fallback message" do
          Kernel.should_receive(:warn).with(/custom/)
          described_class.select_and_initialize('dir', :polling_fallback_message => 'custom')
        end
      end

      context "with polling_fallback_message to false" do
        it "doesn't warn with a polling fallback message" do
          Kernel.should_not_receive(:warn)
          described_class.select_and_initialize('dir', :polling_fallback_message => false)
        end
      end
    end

    Listen::Adapter::OPTIMIZED_ADAPTERS.each do |adapter|
      adapter_class = Listen::Adapters.const_get(adapter)

      context "on #{adapter}" do
        before { adapter_class.stub(:usable_and_works?) { true } }

        it "uses Listen::Adapters::#{adapter}" do
          adapter_class.should_receive(:new).with('dir', {})
          described_class.select_and_initialize('dir')
        end

        context 'when the use of the polling adapter is forced' do
          it 'uses Listen::Adapters::Polling' do
            Listen::Adapters::Polling.should_receive(:new).with('dir', {})
            described_class.select_and_initialize('dir', :force_polling => true)
          end
        end
      end
    end
  end

  describe '.load_dependend_adapter' do
    after(:each) { described_class.instance_variable_set('@loaded', nil) }

    it 'returns true (success) even if the adapter_gem has already been required' do
      described_class.stub(:adapter_gem => 'already_loaded_gem')
      described_class.stub(:require => false)

      described_class.load_dependent_adapter.should be_true
    end

    it 'returns false (failure) if the adapter_gem cannot be required' do
      described_class.stub(:adapter_gem => 'unloadable_gem')
      described_class.stub(:require) do
        raise LoadError.new('no such file to load -- unloadable_gem')
      end

      described_class.load_dependent_adapter.should be_false
    end
  end

  Listen::Adapter::OPTIMIZED_ADAPTERS.each do |adapter|
    adapter_class = Listen::Adapters.const_get(adapter)
    if adapter_class.usable?
      describe '.usable_and_works?' do
        it 'checks if the adapter is usable' do
          adapter_class.stub(:works?)
          adapter_class.should_receive(:usable?)
          adapter_class.usable_and_works?('dir')
        end

        context 'with one directory' do
          it 'tests if that directory actually work' do
            fixtures do |path|
              adapter_class.should_receive(:works?).with(path, anything).and_return(true)
              adapter_class.usable_and_works?(path)
            end
          end
        end

        context 'with multiple directories' do
          it 'tests if each directory passed does actually work' do
            fixtures(3) do |path1, path2, path3|
              adapter_class.should_receive(:works?).exactly(3).times.with do |path, options|
                [path1, path2, path3].include? path
              end.and_return(true)
              adapter_class.usable_and_works?([path1, path2, path3])
            end
          end
        end
      end

      describe '.works?' do
        it 'does work' do
          fixtures do |path|
            adapter_class.works?(path).should be_true
          end
        end
      end
    end
  end
end
