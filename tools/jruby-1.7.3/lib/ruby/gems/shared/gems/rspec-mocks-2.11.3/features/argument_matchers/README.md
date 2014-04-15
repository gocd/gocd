### Introduction

Argument matchers can be used:

* In stubs to constrain the scope of the stubbed method

    obj.stub(:foo).with(:bar) do |arg|
      #do something for :bar
    end
    obj.stub(:foo).with(:baz) do |arg|
      #do something for :baz
    end

* In expectations to validate the arguments that should be received in a method call

    #create a double
    obj = double()
      
    #expect a message with given args
    obj.should_receive(:message).with('an argument')

If more control is needed, one can use a block

    obj.should_receive(:message) do |arg1,  arg2|
      # set expectations about the args in this block
      # and optionally set a return value
    end
