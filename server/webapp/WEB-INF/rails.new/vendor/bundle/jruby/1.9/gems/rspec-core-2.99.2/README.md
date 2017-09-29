# rspec-core [![Build Status](https://secure.travis-ci.org/rspec/rspec-core.png?branch=master)](http://travis-ci.org/rspec/rspec-core) [![Code Climate](https://codeclimate.com/github/rspec/rspec-core.png)](https://codeclimate.com/github/rspec/rspec-core)

rspec-core provides the structure for writing executable examples of how your
code should behave, and an `rspec` command with tools to constrain which
examples get run and tailor the output.

## install

    gem install rspec      # for rspec-core, rspec-expectations, rspec-mocks
    gem install rspec-core # for rspec-core only
    rspec --help

## basic structure

RSpec uses the words "describe" and "it" so we can express concepts like a conversation:

    "Describe an order."
    "It sums the prices of its line items."

```ruby
describe Order do
  it "sums the prices of its line items" do
    order = Order.new
    order.add_entry(LineItem.new(:item => Item.new(
      :price => Money.new(1.11, :USD)
    )))
    order.add_entry(LineItem.new(:item => Item.new(
      :price => Money.new(2.22, :USD),
      :quantity => 2
    )))
    expect(order.total).to eq(Money.new(5.55, :USD))
  end
end
```

The `describe` method creates an [ExampleGroup](http://rubydoc.info/gems/rspec-core/RSpec/Core/ExampleGroup).  Within the
block passed to `describe` you can declare examples using the `it` method.

Under the hood, an example group is a class in which the block passed to
`describe` is evaluated. The blocks passed to `it` are evaluated in the
context of an _instance_ of that class.

## nested groups

You can also declare nested nested groups using the `describe` or `context`
methods:

```ruby
describe Order do
  context "with no items" do
    it "behaves one way" do
      # ...
    end
  end

  context "with one item" do
    it "behaves another way" do
      # ...
    end
  end
end
```

## aliases

You can declare example groups using either `describe` or `context`, though
only `describe` is available at the top level.

You can declare examples within a group using any of `it`, `specify`, or
`example`.

## shared examples and contexts

Declare a shared example group using `shared_examples`, and then include it
in any group using `include_examples`.

```ruby
shared_examples "collections" do |collection_class|
  it "is empty when first created" do
    expect(collection_class.new).to be_empty
  end
end

describe Array do
  include_examples "collections", Array
end

describe Hash do
  include_examples "collections", Hash
end
```

Nearly anything that can be declared within an example group can be declared
within a shared example group. This includes `before`, `after`, and `around`
hooks, `let` declarations, and nested groups/contexts.

You can also use the names `shared_context` and `include_context`. These are
pretty much the same as `shared_examples` and `include_examples`, providing
more accurate naming when you share hooks, `let` declarations, helper methods,
etc, but no examples.

## metadata

rspec-core stores a metadata hash with every example and group, which
contains their descriptions, the locations at which they were
declared, etc, etc. This hash powers many of rspec-core's features,
including output formatters (which access descriptions and locations),
and filtering before and after hooks.

Although you probably won't ever need this unless you are writing an
extension, you can access it from an example like this:

```ruby
it "does something" do
  expect(example.metadata[:description]).to eq("does something")
end
```

### `described_class`

When a class is passed to `describe`, you can access it from an example
using the `described_class` method, which is a wrapper for
`example.metadata[:described_class]`.

```ruby
describe Widget do
  example do
    expect(described_class).to equal(Widget)
  end
end
```

This is useful in extensions or shared example groups in which the specific
class is unknown. Taking the collections shared example group from above, we can
clean it up a bit using `described_class`:

```ruby
shared_examples "collections" do
  it "is empty when first created" do
    expect(described_class.new).to be_empty
  end
end

describe Array do
  include_examples "collections"
end

describe Hash do
  include_examples "collections"
end
```

## the `rspec` command

When you install the rspec-core gem, it installs the `rspec` executable,
which you'll use to run rspec. The `rspec` command comes with many useful
options.
Run `rspec --help` to see the complete list.

## store command line options `.rspec`

You can store command line options in a `.rspec` file in the project's root
directory, and the `rspec` command will read them as though you typed them on
the command line.

## autotest integration

rspec-core ships with an Autotest extension, which is loaded automatically if
there is a `.rspec` file in the project's root directory.

## rcov integration

rcov is best integrated via the [rcov rake
task](http://www.rubydoc.info/github/relevance/rcov/master/Rcov/RcovTask).

rcov can also be integrated via the rspec rake task, but it requires a bit
more setup:

```ruby
# Rakefile
require 'rspec/core/rake_task'

RSpec::Core::RakeTask.new(:spec) do |config|
  config.rcov = true
end

task :default => :spec

# spec/spec_helper.rb
require 'rspec/autorun' # **add this**
```

## get started

Start with a simple example of behavior you expect from your system. Do
this before you write any implementation code:

```ruby
# in spec/calculator_spec.rb
describe Calculator do
  describe '#add' do
    it 'returns the sum of its arguments' do
      expect(Calculator.new.add(1, 2)).to eq(3)
    end
  end
end
```

Run this with the rspec command, and watch it fail:

```
$ rspec spec/calculator_spec.rb
./spec/calculator_spec.rb:1: uninitialized constant Calculator
```

Implement the simplest solution:

```ruby
# in lib/calculator.rb
class Calculator
  def add(a,b)
    a + b
  end
end
```

Be sure to require the implementation file in the spec:

```ruby
# in spec/calculator_spec.rb
# - RSpec adds ./lib to the $LOAD_PATH
require "calculator"
```

Now run the spec again, and watch it pass:

```
$ rspec spec/calculator_spec.rb
.

Finished in 0.000315 seconds
1 example, 0 failures
```

Use the `documentation` formatter to see the resulting spec:

```
$ rspec spec/calculator_spec.rb --format doc
Calculator
  #add
    returns the sum of its arguments

Finished in 0.000379 seconds
1 example, 0 failures
```

## Also see

* [http://github.com/rspec/rspec](http://github.com/rspec/rspec)
* [http://github.com/rspec/rspec-expectations](http://github.com/rspec/rspec-expectations)
* [http://github.com/rspec/rspec-mocks](http://github.com/rspec/rspec-mocks)
