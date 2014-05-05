module Bundler
  module CLI::Common
    def self.without_groups_message
      groups = Bundler.settings.without
      group_list = [groups[0...-1].join(", "), groups[-1..-1]].
        reject{|s| s.to_s.empty? }.join(" and ")
      group_str = (groups.size == 1) ? "group" : "groups"
      "Gems in the #{group_str} #{group_list} were not installed."
    end

    def self.select_spec(name, regex_match = nil)
      specs = []
      regexp = Regexp.new(name) if regex_match

      Bundler.definition.specs.each do |spec|
        return spec if spec.name == name
        specs << spec if regexp && spec.name =~ regexp
      end

      case specs.count
      when 0
        raise GemNotFound, gem_not_found_message(name, Bundler.definition.dependencies)
      when 1
        specs.first
      else
        ask_for_spec_from(specs)
      end
    end

    def self.ask_for_spec_from(specs)
      if !$stdout.tty? && ENV['BUNDLE_SPEC_RUN'].nil?
        raise GemNotFound, gem_not_found_message(name, Bundler.definition.dependencies)
      end

      specs.each_with_index do |spec, index|
        Bundler.ui.info "#{index.succ} : #{spec.name}", true
      end
      Bundler.ui.info '0 : - exit -', true

      num = Bundler.ui.ask('> ').to_i
      num > 0 ? specs[num - 1] : nil
    end

    def self.gem_not_found_message(missing_gem_name, alternatives)
      require 'bundler/similarity_detector'
      message = "Could not find gem '#{missing_gem_name}'."
      alternate_names = alternatives.map { |a| a.respond_to?(:name) ? a.name : a }
      suggestions = SimilarityDetector.new(alternate_names).similar_word_list(missing_gem_name)
      message += "\nDid you mean #{suggestions}?" if suggestions
      message
    end

  end
end
