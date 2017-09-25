require 'spec_helper'

def run_in_two_threads(proc1, proc2)
  t1 = Thread.new &proc1
  sleep test_latency # t1 must run before t2
  t2 = Thread.new { proc2.call; Thread.kill t1 }
  t2.join(test_latency * 2)
ensure
  Thread.kill t1 if t1
  Thread.kill t2 if t2
end

describe Listen::Turnstile do
  before { @called = false }

  describe '#wait' do
    context 'without a signal' do
      it 'blocks one thread indefinitely' do
        run_in_two_threads lambda {
          subject.wait
          @called = true
        }, lambda {
          sleep test_latency
        }
        @called.should be_false
      end
    end

    context 'with a signal' do
      it 'blocks one thread until it recieves a signal from another thread' do
        run_in_two_threads lambda {
          subject.wait
          @called = true
        }, lambda {
          subject.signal
          sleep test_latency
        }
        @called.should be_true
      end
    end
  end

  describe '#signal' do
    context 'without a wait-call before' do
      it 'does nothing' do
        run_in_two_threads lambda {
          subject.signal
          @called = true
        }, lambda {
          sleep test_latency
        }
        @called.should be_true
      end
    end
  end
end
