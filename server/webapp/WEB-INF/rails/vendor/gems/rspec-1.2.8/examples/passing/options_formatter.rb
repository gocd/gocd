# This is an example of how you can use a custom formatter to do custom
# reporting. This formatter will only report example groups and examples that
# have :report => true (or anything truthy) in the declaration. See
# options_example.rb in this directory.  

require 'spec/runner/formatter/base_text_formatter'

class OptionsFormatter < Spec::Runner::Formatter::BaseTextFormatter
  def example_started(proxy)
    if proxy.options[:report]
      puts proxy.description
    end
  end

  def example_group_started(proxy)
    if proxy.options[:report]
      puts proxy.description
    end
  end
end
