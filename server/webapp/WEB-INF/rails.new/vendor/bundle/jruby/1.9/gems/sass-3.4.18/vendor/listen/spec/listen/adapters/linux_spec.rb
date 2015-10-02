require 'spec_helper'

describe Listen::Adapters::Linux do
  if linux?
    if Listen::Adapters::Linux.usable?
      it "is usable on Linux" do
        described_class.should be_usable
      end

      it_should_behave_like 'a filesystem adapter'
      it_should_behave_like 'an adapter that call properly listener#on_change'

      describe '#initialize' do
        context 'when the inotify limit for watched files is not enough' do
          before { INotify::Notifier.any_instance.should_receive(:watch).and_raise(Errno::ENOSPC) }

          it 'fails gracefully' do
            described_class.any_instance.should_receive(:abort).with(described_class::INOTIFY_LIMIT_MESSAGE)
            described_class.new(File.dirname(__FILE__))
          end
        end
      end
    else
      it "isn't usable on Linux with #{RbConfig::CONFIG['RUBY_INSTALL_NAME']}" do
        described_class.should_not be_usable
      end
    end
  end

  if bsd?
    it "isn't usable on BSD" do
      described_class.should_not be_usable
    end
  end

  if mac?
    it "isn't usable on Mac OS X" do
      described_class.should_not be_usable
    end
  end

  if windows?
    it "isn't usable on Windows" do
      described_class.should_not be_usable
    end
  end
end
