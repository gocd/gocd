require 'spec_helper'

module WrapDeprecationCallSite
  def expect_deprecation_with_call_site(file, line)
    actual_call_site = nil
    allow(RSpec.configuration.reporter).to receive(:deprecation) do |options|
      actual_call_site = options[:call_site]
    end

    yield

    expect(actual_call_site).to match([file, line].join(':'))
  end
end

describe "expect { ... }.to raise_error" do
  it_behaves_like("an RSpec matcher", :valid_value => lambda { raise "boom" },
                                      :invalid_value => lambda { }) do
    let(:matcher) { raise_error(/boom/) }
  end

  it "passes if anything is raised" do
    expect {raise}.to raise_error
  end

  it "passes if an error instance is expected" do
    s = StandardError.new
    expect {raise s}.to raise_error(s)
  end

  it "fails if a different error instance is thrown from the one that is expected" do
    s = StandardError.new("Error 1")
    to_raise = StandardError.new("Error 2")
    expect do
      expect {raise to_raise}.to raise_error(s)
    end.to fail_with(Regexp.new("expected #{s.inspect}, got #{to_raise.inspect} with backtrace"))
  end

  it "passes if an error class is expected and an instance of that class is thrown" do
    s = StandardError.new :bees

    expect { raise s }.to raise_error(StandardError)
  end

  it "fails if nothing is raised" do
    expect {
      expect {}.to raise_error
    }.to fail_with("expected Exception but nothing was raised")
  end
end

describe "raise_exception aliased to raise_error" do
  it "passes if anything is raised" do
    expect {raise}.to raise_exception
  end
end

describe "expect { ... }.to raise_error {|err| ... }" do
  it "passes if there is an error" do
    ran = false
    expect { non_existent_method }.to raise_error {|e|
      ran = true
    }
    expect(ran).to be_true
  end

  it "passes the error to the block" do
    error = nil
    expect { non_existent_method }.to raise_error {|e|
      error = e
    }
    expect(error).to be_kind_of(NameError)
  end
end

