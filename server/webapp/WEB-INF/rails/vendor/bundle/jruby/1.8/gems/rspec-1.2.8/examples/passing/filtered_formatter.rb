$:.unshift File.join(File.dirname(__FILE__), "/../../lib")
require 'spec/runner/formatter/nested_text_formatter'

class FilteredFormatter < Spec::Runner::Formatter::NestedTextFormatter
  def add_example_group(example_group)
    if example_group.options[:show] == false
      @showing = false
    else
      @showing = true
      puts example_group.description 
    end
  end
  
  def example_passed(example)
    puts "  " << example.description if @showing unless example.options[:show] == false
  end
end

