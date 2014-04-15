### Stub return values

    # create a double
    obj = double()

    # specify a return value
    obj.stub(:message) { :value }
    obj.stub(:message => :value)
    obj.stub(:message).and_return(:value)

These forms are somewhat interchangeable. The difference is that the
block contents are evaluated lazily when the `obj` receives the
`message` message, whereas the others are evaluated as they are read.

### Fake implementation

    obj.stub(:message) do |arg1, arg2|
      # set expectations about the args in this block
      # and/or set a return value
    end

### Raising/Throwing

    obj.stub(:message).and_raise("this error")
    obj.stub(:message).and_throw(:this_symbol)

You can also use the block format:

    obj.stub(:message) { raise "this error" }
    obj.stub(:message) { throw :this_symbol }

### Argument constraints
   
#### Explicit arguments

    obj.stub(:message).with('an argument') { ... }
    obj.stub(:message).with('more_than', 'one_argument') { ... }

#### Argument matchers

    obj.stub(:message).with(anything()) { ... }
    obj.stub(:message).with(an_instance_of(Money)) { ... }
    obj.stub(:message).with(hash_including(:a => 'b')) { ... }

#### Regular expressions

    obj.stub(:message).with(/abc/) { ... }
