##
# Simple plugin to turn off ruby 1.9's gem_prelude in Hoe::RUBY_FLAGS

module Hoe::GemPreludeSucks

  Hoe::RUBY_FLAGS.insert 0, "--disable-gems -rubygems "

  def initialize_gem_prelude_sucks
    # do nothing
  end

  def define_gem_prelude_sucks_tasks
    # do nothing
  end
end
