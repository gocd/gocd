### Stub return values

    # create a double
    obj = double()

    # specify a return value using `:expect` syntax
    allow(obj).to receive(:message) { :value }
    allow(obj).to receive(:message).and_return(:value)

    # specify a return value using `:should` syntax
    obj.stub(:message) { :value }
    obj.stub(:message => :value)
    obj.stub(:message).and_return(:value)

These forms are somewhat interchangeable. The difference is that the
block contents are evaluated lazily when the `obj` receives the
`message` message, whereas the others are evaluated as they are read.

### Fake implementation

    allow(obj).to receive(:message) do |arg1, arg2|
      # set expectations about the args in this block
      # and/or return  value
    end

    obj.stub(:message) do |arg1, arg2|
      # set expectations about the args in this block
      # and/or return a value
    end

### Raising/Throwing

    allow(obj).to receive(:message).and_raise("this error")
    allow(obj).to receive(:message).and_throw(:this_symbol)

    obj.stub(:message).and_raise("this error")
    obj.stub(:message).and_throw(:this_symbol)

You can also use the block format:

    allow(obj).to receive(:message) { raise "this error" }
    allow(obj).to receive(:message) { throw :this_symbol }

    obj.stub(:message) { raise "this error" }
    obj.stub(:message) { throw :this_symbol }

### Argument constraints

#### Explicit arguments

    allow(obj).to receive(:message).with('an argument') { ... }

    obj.stub(:message).with('an argument') { ... }
    obj.stub(:message).with('more_than', 'one_argument') { ... }

#### Argument matchers

    allow(obj).to receive(:message).with(anything()) { ... }
    allow(obj).to receive(:message).with(an_instance_of(Money)) { ... }
    allow(obj).to receive(:message).with(hash_including(:a => 'b')) { ... }
    allow(obj).to receive(:message).with(array_including(1,2,3)) { ... }
    # or
    allow(obj).to receive(:message).with(array_including([1,2,3])) { ... }

    obj.stub(:message).with(anything()) { ... }
    obj.stub(:message).with(an_instance_of(Money)) { ... }
    obj.stub(:message).with(hash_including(:a => 'b')) { ... }

#### Regular expressions

    allow(obj).to receive(:message).with(/abc/) { ... }

    obj.stub(:message).with(/abc/) { ... }
