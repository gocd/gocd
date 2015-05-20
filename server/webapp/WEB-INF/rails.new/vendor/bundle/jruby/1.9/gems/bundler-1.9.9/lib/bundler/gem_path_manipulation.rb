module Bundler
  def self.preserve_gem_path
    original_gem_path = ENV["_ORIGINAL_GEM_PATH"]
    gem_path          = ENV["GEM_PATH"]
    ENV["_ORIGINAL_GEM_PATH"] = gem_path          if original_gem_path.nil? || original_gem_path == ""
    ENV["GEM_PATH"]           = original_gem_path if gem_path.nil? || gem_path == ""
  end
end
