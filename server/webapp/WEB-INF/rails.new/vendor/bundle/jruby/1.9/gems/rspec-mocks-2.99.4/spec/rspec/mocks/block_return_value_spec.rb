require "spec_helper"

describe "a double declaration with a block handed to:" do
  # The "receives a block" part is important: 1.8.7 has a bug that reports the
  # wrong arity when a block receives a block.
  it 'forwards all given args to the block, even when it receives a block', :unless => RUBY_VERSION.to_s == '1.8.6' do
    obj = Object.new
    yielded_args = []
    eval("obj.stub(:foo) { |*args, &bl| yielded_args << args }")
    obj.foo(1, 2, 3)

    expect(yielded_args).to eq([[1, 2, 3]])
  end

  describe "should_receive" do
    it "returns the value of executing the block" do
      obj = Object.new
      obj.should_receive(:foo) { 'bar' }
      expect(obj.foo).to eq('bar')
    end

    it "works when a multi-return stub has already been set" do
      obj = Object.new
      return_value = Object.new
      obj.stub(:foo).and_return(return_value, nil)
      obj.should_receive(:foo) { return_value }
      expect(obj.foo).to be(return_value)
    end
  end

  describe "stub" do
    it "returns the value of executing the block" do
      obj = Object.new
      obj.stub(:foo) { 'bar' }
      expect(obj.foo).to eq('bar')
    end

    it "does not complain if a lambda block and mismatched arguments are passed" do
      RSpec.stub :deprecate
      obj = Object.new
      obj.stub(:foo, &lambda { 'bar' })
      expect(obj.foo(1, 2)).to eq('bar')
    end

    it 'warns of deprection if argument counts dont match' do
      expect(RSpec).to receive(:deprecate) do |message, opts|
        expect(message).to eq "stubbing implementations with mismatched arity"
        expect(opts[:call_site]).to match %r%/spec/rspec/mocks/block_return_value_spec.rb%
      end
      obj = Object.new
      obj.stub(:foo, &lambda { 'bar' })
      expect(obj.foo(1, 2)).to eq('bar')
    end
  end

  describe "with" do
    it "returns the value of executing the block" do
      obj = Object.new
      obj.stub(:foo).with('baz') { 'bar' }
      expect(obj.foo('baz')).to eq('bar')
    end

    it "does not complain if a lambda block and mismatched arguments are passed" do
      RSpec.stub :deprecate
      obj = Object.new
      obj.stub(:foo).with(1, 2, &lambda { 'bar' })
      expect(obj.foo(1, 2)).to eq('bar')
    end

    it 'warns of deprection if argument counts dont match' do
      expect(RSpec).to receive(:deprecate) do |message, opts|
        expect(message).to eq "stubbing implementations with mismatched arity"
        expect(opts[:call_site]).to match %r%/spec/rspec/mocks/block_return_value_spec.rb%
      end
      obj = Object.new
      obj.stub(:foo).with(1, 2, &lambda { 'bar' })
      expect(obj.foo(1, 2)).to eq('bar')
    end

    it 'warns of deprecation when provided block but no arguments' do
      expect(RSpec).to receive(:deprecate) do |message, opts|
        expect(message).to match(/Using the return value of a `with` block/)
      end
      obj = Object.new
      obj.stub(:foo).with {|x| 'baz' }.and_return('bar')
      expect(obj.foo(1)).to eq('bar')
    end

    it 'includes callsite in deprecation of provided block but no arguments' do
      obj = Object.new
      expect_deprecation_with_call_site __FILE__, __LINE__ + 1
      obj.stub(:foo).with {|x| 'baz' }.and_return('bar')
      expect(obj.foo(1)).to eq('bar')
    end
  end

  %w[once twice ordered].each do |method|
    describe method do
      it "returns the value of executing the block" do
        obj = Object.new
        obj.stub(:foo).send(method) { 'bar' }
        expect(obj.foo).to eq('bar')
      end

      it "does not complain if a lambda block and mismatched arguments are passed" do
        RSpec.stub :deprecate
        obj = Object.new
        obj.stub(:foo).send(method, &lambda { 'bar' })
        expect(obj.foo(1, 2)).to eq('bar')
      end

      it 'warns of deprection if argument counts dont match' do
        expect(RSpec).to receive(:deprecate) do |message, opts|
          expect(message).to eq "stubbing implementations with mismatched arity"
          expect(opts[:call_site]).to match %r%/spec/rspec/mocks/block_return_value_spec.rb%
        end
        obj = Object.new
        obj.stub(:foo).send(method, &lambda { 'bar' })
        expect(obj.foo(1, 2)).to eq('bar')
      end
    end
  end

  describe 'and_return' do
    before do
      allow_deprecation
    end

    it "returns the value of executing the block" do
      obj = Object.new
      obj.stub(:foo).and_return { 'bar' }
      expect(obj.foo).to eq('bar')
    end

    it "does not complain if a lambda block and mismatched arguments are passed" do
      obj = Object.new
      obj.stub(:foo).and_return(&lambda { 'bar' })
      expect(obj.foo(1, 2)).to eq('bar')
    end

    it 'warns of deprecation of `and_return { value }`' do
      expect_deprecation_with_call_site(__FILE__, __LINE__ + 3, '`and_return { value }`')

      obj = Object.new
      obj.stub(:foo).and_return { 'bar' }
      expect(obj.foo(1, 2)).to eq('bar')
    end

    it 'warns of deprection if argument counts dont match' do
      warned = false

      expect(RSpec).to receive(:deprecate).at_least(1) do |message, opts|
        next unless message == "stubbing implementations with mismatched arity"
        expect(opts[:call_site]).to match %r%/spec/rspec/mocks/block_return_value_spec.rb%
        warned = true
      end

      obj = Object.new
      obj.stub(:foo).and_return(&lambda { 'bar' })
      expect(obj.foo(1, 2)).to eq('bar')

      expect(warned).to be true
    end
  end

  describe 'any_number_of_times' do
    before do
      RSpec.stub(:deprecate)
    end

    it "warns about deprecation" do
      expect(RSpec).to receive(:deprecate).with("any_number_of_times", :replacement => "stub")
      Object.new.stub(:foo).any_number_of_times { 'bar' }
    end

    it "returns the value of executing the block" do
      obj = Object.new
      obj.stub(:foo).any_number_of_times { 'bar' }
      expect(obj.foo).to eq('bar')
    end
  end

  describe "times" do
    it "returns the value of executing the block" do
      obj = Object.new
      obj.stub(:foo).at_least(1).times { 'bar' }
      expect(obj.foo('baz')).to eq('bar')
    end
  end
end
