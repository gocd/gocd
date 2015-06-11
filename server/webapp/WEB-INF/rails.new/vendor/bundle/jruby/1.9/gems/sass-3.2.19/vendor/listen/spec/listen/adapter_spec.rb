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
      Listen::Adapters::Darwin.stub(:usable_and_works?) { false }
      Listen::Adapters::Linux.stub(:usable_and_works?) { false }
      Listen::Adapters::BSD.stub(:usable_and_works?) { false }
      Listen::Adapters::Windows.stub(:usable_and_works?) { false }
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

      context 'when the dependencies of an adapter are not satisfied' do
        before do
          Listen::Adapters::Darwin.stub(:usable_and_works?).and_raise(Listen::DependencyManager::Error)
          Listen::Adapters::Linux.stub(:usable_and_works?).and_raise(Listen::DependencyManager::Error)
          Listen::Adapters::BSD.stub(:usable_and_works?).and_raise(Listen::DependencyManager::Error)
          Listen::Adapters::Windows.stub(:usable_and_works?).and_raise(Listen::DependencyManager::Error)
        end

        it 'invites the user to satisfy the dependencies of the adapter in the warning' do
          Kernel.should_receive(:warn).with(/#{Listen::Adapter::MISSING_DEPENDENCY_MESSAGE}/)
          described_class.select_and_initialize('dir')
        end
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

    context "on Mac OX >= 10.6" do
      before { Listen::Adapters::Darwin.stub(:usable_and_works?) { true } }

      it "uses Listen::Adapters::Darwin" do
        Listen::Adapters::Darwin.should_receive(:new).with('dir', {})
        described_class.select_and_initialize('dir')
      end

      context 'when the use of the polling adapter is forced' do
        it 'uses Listen::Adapters::Polling' do
          Listen::Adapters::Polling.should_receive(:new).with('dir', {})
          described_class.select_and_initialize('dir', :force_polling => true)
        end
      end
    end

    context "on Linux" do
      before { Listen::Adapters::Linux.stub(:usable_and_works?) { true } }

      it "uses Listen::Adapters::Linux" do
        Listen::Adapters::Linux.should_receive(:new).with('dir', {})
        described_class.select_and_initialize('dir')
      end

      context 'when the use of the polling adapter is forced' do
        it 'uses Listen::Adapters::Polling' do
          Listen::Adapters::Polling.should_receive(:new).with('dir', {})
          described_class.select_and_initialize('dir', :force_polling => true)
        end
      end
    end

    context "on BSD" do
      before { Listen::Adapters::BSD.stub(:usable_and_works?) { true } }

      it "uses Listen::Adapters::BSD" do
        Listen::Adapters::BSD.should_receive(:new).with('dir', {})
        described_class.select_and_initialize('dir')
      end

      context 'when the use of the polling adapter is forced' do
        it 'uses Listen::Adapters::Polling' do
          Listen::Adapters::Polling.should_receive(:new).with('dir', {})
          described_class.select_and_initialize('dir', :force_polling => true)
        end
      end
    end

    context "on Windows" do
      before { Listen::Adapters::Windows.stub(:usable_and_works?) { true } }

      it "uses Listen::Adapters::Windows" do
        Listen::Adapters::Windows.should_receive(:new).with('dir', {})
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

  [Listen::Adapters::Darwin, Listen::Adapters::Linux,
   Listen::Adapters::BSD, Listen::Adapters::Windows].each do
    |adapter_class|
    if adapter_class.usable?
      describe '.usable?' do
        it 'checks the dependencies' do
          adapter_class.should_receive(:load_depenencies)
          adapter_class.should_receive(:dependencies_loaded?)
          adapter_class.usable?
        end
      end

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
