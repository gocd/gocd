Doubles, stubs, and message expectations are all cleaned out after each
example. This ensures that each example can be run in isolation, and in any
order.

### `before(:each)`

It is perfectly fine to set up doubles, stubs, and message expectations in
a `before(:each)` hook, as that hook is executed in the scope of the example:

    before(:each) do
      @account = double('account')
    end

### Do not create doubles, stubs, or message expectations in `before(:all)`

If you do, they'll get cleaned out after the first example, and you will be
very confused as to what's going on in the second example.
