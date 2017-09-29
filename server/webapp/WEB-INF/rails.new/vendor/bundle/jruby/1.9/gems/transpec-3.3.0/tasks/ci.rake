# coding: utf-8

tasks = %w(spec)

if RSpec::Core::Version::STRING.start_with?('2.14') && RUBY_ENGINE != 'jruby'
  tasks << 'style' if RUBY_VERSION >= '2.0.0'
  tasks.concat(%w(readme:check test:all))
end

task ci: tasks
