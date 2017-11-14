require 'roar/version'
module Roar
  def self.root
    File.expand_path '../..', __FILE__
  end
end
