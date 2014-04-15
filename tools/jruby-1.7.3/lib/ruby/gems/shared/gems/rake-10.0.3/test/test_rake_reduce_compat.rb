require File.expand_path('../helper', __FILE__)
require 'open3'

class TestRakeReduceCompat < Rake::TestCase
  # TODO: factor out similar code in test_rake_functional.rb
  def rake(*args)
    Open3.popen3(RUBY, "-I", @rake_lib, @rake_exec, *args) { |_, out, _, _|
      out.read
    }
  end

  def invoke_normal(task_name)
    rake task_name.to_s
  end

  def test_no_deprecated_dsl
    rakefile %q{
      task :check_task do
        Module.new { p defined?(task) }
      end

      task :check_file do
        Module.new { p defined?(file) }
      end
    }

    assert_equal "nil", invoke_normal(:check_task).chomp
    assert_equal "nil", invoke_normal(:check_file).chomp
  end
end
