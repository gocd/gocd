require 'spec_helper'

describe "expect(...).to have_sym(*args)" do
  it_behaves_like "an RSpec matcher", :valid_value => { :a => 1 },
                                      :invalid_value => {} do
    let(:matcher) { have_key(:a) }
  end

  it "passes if #has_sym?(*args) returns true" do
    expect({:a => "A"}).to have_key(:a)
  end

  it "fails if #has_sym?(*args) returns false" do
    expect {
      expect({:b => "B"}).to have_key(:a)
    }.to fail_with("expected #has_key?(:a) to return true, got false")
  end

  it 'does not include any args in the failure message if no args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?; false; end
    expect {
      expect(o).to have_some_stuff
    }.to fail_with("expected #has_some_stuff? to return true, got false")
  end

  it 'includes multiple args in the failure message if multiple args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?(*_); false; end
    expect {
      expect(o).to have_some_stuff(:a, 7, "foo")
    }.to fail_with('expected #has_some_stuff?(:a, 7, "foo") to return true, got false')
  end

  it "fails if #has_sym?(*args) returns nil" do
    klass = Class.new do
      def has_foo?
      end
    end
    expect {
      expect(klass.new).to have_foo
    }.to fail_with(/expected #has_foo.* to return true, got false/)
  end

  it "fails if target does not respond to #has_sym?" do
    expect {
      expect(Object.new).to have_key(:a)
    }.to raise_error(NoMethodError)
  end

  it "reraises an exception thrown in #has_sym?(*args)" do
    o = Object.new
    def o.has_sym?(*args)
      raise "Funky exception"
    end
    expect {
      expect(o).to have_sym(:foo)
    }.to raise_error("Funky exception")
  end
end

describe "expect(...).not_to have_sym(*args)" do
  it "passes if #has_sym?(*args) returns false" do
    expect({:a => "A"}).not_to have_key(:b)
  end

  it "passes if #has_sym?(*args) returns nil" do
    klass = Class.new do
      def has_foo?
      end
    end
    expect(klass.new).not_to have_foo
  end

  it "fails if #has_sym?(*args) returns true" do
    expect {
      expect({:a => "A"}).not_to have_key(:a)
    }.to fail_with("expected #has_key?(:a) to return false, got true")
  end

  it "fails if target does not respond to #has_sym?" do
    expect {
      expect(Object.new).to have_key(:a)
    }.to raise_error(NoMethodError)
  end

  it "reraises an exception thrown in #has_sym?(*args)" do
    o = Object.new
    def o.has_sym?(*args)
      raise "Funky exception"
    end
    expect {
      expect(o).not_to have_sym(:foo)
    }.to raise_error("Funky exception")
  end

  it 'does not include any args in the failure message if no args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?; true; end
    expect {
      expect(o).not_to have_some_stuff
    }.to fail_with("expected #has_some_stuff? to return false, got true")
  end

  it 'includes multiple args in the failure message if multiple args were given to the matcher' do
    o = Object.new
    def o.has_some_stuff?(*_); true; end
    expect {
      expect(o).not_to have_some_stuff(:a, 7, "foo")
    }.to fail_with('expected #has_some_stuff?(:a, 7, "foo") to return false, got true')
  end
end

describe "has" do
  it "works when the target implements #send" do
    o = {:a => "A"}
    def o.send(*args); raise "DOH! Library developers shouldn't use #send!" end
    expect {
      expect(o).to have_key(:a)
    }.not_to raise_error
  end
end
