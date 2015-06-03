# ## Built-in Formatters
#
# * progress (default) - prints dots for passing examples, `F` for failures, `*` for pending
# * documentation - prints the docstrings passed to `describe` and `it` methods (and their aliases)
# * html
# * textmate - html plus links to editor
# * json - useful for archiving data for subsequent analysis
#
# The progress formatter is the default, but you can choose any one or more of
# the other formatters by passing with the `--format` (or `-f` for short)
# command-line option, e.g.
#
#     rspec --format documentation
#
# You can also send the output of multiple formatters to different streams, e.g.
#
#     rspec --format documentation --format html --out results.html
#
# This example sends the output of the documentation formatter to `$stdout`, and
# the output of the html formatter to results.html.
#
# ## Custom Formatters
#
# You can tell RSpec to use a custom formatter by passing its path and name to
# the `rspec` commmand. For example, if you define MyCustomFormatter in
# path/to/my_custom_formatter.rb, you would type this command:
#
#     rspec --require path/to/my_custom_formatter.rb --format MyCustomFormatter
#
# The reporter calls every formatter with this protocol:
#
# * `start(expected_example_count)`
# * zero or more of the following
#   * `example_group_started(group)`
#   * `example_started(example)`
#   * `example_passed(example)`
#   * `example_failed(example)`
#   * `example_pending(example)`
#   * `message(string)`
# * `stop`
# * `start_dump`
# * `dump_pending`
# * `dump_failures`
# * `dump_summary(duration, example_count, failure_count, pending_count)`
# * `seed(value)`
# * `close`
#
# You can either implement all of those methods or subclass
# `RSpec::Core::Formatters::BaseTextFormatter` and override the methods you want
# to enhance.
#
# @see RSpec::Core::Formatters::BaseTextFormatter
# @see RSpec::Core::Reporter
module RSpec::Core::Formatters
end
