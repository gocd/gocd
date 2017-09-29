# coding: utf-8

module Astrolabe
  # @api private
  # http://semver.org/
  module Version
    MAJOR = 1
    MINOR = 3
    PATCH = 1

    def self.to_s
      [MAJOR, MINOR, PATCH].join('.')
    end
  end
end
