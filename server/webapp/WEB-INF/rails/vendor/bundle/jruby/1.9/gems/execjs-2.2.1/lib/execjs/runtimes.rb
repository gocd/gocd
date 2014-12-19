require "execjs/module"
require "execjs/disabled_runtime"
require "execjs/external_runtime"
require "execjs/johnson_runtime"
require "execjs/mustang_runtime"
require "execjs/ruby_racer_runtime"
require "execjs/ruby_rhino_runtime"

module ExecJS
  module Runtimes
    Disabled = DisabledRuntime.new

    RubyRacer = RubyRacerRuntime.new

    RubyRhino = RubyRhinoRuntime.new

    Johnson = JohnsonRuntime.new

    Mustang = MustangRuntime.new

    Node = ExternalRuntime.new(
      name:        "Node.js (V8)",
      command:     ["nodejs", "node"],
      runner_path: ExecJS.root + "/support/node_runner.js",
      encoding:    'UTF-8'
    )

    JavaScriptCore = ExternalRuntime.new(
      name:        "JavaScriptCore",
      command:     "/System/Library/Frameworks/JavaScriptCore.framework/Versions/A/Resources/jsc",
      runner_path: ExecJS.root + "/support/jsc_runner.js"
    )

    SpiderMonkey = Spidermonkey = ExternalRuntime.new(
      name:        "SpiderMonkey",
      command:     "js",
      runner_path: ExecJS.root + "/support/spidermonkey_runner.js",
      deprecated:  true
    )

    JScript = ExternalRuntime.new(
      name:        "JScript",
      command:     "cscript //E:jscript //Nologo //U",
      runner_path: ExecJS.root + "/support/jscript_runner.js",
      encoding:    'UTF-16LE' # CScript with //U returns UTF-16LE
    )


    def self.autodetect
      from_environment || best_available ||
        raise(RuntimeUnavailable, "Could not find a JavaScript runtime. " +
          "See https://github.com/sstephenson/execjs for a list of available runtimes.")
    end

    def self.best_available
      runtimes.reject(&:deprecated?).find(&:available?)
    end

    def self.from_environment
      if name = ENV["EXECJS_RUNTIME"]
        if runtime = const_get(name)
          if runtime.available?
            runtime if runtime.available?
          else
            raise RuntimeUnavailable, "#{runtime.name} runtime is not available on this system"
          end
        elsif !name.empty?
          raise RuntimeUnavailable, "#{name} runtime is not defined"
        end
      end
    end

    def self.names
      @names ||= constants.inject({}) { |h, name| h.merge(const_get(name) => name) }.values
    end

    def self.runtimes
      @runtimes ||= [
        RubyRacer,
        RubyRhino,
        Johnson,
        Mustang,
        Node,
        JavaScriptCore,
        SpiderMonkey,
        JScript
      ]
    end
  end

  def self.runtimes
    Runtimes.runtimes
  end
end
