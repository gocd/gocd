# 0.0.13

* Add proc syntax for builders. This allows using `return` in the block. Thanks to @dutow for implementing this.

# 0.0.12

* Make it run with Ruby 2.2.

# 0.0.11

* Don't clone nil, false, true and symbols in `::inheritable_attr`.

# 0.0.10

* Builders are _not_ inherited to subclasses. This allows instantiating subclasses directly without running builders.

# 0.0.9

* Add `Uber::Builder`.

# 0.0.8

* Add `Uber::Delegates` that provides delegation that can be overridden and called with `super`.

# 0.0.7

* Add `Uber::Callable` and support for it in `Options::Value`.

# 0.0.6

* Fix `Version#>=` partially.

# 0.0.5

* Add `Uber::Version` for simple gem version deciders.

# 0.0.4

* Fix a bug where `dynamic: true` wouldn't invoke a method but try to run it as a block.

# 0.0.3

* Add `Options` and `Options::Value´ for abstracting dynamic options.

# 0.0.2

* Add `::inheritable_attr`.