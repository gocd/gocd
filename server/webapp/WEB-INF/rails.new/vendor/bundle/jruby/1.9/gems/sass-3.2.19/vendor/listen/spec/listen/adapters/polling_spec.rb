require 'spec_helper'

describe Listen::Adapters::Polling do
  subject { described_class.new('dir') }

  it_should_behave_like 'a filesystem adapter'

  describe '#initialize' do
    it 'sets the latency to the default polling one' do
      subject.latency.should eq Listen::Adapters::DEFAULT_POLLING_LATENCY
    end
  end

  describe '#poll' do
    let(:listener) { mock(Listen::Listener) }
    let(:callback) { lambda { |changed_dirs, options| @called = true; listener.on_change(changed_dirs, options) } }

    after { subject.stop }

    context 'with one directory to watch' do
      subject { Listen::Adapters::Polling.new('dir', {}, &callback) }

      it 'calls listener.on_change' do
        listener.should_receive(:on_change).at_least(1).times.with(['dir'], :recursive => true)
        subject.start(false)
        subject.wait_for_callback
      end

      it 'calls listener.on_change continuously' do
        subject.latency = 0.001
        listener.should_receive(:on_change).at_least(10).times.with(['dir'], :recursive => true)
        subject.start(false)
        10.times { subject.wait_for_callback }
      end

      it "doesn't call listener.on_change if paused" do
        subject.paused = true
        subject.start(false)
        subject.wait_for_callback
        @called.should be_nil
      end
    end

    context 'with multiple directories to watch' do
      subject { Listen::Adapters::Polling.new(%w{dir1 dir2}, {}, &callback) }

      it 'calls listener.on_change' do
        listener.should_receive(:on_change).at_least(1).times.with(%w{dir1 dir2}, :recursive => true)
        subject.start(false)
        subject.wait_for_callback
      end

      it 'calls listener.on_change continuously' do
        subject.latency = 0.001
        listener.should_receive(:on_change).at_least(10).times.with(%w{dir1 dir2}, :recursive => true)
        subject.start(false)
        10.times { subject.wait_for_callback }
      end

      it "doesn't call listener.on_change if paused" do
        subject.paused = true
        subject.start(false)
        subject.wait_for_callback
        @called.should be_nil
      end
    end
  end
end
