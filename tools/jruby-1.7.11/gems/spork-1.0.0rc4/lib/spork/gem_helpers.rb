module Spork::GemHelpers
  extend self

  def latest_specs
    Gem::Specification.inject({}) do |h, spec|
      h[spec.name] = spec if h[spec.name].nil? || (spec.version > h[spec.name].version)
      h
    end.values
  end

  def find_files_using_latest_spec(pattern)
    case
    when defined?(Bundler)
      Gem.find_files(pattern)
    when Gem.respond_to?(:find_files)
      latest_specs.map {  |spec| spec.matches_for_glob(pattern) }.flatten
    else
      STDERR.puts "No mechanism available to scan for other gems implementing Spork extensions."
      return []
    end
  end
end
