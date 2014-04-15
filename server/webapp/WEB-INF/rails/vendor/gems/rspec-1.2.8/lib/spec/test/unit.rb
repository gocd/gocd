require 'spec/interop/test'

# Hack to stop active_support/dependencies from complaining about
# 'spec/test/unit' not defining Spec::Test::Unit
module Spec
  module Test
    module Unit
    end
  end
end
