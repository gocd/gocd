require File.expand_path('../spec_helper', File.dirname(__FILE__))

describe Rhino::JSError do

  it "works as a StandardError with a message being passed" do
    js_error = Rhino::JSError.new 'an error message'
    lambda { js_error.to_s && js_error.inspect }.should_not raise_error

    js_error.cause.should be nil
    js_error.message.should == 'an error message'
    js_error.javascript_backtrace.should be nil
  end

  it "might wrap a RhinoException wrapped in a NativeException like error" do
    # JRuby's NativeException.new(rhino_e) does not work as it is
    # intended to handle Java exceptions ... no new on the Ruby side
    native_error_class = Class.new(RuntimeError) do

      def initialize(cause)
        @cause = cause
      end

      def cause
        @cause
      end

    end

    rhino_e = Rhino::JS::JavaScriptException.new("42".to_java)
    js_error = Rhino::JSError.new native_error_class.new(rhino_e)
    lambda { js_error.to_s && js_error.inspect }.should_not raise_error

    js_error.cause.should be rhino_e
    js_error.message.should == '42'
    js_error.javascript_backtrace.should_not be nil
  end

  it "keeps the thrown javascript object value" do
    begin
      Rhino::Context.eval "throw { foo: 'bar' }"
    rescue => e
      e.should be_a(Rhino::JSError)
      e.value.should be_a(Rhino::JS::NativeObject)
      e.value['foo'].should == 'bar'
      e.message.should == e.value.to_s
    else
      fail "expected to rescue"
    end
  end

  it "keeps the thrown javascript string value" do
    begin
      Rhino::Context.eval "throw 'mehehehe'"
    rescue => e
      e.should be_a(Rhino::JSError)
      e.value.should == 'mehehehe'
      e.message.should == e.value.to_s
    else
      fail "expected to rescue"
    end
  end

  it "contains the native error as the cause" do
    begin
      Rhino::Context.eval "throw 42"
    rescue => e
      e.cause.should_not be nil
      e.cause.should be_a Java::OrgMozillaJavascript::JavaScriptException
      e.cause.getValue.should == 42
      e.cause.lineNumber.should == 1
      e.cause.sourceName.should == '<eval>'
    else
      fail "expected to rescue"
    end
  end

  it "has a correct javascript backtrace" do
    begin
      Rhino::Context.eval "throw 42"
    rescue => e
      # [ "at <eval>:1", "at org/mozilla/javascript/gen/<eval>:1" ]
      e.javascript_backtrace.should be_a Enumerable
      e.javascript_backtrace.size.should >= 1
      e.javascript_backtrace[0].should == "at <eval>:1"
      e.javascript_backtrace(true).should be_a Enumerable
      e.javascript_backtrace(true).size.should >= 1
      element = e.javascript_backtrace(true)[0]
      element.file_name.should == '<eval>'
      element.function_name.should be nil
      element.line_number.should == 1
    else
      fail "expected to rescue"
    end
  end

  it "contains function name in javascript backtrace" do
    begin
      Rhino::Context.eval "function fortyTwo() { throw 42 }\n fortyTwo()"
    rescue => e
      e.javascript_backtrace.size.should >= 2
      e.javascript_backtrace[0].should == "at <eval>:1 (fortyTwo)"
      expect( e.javascript_backtrace.find { |trace| trace == "at <eval>:2" } ).to_not be nil
    else
      fail "expected to rescue"
    end
  end

  it "backtrace starts with the javascript part" do
    begin
      Rhino::Context.eval "throw 42"
    rescue => e
      e.backtrace.should be_a Array
      e.backtrace[0].should == "at <eval>:1"
      e.backtrace[1].should_not be nil
    else
      fail "expected to rescue"
    end
  end

  it "inspect shows the javascript value" do
    begin
      Rhino::Context.eval "throw '42'"
    rescue => e
      e.inspect.should == '#<Rhino::JSError: 42>'
      e.to_s.should == '42'
    else
      fail "expected to rescue"
    end
  end

  it "wrapps false value correctly" do
    begin
      Rhino::Context.eval "throw false"
    rescue => e
      e.inspect.should == '#<Rhino::JSError: false>'
      e.value.should be false
    else
      fail "expected to rescue"
    end
  end

  it "wrapps null value correctly" do
    begin
      Rhino::Context.eval "throw null"
    rescue => e
      e.inspect.should == '#<Rhino::JSError: null>'
      e.value.should be nil
    else
      fail "expected to rescue"
    end
  end

  it "raises correct error from function#apply" do
    begin
      context = Rhino::Context.new
      context.eval "function foo() { throw 'bar' }"
      context['foo'].apply(nil)
    rescue => e
      e.should be_a Rhino::JSError
      e.value.should == 'bar'
    else
      fail "expected to rescue"
    end
  end

  it "prints info about nested (ruby) error" do
    context = Rhino::Context.new
    klass = Class.new do
      def hello(arg = 42)
        raise RuntimeError, 'hello' if arg != 42
      end
    end
    context[:Hello] = klass.new
    hi = context.eval "( function hi(arg) { Hello.hello(arg); } )"
    begin
      hi.call(24)
    rescue => e
      e.should be_a Rhino::JSError
      e.value.should_not be nil
      e.value.should be_a Rhino::Ruby::Object
      e.value(true).should be_a RuntimeError # unwraps ruby object
      # prints the original message (beyond [ruby RuntimeError]) :
      e.message.should == "RuntimeError: hello"
    else
      fail "expected to rescue"
    end
    # V8::JSError: hello
    #   from (irb):4:in `hello'
    #   from at hi (<eval>:1:28)
    #   from (irb):9
  end

end