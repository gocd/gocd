module Bundler
  class CLI::Console
    attr_reader :options, :group, :consoles
    def initialize(options, group, consoles)
      @options = options
      @group = group
      @consoles = consoles
    end

    def run
      group ? Bundler.require(:default, *(group.split.map! {|g| g.to_sym })) : Bundler.require
      ARGV.clear

      preferred = Bundler.settings[:console] || 'irb'

      # See if console is available
      begin
        require preferred || true
      rescue LoadError
        # Is it in Gemfile?
        Bundler.ui.error "Could not load the #{preferred} console"
        Bundler.ui.info "Falling back on IRB..."

        require 'irb'
        preferred = 'irb'
      end

      constant = consoles[preferred]

      console = begin
                  Object.const_get(constant)
                rescue NameError => e
                  Bundler.ui.error e.inspect
                  Bundler.ui.error "Could not load the #{constant} console"
                  return
                end

      console.start
    end

  end
end
