# Uber

_Gem-authoring tools like class method inheritance in modules, dynamic options and more._

## Installation

[![Gem Version](https://badge.fury.io/rb/uber.svg)](http://badge.fury.io/rb/uber)

Add this line to your application's Gemfile:

```ruby
gem 'uber'
```

Ready?

# Inheritable Class Attributes

If you want inherited class attributes, this is for you.
This is a mandatory mechanism for creating DSLs.

```ruby
require 'uber/inheritable_attr'

class Song
  extend Uber::InheritableAttr

  inheritable_attr :properties
  self.properties = [:title, :track] # initialize it before using it.
end
```

Note that you have to initialize your class attribute with whatever you want - usually a hash or an array.

```ruby
Song.properties #=> [:title, :track]
```

A subclass of `Song` will have a `clone`d `properties` class attribute.

```ruby
class Hit < Song
end

Hit.properties #=> [:title, :track]
```

The cool thing about the inheritance is: you can work on the inherited attribute without any restrictions. It is a _copy_ of the original.

```ruby
Hit.properties << :number

Hit.properties  #=> [:title, :track, :number]
Song.properties #=> [:title, :track]
```

It's similar to ActiveSupport's `class_attribute` but with a simpler implementation.
It is less dangerous. There are no restrictions for modifying the attribute. [compared to `class_attribute`](http://apidock.com/rails/v4.0.2/Class/class_attribute).

## Uncloneable Values

`::inheritable_attr` will `clone` values to copy them to subclasses. Uber won't attempt to clone `Symbol`, `nil`, `true` and `false` per default.

If you assign any other unclonable value you need to tell Uber that.

```ruby
class Song
  extend Uber::InheritableAttr
  inheritable_attr :properties, clone: false
```

This won't `clone` but simply pass the value on to the subclass.


# Dynamic Options

Implements the pattern of defining configuration options and dynamically evaluating them at run-time.

Usually DSL methods accept a number of options that can either be static values, symbolized instance method names, or blocks (lambdas/Procs).

Here's an example from Cells.

```ruby
cache :show, tags: lambda { Tag.last }, expires_in: 5.mins, ttl: :time_to_live
```

Usually, when processing these options, you'd have to check every option for its type, evaluate the `tags:` lambda in a particular context, call the `#time_to_live` instance method, etc.

This is abstracted in `Uber::Options` and could be implemented like this.

```ruby
require 'uber/options'

options = Uber::Options.new(tags:       lambda { Tag.last },
                            expires_in: 5.mins,
                            ttl:        :time_to_live)
```

Just initialize `Options` with your actual options hash. While this usually happens on class level at compile-time, evaluating the hash happens at run-time.

```ruby
class User < ActiveRecord::Base # this could be any Ruby class.
  # .. lots of code

  def time_to_live(*args)
    "n/a"
  end
end

user = User.find(1)

options.evaluate(user, *args) #=> {tags: "hot", expires_in: 300, ttl: "n/a"}
```

## Evaluating Dynamic Options

To evaluate the options to a real hash, the following happens:

* The `tags:` lambda is executed in `user` context (using `instance_exec`). This allows accessing instance variables or calling instance methods.
* Nothing is done with `expires_in`'s value, it is static.
* `user.time_to_live?` is called as the symbol `:time_to_live` indicates that this is an instance method.

The default behaviour is to treat `Proc`s, lambdas and symbolized `:method` names as dynamic options, everything else is considered static. Optional arguments from the `evaluate` call are passed in either as block or method arguments for dynamic options.

This is a pattern well-known from Rails and other frameworks.

## Uber::Callable

A third way of providing a dynamic option is using a "callable" object. This saves you the unreadable lambda syntax and gives you more flexibility.

```ruby
require 'uber/callable'
class Tags
  include Uber::Callable

  def call(context, *args)
    [:comment]
  end
end
```

By including `Uber::Callable`, uber will invoke the `#call` method on the specified object.

Note how you simply pass an instance of the callable object into the hash instead of a lambda.

```ruby
options = Uber::Options.new(tags: Tags.new)
```

## Evaluating Elements

If you want to evaluate a single option element, use `#eval`.

```ruby
options.eval(:ttl, user) #=> "n/a"
```

## Single Values

Sometimes you don't need an entire hash but a dynamic value, only.

```ruby
value = Uber::Options::Value.new(lambda { |volume| volume < 0 ? 0 : volume })

value.evaluate(context, -122.18) #=> 0
```

Use `Options::Value#evaluate` to handle single values.

If the `Value` represents a lambda and is `evaluate`d with `nil` as context, the block is called in the original context.

```ruby
volume = 99
value = Uber::Options::Value.new(lambda { volume })

value.evaluate(nil) #=> 99
```


## Performance

Evaluating an options hash can be time-consuming. When `Options` contains static elements only, it behaves *and performs* like an ordinary hash.


# Delegates

Using `::delegates` works exactly like the `Forwardable` module in Ruby, with one bonus: It creates the accessors in a module, allowing you to override and call `super` in a user module or class.

```ruby
require 'uber/delegates'

class SongDecorator
  def initialize(song)
    @song = song
  end
  attr_reader :song

  extend Uber::Delegates

  delegates :song, :title, :id # delegate :title and :id to #song.

  def title
    super.downcase # this calls the original delegate #title.
  end
end
```

This creates readers `#title` and `#id` which are delegated to `#song`.

```ruby
song = SongDecorator.new(Song.create(id: 1, title: "HELLOWEEN!"))

song.id #=> 1
song.title #=> "helloween!"
```

Note how `#title` calls the original title and then downcases the string.


# Builder

When included, `Builder` allows to add builder instructions on the class level. These can then be evaluated when instantiating
the class to conditionally build (sub-)classes based on the incoming parameters.

Builders can be defined in three different ways.

## Block Syntax

```ruby
class Listener
  include Uber::Builder

  builds do |params|
    SignedIn if params[:current_user]
  end
end

class SignedIn
end
```

The class then has to use the builder to compute a class name using the build blocks you defined.

```ruby
class Listener
  def self.build(params)
    class_builder.call(params).
    new(params)
  end
end
```

As you can see, it's still up to you to _instantiate_ the object, the builder only helps you computing the concrete class.

```ruby
Listener.build({}) #=> Listener
Listener.build({current_user: @current_user}) #=> SignedIn
```

## Proc Syntax

Setting up builders using the proc syntax allows to call `return` in the block. This is our preferred way to define builders.

```ruby
build ->(params) do
  return SignedIn if params[:current_user]
  return Admin    if params[:admin]
  Default
end
```

This makes the block extremely readable.

## Method Syntax

You can also specify a build method.

```ruby
build :build_method

def self.build_method(params)
  return SignedIn if params[:current_user]
end
```

The method has to be a class method on the building class.

## Build Context

Normally, build blocks and methods are run in the context where they were defined in. You can change that by passing any context object to `class_builder`.

```ruby
def self.build(params)
  class_builder(context_object) # ...
end
```

This allows copying builders to other classes and evaluate blocks in the new context.

## More On Builders

Note that builders are _not_ inherited to subclasses. This allows instantiating subclasses directly without running builders.

This pattern is used in [Cells](https://github.com/apotonick/cells), [Trailblazer](https://github.com/apotonick/trailblazer) and soon Reform and Representable/Roar, too.

# Version

Writing gems against other gems often involves checking for versions and loading appropriate version strategies - e.g. _"is Rails >= 4.0?"_. Uber gives you `Version` for easy, semantic version deciders.

```ruby
version = Uber::Version.new("1.2.3")
```

The API currently gives you `#>=` and `#~`.

```ruby
version >= "1.1" #=> true
version >= "1.3" #=> false
```

The `~` method does a semantic check (currently on major and minor level, only).

```ruby
version.~ "1.1" #=> false
version.~ "1.2" #=> true
version.~ "1.3" #=> false
```

Accepting a list of versions, it makes it simple to check for multiple minor versions.

```ruby
version.~ "1.1", "1.0" #=> false
version.~ "1.1", "1.2" #=> true
```


# Undocumented Features

(Please don't read this!)

* You can enforce treating values as dynamic (or not): `Uber::Options::Value.new("time_to_live", dynamic: true)` will always run `#time_to_live` as an instance method on the context, even though it is not a symbol.

# License

Copyright (c) 2014 by Nick Sutterer <apotonick@gmail.com>

Uber is released under the [MIT License](http://www.opensource.org/licenses/MIT).
