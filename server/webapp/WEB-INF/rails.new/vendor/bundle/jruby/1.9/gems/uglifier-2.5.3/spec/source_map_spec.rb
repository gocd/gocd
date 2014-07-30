# encoding: UTF-8
require 'stringio'
require File.expand_path(File.dirname(__FILE__) + '/spec_helper')

describe "Uglifier" do
  it "generates source maps" do
    source = File.open("lib/uglify.js", "r:UTF-8").read
    minified, map = Uglifier.new.compile_with_map(source)
    expect(minified.length).to be < source.length
    expect(map.length).to be > 0
    expect { JSON.parse(map) }.not_to raise_error
  end

  it "generates source maps with the correct meta-data" do
    source = <<-JS
      function hello () {
        function world () {
          return 2;
        };

        return world() + world();
      };
    JS

    _, map = Uglifier.compile_with_map(source,
                                       :source_filename => "ahoy.js",
                                       :output_filename => "ahoy.min.js",
                                       :source_root => "http://localhost/")

    map = SourceMap.from_s(map)
    expect(map.file).to eq("ahoy.min.js")
    expect(map.sources).to eq(["ahoy.js"])
    expect(map.names).to eq(%w(hello world))
    expect(map.source_root).to eq("http://localhost/")
    expect(map.mappings.first[:generated_line]).to eq(1)
  end

  it "should skip copyright lines in source maps" do
    source = <<-JS
      /* @copyright Conrad Irwin */
      function hello () {
        function world () {
          return 2;
        };

        return world() + world();
      };
    JS

    _, map = Uglifier.compile_with_map(source,
                                       :source_filename => "ahoy.js",
                                       :source_root => "http://localhost/")
    map = SourceMap.from_s(map)
    expect(map.mappings.first[:generated_line]).to eq(2)
  end

  it "should be able to handle an input source map" do
    source = <<-JS
      function hello () {
        function world () {
          return 2;
        };

        return world() + world();
      };
    JS

    minified1, map1 = Uglifier.compile_with_map(
      source,
      :source_filename => "ahoy.js",
      :source_root => "http://localhost/",
      :mangle => false
    )

    _, map2 = Uglifier.compile_with_map(source,
                                        :input_source_map => map1,
                                        :mangle => true)

    expect(minified1.lines.to_a.length).to eq(1)

    map = SourceMap.from_s(map2)
    expect(map.sources).to eq(["http://localhost/ahoy.js"])
    expect(map.mappings.first[:source_line]).to eq(1)
    expect(map.mappings.last[:source_line]).to eq(6)
  end
end
