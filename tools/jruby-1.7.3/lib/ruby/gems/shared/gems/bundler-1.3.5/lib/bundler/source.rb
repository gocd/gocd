module Bundler
  module Source
    autoload :Rubygems, 'bundler/source/rubygems'
    autoload :Path, 'bundler/source/path'
    autoload :Git, 'bundler/source/git'
  end
end
