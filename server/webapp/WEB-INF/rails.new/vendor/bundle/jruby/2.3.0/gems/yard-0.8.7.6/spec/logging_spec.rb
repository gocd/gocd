require File.join(File.dirname(__FILE__), "spec_helper")

describe YARD::Logger do
  describe '#show_backtraces' do
    it "should be true if debug level is on" do
      log.show_backtraces = true
      log.enter_level(Logger::DEBUG) do
        log.show_backtraces = false
        log.show_backtraces.should == true
      end
      log.show_backtraces.should == false
    end
  end

  describe '#backtrace' do
    before { log.show_backtraces = true }
    after { log.show_backtraces = false }

    it "should log backtrace in error by default" do
      log.should_receive(:error).with("RuntimeError: foo")
      log.should_receive(:error).with("Stack trace:\n\tline1\n\tline2\n")
      exc = RuntimeError.new("foo")
      exc.set_backtrace(['line1', 'line2'])
      log.enter_level(Logger::INFO) { log.backtrace(exc) }
    end

    it "should allow backtrace to be entered in other modes" do
      log.should_receive(:warn).with("RuntimeError: foo")
      log.should_receive(:warn).with("Stack trace:\n\tline1\n\tline2\n")
      exc = RuntimeError.new("foo")
      exc.set_backtrace(['line1', 'line2'])
      log.enter_level(Logger::INFO) { log.backtrace(exc, :warn) }
    end
  end
end
