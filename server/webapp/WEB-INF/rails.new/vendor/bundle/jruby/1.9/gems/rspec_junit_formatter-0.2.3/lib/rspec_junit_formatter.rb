require "time"
require "builder"

require "rspec/core"
require "rspec/core/formatters/base_formatter"

# Dumps rspec results as a JUnit XML file.
# Based on XML schema: http://windyroad.org/dl/Open%20Source/JUnit.xsd
class RSpecJUnitFormatter < RSpec::Core::Formatters::BaseFormatter
  # rspec 2 and 3 implements are in separate files.

private

  def xml
    @xml ||= Builder::XmlMarkup.new target: output, indent: 2
  end

  def xml_dump
    xml.instruct!
    xml.testsuite name: "rspec#{ENV['TEST_ENV_NUMBER']}", tests: example_count, failures: failure_count, errors: 0, time: "%.6f" % duration, timestamp: started.iso8601 do
      xml.comment! "Randomized with seed #{RSpec.configuration.seed}"
      xml.properties
      xml_dump_examples
    end
  end

  def xml_dump_examples
    examples.each do |example|
      send :"xml_dump_#{result_of(example)}", example
    end
  end

  def xml_dump_passed(example)
    xml_dump_example(example)
  end

  def xml_dump_pending(example)
    xml_dump_example(example) do
      xml.skipped
    end
  end

  def xml_dump_failed(example)
    exception = exception_for(example)
    backtrace = formatted_backtrace_for(example)

    xml_dump_example(example) do
      xml.failure message: exception.to_s, type: exception.class.name do
        xml.cdata! "#{exception.message}\n#{backtrace.join "\n"}"
      end
    end
  end

  def xml_dump_example(example, &block)
    xml.testcase classname: classname_for(example), name: description_for(example), file: example_group_file_path_for(example), time: "%.6f" % duration_for(example), &block
  end
end

RspecJunitFormatter = RSpecJUnitFormatter

if RSpec::Core::Version::STRING.start_with? "3."
  require "rspec_junit_formatter/rspec3"
else RSpec::Core::Version::STRING.start_with? "2."
  require "rspec_junit_formatter/rspec2"
end
