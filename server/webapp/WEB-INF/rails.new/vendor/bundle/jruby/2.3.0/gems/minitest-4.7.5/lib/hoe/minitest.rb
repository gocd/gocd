# :stopdoc:

class Hoe
end

module Hoe::Minitest
  def initialize_minitest
    gem "minitest"
    require 'minitest/unit'
    version = MiniTest::Unit::VERSION.split(/\./).first(2).join(".")

    dependency 'minitest', "~> #{version}", :development unless
      self.name == "minitest"
  end

  def define_minitest_tasks
    self.testlib = :minitest

    # make sure we use the gemmed minitest on 1.9
    self.test_prelude = 'gem "minitest"'
  end
end
