#
# Copyright Thoughtworks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Be sure to restart your server when you modify this file.

# You can add backtrace silencers for libraries that you're using but don't wish to see in your backtraces.
# Rails.backtrace_cleaner.add_silencer { |line| /my_noisy_library/.match?(line) }

# GoCD runs on JRuby, so uncaught exceptions raised in Java however Rails' BacktraceCleaner keeps only frames matching
# APP_DIRS_PATTERN and silences everything else, which is strictly following normal Rails layout in what it matches.
# This strips every Java frame and leaves an unhelpful Ruby-only backtrace that stops at the ERB template.
#
# Extend the original to keep all `.java` frames, the original Ruby app frames and rspec app frames (`spec/` rather
# than minitest's `test` convention).
if defined?(Rails::BacktraceCleaner) && Rails::BacktraceCleaner.const_defined?(:APP_DIRS_PATTERN, false)
  module Rails
    class BacktraceCleaner
      widened = Regexp.union(APP_DIRS_PATTERN, /\.java/, /\A(?:\.\/)?spec/)
      remove_const(:APP_DIRS_PATTERN)
      APP_DIRS_PATTERN = widened
    end
  end

  # Still silence stuff from the JRuby runtime and JDK internals/reflection code
  Rails.backtrace_cleaner.add_silencer { |line| line.match?(%r{org/jruby/|jdk/|java/lang/reflect}) }
end

# You can also remove all the silencers if you're trying to debug a problem that might stem from framework code
# by setting BACKTRACE=1 before calling your invocation, like "RAILS_BACKTRACE=1 ./bin/rails runner 'MyClass.perform'".
Rails.backtrace_cleaner.remove_silencers! if ENV["RAILS_BACKTRACE"]
