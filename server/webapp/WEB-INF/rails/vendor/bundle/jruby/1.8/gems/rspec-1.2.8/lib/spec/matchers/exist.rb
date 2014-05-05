module Spec
  module Matchers
    # :call-seq:
    #   should exist
    #   should_not exist
    #
    # Passes if actual.exist?
    def exist
      Matcher.new :exist do
        match do |actual|
          actual.exist?
        end
      end
    end
  end
end
