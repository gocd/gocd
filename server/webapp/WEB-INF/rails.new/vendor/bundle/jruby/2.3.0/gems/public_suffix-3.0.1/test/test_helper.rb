if ENV["COVERALL"]
  require "coveralls"
  Coveralls.wear!
end

require "minitest/autorun"
require "minitest/reporters"
require "mocha/setup"

Minitest::Reporters.use! Minitest::Reporters::DefaultReporter.new(color: true)

$LOAD_PATH.unshift File.expand_path("../../lib", __FILE__)
require "public_suffix"
