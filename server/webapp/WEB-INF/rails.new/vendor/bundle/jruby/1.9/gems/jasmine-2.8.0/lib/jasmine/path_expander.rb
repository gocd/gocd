module Jasmine
  class PathExpander

    def self.expand(base_directory, patterns, globber = Dir.method(:glob))
      negative, positive = patterns.partition {|pattern| /^!/ =~ pattern}
      chosen, negated = [positive, negative].collect do |patterns|
        patterns.map do |path|
          files = globber.call(File.join(base_directory, path.gsub(/^!/, '')))
          if files.empty? && !(path =~ /\*|^\!/)
            if path[0..3] == 'http'
              files << path
            else
              files = [File.join(base_directory, path)]
            end
          end
          files.sort
        end.flatten.uniq
      end
      chosen - negated
    end
  end
end