describe "expect { ... }.not_to raise_error" do

  context "with a specific error class" do
    it "is deprecated" do
      RSpec.should_receive :deprecate
      expect {"bees"}.not_to raise_error(RuntimeError)
    end
  end

  context "with no specific error class" do
    it "is not deprecated" do
      run = nil
      allow(RSpec).to receive(:deprecate) { run = true }
      expect {"bees"}.not_to raise_error
      expect(run).to be_nil
    end

    it "passes if nothing is raised" do
      expect {}.not_to raise_error
    end

    it "fails if anything is raised" do
      expect {
        expect { raise RuntimeError, "example message" }.not_to raise_error
      }.to fail_with(/expected no Exception, got #<RuntimeError: example message>/)
    end

    it 'includes the backtrace of the error that was raised in the error message' do
      expect {
        expect { raise "boom" }.not_to raise_error
      }.to raise_error { |e|
        backtrace_line = "#{File.basename(__FILE__)}:#{__LINE__ - 2}"
        expect(e.message).to include("with backtrace", backtrace_line)
      }
    end

    it 'formats the backtrace using the configured backtrace formatter' do
      RSpec::Matchers.configuration.backtrace_formatter.
        stub(:format_backtrace).
        and_return("formatted-backtrace")

      expect {
        expect { raise "boom" }.not_to raise_error
      }.to raise_error { |e|
        expect(e.message).to include("with backtrace", "formatted-backtrace")
      }
    end
  end
end

describe "expect { ... }.to raise_error(message)" do
  it "passes if RuntimeError is raised with the right message" do
    expect {raise 'blah'}.to raise_error('blah')
  end

  it "passes if RuntimeError is raised with a matching message" do
    expect {raise 'blah'}.to raise_error(/blah/)
  end

  it "passes if any other error is raised with the right message" do
    expect {raise NameError.new('blah')}.to raise_error('blah')
  end

  it "fails if RuntimeError error is raised with the wrong message" do
    expect do
      expect {raise 'blarg'}.to raise_error('blah')
    end.to fail_with(/expected Exception with \"blah\", got #<RuntimeError: blarg>/)
  end

  it "fails if any other error is raised with the wrong message" do
    expect do
      expect {raise NameError.new('blarg')}.to raise_error('blah')
    end.to fail_with(/expected Exception with \"blah\", got #<NameError: blarg>/)
  end

  it 'includes the backtrace of any other error in the failure message' do
    expect {
      expect { raise "boom" }.to raise_error(ArgumentError)
    }.to raise_error { |e|
      backtrace_line = "#{File.basename(__FILE__)}:#{__LINE__ - 2}"
      expect(e.message).to include("with backtrace", backtrace_line)
    }
  end
end

describe "expect { ... }.not_to raise_error(message)" do
  include WrapDeprecationCallSite

  before do
    allow(RSpec).to receive(:deprecate)
  end

  it "is deprecated" do
    expect(RSpec).to receive(:deprecate).with(
      /not_to raise_error\(message\)/,
      :replacement =>"`expect { }.not_to raise_error` (with no args)"
    )
    expect {raise 'blarg'}.not_to raise_error('blah')
  end

  it 'reports the line number of the deprecated syntax' do
    allow(RSpec).to receive(:deprecate).and_call_original

    expect_deprecation_with_call_site(__FILE__, __LINE__ + 1) do
      expect {raise 'blarg'}.not_to raise_error('blah')
    end
  end

  it "passes if RuntimeError error is raised with the different message" do
    expect {raise 'blarg'}.not_to raise_error('blah')
  end

  it "passes if any other error is raised with the wrong message" do
    expect {raise NameError.new('blarg')}.not_to raise_error('blah')
  end

  it "fails if RuntimeError is raised with message" do
    expect do
      expect {raise 'blah'}.not_to raise_error('blah')
    end.to fail_with(/expected no Exception with "blah", got #<RuntimeError: blah>/)
  end

  it "fails if any other error is raised with message" do
    expect do
      expect {raise NameError.new('blah')}.not_to raise_error('blah')
    end.to fail_with(/expected no Exception with "blah", got #<NameError: blah>/)
  end
end

describe "expect { ... }.to raise_error(NamedError)" do
  it "passes if named error is raised" do
    expect { non_existent_method }.to raise_error(NameError)
  end

  it "fails if nothing is raised" do
    expect {
      expect { }.to raise_error(NameError)
    }.to fail_with(/expected NameError but nothing was raised/)
  end

  it "fails if another error is raised (NameError)" do
    expect {
      expect { raise RuntimeError, "example message" }.to raise_error(NameError)
    }.to fail_with(/expected NameError, got #<RuntimeError: example message>/)
  end

  it "fails if another error is raised (NameError)" do
    expect {
      expect { load "non/existent/file" }.to raise_error(NameError)
    }.to fail_with(/expected NameError, got #<LoadError/)
  end
end

describe "expect { ... }.not_to raise_error(NamedError)" do
  include WrapDeprecationCallSite

  before do
    allow(RSpec).to receive(:deprecate)
  end

  it "is deprecated" do
    expect(RSpec).to receive(:deprecate).with(
      /not_to raise_error\(SpecificErrorClass\)/,
      :replacement =>"`expect { }.not_to raise_error` (with no args)"
    )
    expect { }.not_to raise_error(NameError)
  end

  it 'reports the line number of the deprecated syntax' do
    allow(RSpec).to receive(:deprecate).and_call_original
    expect_deprecation_with_call_site(__FILE__, __LINE__ + 1) do
      expect { }.not_to raise_error(NameError)
    end
  end

  it "passes if nothing is raised" do
    expect { }.not_to raise_error(NameError)
  end

  it "passes if another error is raised" do
    expect { raise }.not_to raise_error(NameError)
  end

  it "fails if named error is raised" do
    expect {
      expect { 1 + 'b' }.not_to raise_error(TypeError)
    }.to fail_with(/expected no TypeError, got #<TypeError: String can't be/)
  end
end

describe "expect { ... }.to raise_error(NamedError, error_message) with String" do
  it "passes if named error is raised with same message" do
    expect { raise "example message" }.to raise_error(RuntimeError, "example message")
  end

  it "fails if nothing is raised" do
    expect {
      expect {}.to raise_error(RuntimeError, "example message")
    }.to fail_with(/expected RuntimeError with \"example message\" but nothing was raised/)
  end

  it "fails if incorrect error is raised" do
    expect {
      expect { raise RuntimeError, "example message" }.to raise_error(NameError, "example message")
    }.to fail_with(/expected NameError with \"example message\", got #<RuntimeError: example message>/)
  end

  it "fails if correct error is raised with incorrect message" do
    expect {
      expect { raise RuntimeError.new("not the example message") }.to raise_error(RuntimeError, "example message")
    }.to fail_with(/expected RuntimeError with \"example message\", got #<RuntimeError: not the example message/)
  end
end

describe "expect { ... }.not_to raise_error(NamedError, error_message) with String" do
  include WrapDeprecationCallSite

  before do
    allow(RSpec).to receive(:deprecate)
  end

  it "is deprecated" do
    expect(RSpec).to receive(:deprecate).with(
      /not_to raise_error\(SpecificErrorClass, message\)/,
      :replacement =>"`expect { }.not_to raise_error` (with no args)"
    )
    expect {}.not_to raise_error(RuntimeError, "example message")
  end

  it 'reports the line number of the deprecated syntax' do
    allow(RSpec).to receive(:deprecate).and_call_original
    expect_deprecation_with_call_site(__FILE__, __LINE__ + 1) do
      expect {}.not_to raise_error(RuntimeError, "example message")
    end
  end

  it "passes if nothing is raised" do
    expect {}.not_to raise_error(RuntimeError, "example message")
  end

  it "passes if a different error is raised" do
    expect { raise }.not_to raise_error(NameError, "example message")
  end

  it "passes if same error is raised with different message" do
    expect { raise RuntimeError.new("not the example message") }.not_to raise_error(RuntimeError, "example message")
  end

  it "fails if named error is raised with same message" do
    expect {
      expect { raise "example message" }.not_to raise_error(RuntimeError, "example message")
    }.to fail_with(/expected no RuntimeError with \"example message\", got #<RuntimeError: example message>/)
  end
end

describe "expect { ... }.to raise_error(NamedError, error_message) with Regexp" do
  it "passes if named error is raised with matching message" do
    expect { raise "example message" }.to raise_error(RuntimeError, /ample mess/)
  end

  it "fails if nothing is raised" do
    expect {
      expect {}.to raise_error(RuntimeError, /ample mess/)
    }.to fail_with(/expected RuntimeError with message matching \/ample mess\/ but nothing was raised/)
  end

  it "fails if incorrect error is raised" do
    expect {
      expect { raise RuntimeError, "example message" }.to raise_error(NameError, /ample mess/)
    }.to fail_with(/expected NameError with message matching \/ample mess\/, got #<RuntimeError: example message>/)
  end

  it "fails if correct error is raised with incorrect message" do
    expect {
      expect { raise RuntimeError.new("not the example message") }.to raise_error(RuntimeError, /less than ample mess/)
    }.to fail_with(/expected RuntimeError with message matching \/less than ample mess\/, got #<RuntimeError: not the example message>/)
  end
end

describe "expect { ... }.not_to raise_error(NamedError, error_message) with Regexp" do
  before do
    allow(RSpec).to receive(:deprecate)
  end

  it "is deprecated" do
    expect(RSpec).to receive(:deprecate)
    expect {}.not_to raise_error(RuntimeError, /ample mess/)
  end

  it "passes if nothing is raised" do
    expect {}.not_to raise_error(RuntimeError, /ample mess/)
  end

  it "passes if a different error is raised" do
    expect { raise }.not_to raise_error(NameError, /ample mess/)
  end

  it "passes if same error is raised with non-matching message" do
    expect { raise RuntimeError.new("non matching message") }.not_to raise_error(RuntimeError, /ample mess/)
  end

  it "fails if named error is raised with matching message" do
    expect {
      expect { raise "example message" }.not_to raise_error(RuntimeError, /ample mess/)
    }.to fail_with(/expected no RuntimeError with message matching \/ample mess\/, got #<RuntimeError: example message>/)
  end
end

describe "expect { ... }.to raise_error(NamedError, error_message) { |err| ... }" do
  it "yields exception if named error is raised with same message" do
    ran = false

    expect {
      raise "example message"
    }.to raise_error(RuntimeError, "example message") { |err|
      ran = true
      expect(err.class).to eq RuntimeError
      expect(err.message).to eq "example message"
    }

    expect(ran).to be(true)
  end

  it "yielded block fails on it's own right" do
    ran, passed = false, false

    expect {
      expect {
        raise "example message"
      }.to raise_error(RuntimeError, "example message") { |err|
        ran = true
        expect(5).to eq 4
        passed = true
      }
    }.to fail_with(/expected: 4/m)

    expect(ran).to    be_true
    expect(passed).to be_false
  end

  it "does NOT yield exception if no error was thrown" do
    ran = false

    expect {
      expect {}.to raise_error(RuntimeError, "example message") { |err|
        ran = true
      }
    }.to fail_with(/expected RuntimeError with \"example message\" but nothing was raised/)

    expect(ran).to eq false
  end

  it "does not yield exception if error class is not matched" do
    ran = false

    expect {
      expect {
        raise "example message"
      }.to raise_error(SyntaxError, "example message") { |err|
        ran = true
      }
    }.to fail_with(/expected SyntaxError with \"example message\", got #<RuntimeError: example message>/)

    expect(ran).to eq false
  end

  it "does NOT yield exception if error message is not matched" do
    ran = false

    expect {
      expect {
        raise "example message"
      }.to raise_error(RuntimeError, "different message") { |err|
        ran = true
      }
    }.to fail_with(/expected RuntimeError with \"different message\", got #<RuntimeError: example message>/)

    expect(ran).to eq false
  end
end

describe "expect { ... }.not_to raise_error(NamedError, error_message) { |err| ... }" do
  before do
    allow(RSpec).to receive(:deprecate)
  end

  it "is deprecated" do
    expect(RSpec).to receive(:deprecate)
    expect {}.not_to raise_error(RuntimeError, "example message") { |err| }
  end

  it "passes if nothing is raised" do
    ran = false

    expect {}.not_to raise_error(RuntimeError, "example message") { |err|
      ran = true
    }

    expect(ran).to eq false
  end

  it "passes if a different error is raised" do
    ran = false

    expect { raise }.not_to raise_error(NameError, "example message") { |err|
      ran = true
    }

    expect(ran).to eq false
  end

  it "passes if same error is raised with different message" do
    ran = false

    expect {
      raise RuntimeError.new("not the example message")
    }.not_to raise_error(RuntimeError, "example message") { |err|
      ran = true
    }

    expect(ran).to eq false
  end

  it "fails if named error is raised with same message" do
    ran = false

    expect {
      expect {
        raise "example message"
      }.not_to raise_error(RuntimeError, "example message") { |err|
        ran = true
      }
    }.to fail_with(/expected no RuntimeError with \"example message\", got #<RuntimeError: example message>/)

    expect(ran).to eq false
  end

  it 'skips the error verification block when using the expect {...}.to syntax' do
    ran = false

    expect {
      expect {
        raise "example message"
      }.not_to raise_error(RuntimeError, "example message") { |err|
        ran = true
      }
    }.to fail_with(/expected no RuntimeError with \"example message\", got #<RuntimeError: example message>/)

    expect(ran).to eq false
  end
end

describe "misuse of raise_error, with (), not {}" do
  it "fails with warning" do
    ::Kernel.should_receive(:warn).with(/`raise_error` was called with non-proc object 1\.7/)
    expect {
      expect(Math.sqrt(3)).to raise_error
    }.to fail_with(/nothing was raised/)
  end
end
