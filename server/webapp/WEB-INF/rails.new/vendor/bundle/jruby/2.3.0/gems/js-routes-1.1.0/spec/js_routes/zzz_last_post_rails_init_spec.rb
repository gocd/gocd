# we need to run post_rails_init_spec as the latest
# because it cause unrevertable changes to runtime
# what is why I added "zzz_last" in the beginning

require 'spec_helper'
require "fileutils"

describe "after Rails initialization" do
  NAME = Rails.root.join('app', 'assets', 'javascripts', 'routes.js').to_s

  def sprockets_v3?
    Sprockets::VERSION.to_i >= 3
  end

  def sprockets_context(environment, name, filename)
    if sprockets_v3?
      Sprockets::Context.new(environment: environment, name: name, filename: filename.to_s, metadata: {})
    else
      Sprockets::Context.new(environment, name, filename)
    end
  end

  def evaluate(ctx, file)
    if sprockets_v3?
      ctx.load(ctx.environment.find_asset(file, pipeline: :default).uri).to_s
    else
      ctx.evaluate(file)
    end
  end

  before(:each) do
    FileUtils.rm_rf Rails.root.join('tmp/cache')
    FileUtils.rm_f NAME
    JsRoutes.generate!(NAME)
  end

  before(:all) do
    Rails.configuration.eager_load = false
    Rails.application.initialize!
  end

  it "should generate routes file" do
    expect(File.exists?(NAME)).to be_truthy
  end

  context "JsRoutes::Engine" do
    TEST_ASSET_PATH = Rails.root.join('app','assets','javascripts','test.js')

    before(:all) do
      File.open(TEST_ASSET_PATH,'w') do |f|
        f.puts "function() {}"
      end
    end
    after(:all) do
      FileUtils.rm_f(TEST_ASSET_PATH)
    end

    it "should have registered a preprocessor" do
      pps = Rails.application.assets.preprocessors
      js_pps = pps['application/javascript']
      klass = sprockets_v3? ? 'LegacyProcProcessor' : 'Processor'
      expect(js_pps.map(&:to_s)).to include("Sprockets::#{klass} (js-routes_dependent_on_routes)")
    end

    context "the preprocessor" do
      before(:each) do
        if sprockets_v3?
          expect_any_instance_of(Sprockets::Context).to receive(:depend_on).with(Rails.root.join('config','routes.rb').to_s)
        else
          expect(ctx).to receive(:depend_on).with(Rails.root.join('config','routes.rb').to_s)
        end
      end
      let!(:ctx) do
        sprockets_context(Rails.application.assets,
                         'js-routes.js',
                         Pathname.new('js-routes.js'))
      end

      context "when dealing with js-routes.js" do

        context "with Rails" do
          context "and initialize on precompile" do
            before(:each) do
              Rails.application.config.assets.initialize_on_precompile = true
            end
            it "should render some javascript" do
              expect(evaluate(ctx, 'js-routes.js')).to match(/root\.Routes/)
            end
          end
          context "and not initialize on precompile" do
            before(:each) do
              Rails.application.config.assets.initialize_on_precompile = false
            end
            it "should raise an exception if 3 version" do
              if 3 == Rails::VERSION::MAJOR
                expect { evaluate(ctx, 'js-routes.js') }.to raise_error(/Cannot precompile/)
              else
                expect(evaluate(ctx, 'js-routes.js')).to match(/root\.Routes/)
              end
            end
          end

        end
      end


    end
    context "when not dealing with js-routes.js" do
      it "should not depend on routes.rb" do
        ctx = sprockets_context(Rails.application.assets,
                                'test.js',
                                TEST_ASSET_PATH)
        expect(ctx).not_to receive(:depend_on)
        evaluate(ctx, 'test.js')
      end
    end
  end
end

describe "JSRoutes thread safety" do
  before do
    begin
      Rails.application.initialize!
    rescue
    end
  end

  it "can produce the routes from multiple threads" do
    threads = 2.times.map do
      Thread.start do
        10.times {
          expect { JsRoutes.generate }.to_not raise_error
        }
      end
    end

    threads.each do |thread|
      thread.join
    end
  end
end
