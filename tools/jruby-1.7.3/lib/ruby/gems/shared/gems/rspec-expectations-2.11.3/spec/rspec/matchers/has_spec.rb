require 'spec_helper'

describe "should have_sym(*args)" do
  it_behaves_like "an RSpec matcher", :valid_value => { :a => 1 },
                                      :invalid_value => {} do
    let(:matcher) { have_key(:a) }
  end

  it "passes if #has_sym?(*args) returns true" do
    {:a => "A"}.should have_key(:a)
  end

  it "fails if #has_sym?(*args) returns false" do
    lambda {
      {:b => "B"}.should have_key(:a)
    }.should fail_with("expected #has_key?(:a) to return true, got false")
  end

  it 'does not include any args in the failure message if no args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?; false; end
    expect {
      o.should have_some_stuff
    }.to fail_with("expected #has_some_stuff? to return true, got false")
  end

  it 'includes multiple args in the failure message if multiple args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?(*_); false; end
    expect {
      o.should have_some_stuff(:a, 7, "foo")
    }.to fail_with('expected #has_some_stuff?(:a, 7, "foo") to return true, got false')
  end

  it "fails if #has_sym?(*args) returns nil" do
    klass = Class.new do
      def has_foo?
      end
    end
    lambda {
      klass.new.should have_foo
    }.should fail_with(/expected #has_foo.* to return true, got false/)
  end

  it "fails if target does not respond to #has_sym?" do
    lambda {
      Object.new.should have_key(:a)
    }.should raise_error(NoMethodError)
  end

  it "reraises an exception thrown in #has_sym?(*args)" do
    o = Object.new
    def o.has_sym?(*args)
      raise "Funky exception"
    end
    lambda { o.should have_sym(:foo) }.should raise_error("Funky exception")
  end
end

describe "should_not have_sym(*args)" do
  it "passes if #has_sym?(*args) returns false" do
    {:a => "A"}.should_not have_key(:b)
  end

  it "passes if #has_sym?(*args) returns nil" do
    klass = Class.new do
      def has_foo?
      end
    end
    klass.new.should_not have_foo
  end

  it "fails if #has_sym?(*args) returns true" do
    lambda {
      {:a => "A"}.should_not have_key(:a)
    }.should fail_with("expected #has_key?(:a) to return false, got true")
  end

  it "fails if target does not respond to #has_sym?" do
    lambda {
      Object.new.should have_key(:a)
    }.should raise_error(NoMethodError)
  end

  it "reraises an exception thrown in #has_sym?(*args)" do
    o = Object.new
    def o.has_sym?(*args)
      raise "Funky exception"
    end
    lambda { o.should_not have_sym(:foo) }.should raise_error("Funky exception")
  end

  it 'does not include any args in the failure message if no args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?; true; end
    expect {
      o.should_not have_some_stuff
    }.to fail_with("expected #has_some_stuff? to return false, got true")
  end

  it 'includes multiple args in the failure message if multiple args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?(*_); true; end
    expect {
      o.should_not have_some_stuff(:a, 7, "foo")
    }.to fail_with('expected #has_some_stuff?(:a, 7, "foo") to return false, got true')
  end
end

describe "has" do
  it "works when the target implements #send" do
    o = {:a => "A"}
    def o.send(*args); raise "DOH! Library developers shouldn't use #send!" end
    lambda {
      o.should have_key(:a)
    }.should_not raise_error
  end
end
