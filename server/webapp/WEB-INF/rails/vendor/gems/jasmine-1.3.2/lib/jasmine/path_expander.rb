module Jasmine
  class PathExpander

    def self.expand(base_directory, patterns, globber = Dir.method(:glob))
      negative, positive = patterns.partition {|pattern| /^!/ =~ pattern}
      chosen, negated = [positive, negative].collect do |patterns|
        patterns.map do |path|
          files = globber.call(File.join(base_directory, path.gsub(/^!/, '')))
          if files.empty? && !(path =~ /\*|^\!/)
            files = [File.join(base_directory, path)]
          end
          files
        end.flatten.uniq
      end
      chosen - negated
    end
  end
end
