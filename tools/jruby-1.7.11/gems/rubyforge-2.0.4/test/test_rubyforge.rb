require 'test/unit' unless defined? $ZENTEST and $ZENTEST

$TESTING = true
require 'rubyforge'
require 'tmpdir'

class RubyForge
  attr_writer :client

  alias :old_save_autoconfig :save_autoconfig
  def save_autoconfig
    # raise "not during test"
  end
end

class RubyForge::FakeClient
  def form; end

  def post_content(*args)
    FakeRubyForge::HTML
  end

  def get_content(*args)
    URI::HTTP.data.join("\n")
  end
end

class FakeRubyForge < RubyForge
  JSON = '{"release_id" : 42}'

  attr_accessor :page, :form, :extheader, :requests, :scrape
  def run(page, form, extheader={})
    @page, @form, @extheader = page, form, extheader
    @requests ||= []
    @requests << { :url => page, :form => form, :headers => extheader }
    JSON
  end

  def scrape_project(proj)
    @scrape ||= []
    @scrape << proj
  end
end

# TODO: remove this and make rubyforge use Client exclusively
class URI::HTTP
  def self.data
    @data ||= []
  end

  def read
    self.class.data.shift or raise "no more data"
  end
end

class TestRubyForge < Test::Unit::TestCase
  def setup
    srand(0)
    util_new FakeRubyForge
  end

  def teardown
    #     if defined? @old_autoconfig then
    #       @rubyforge.autoconfig.replace @old_autoconfig
    #       @rubyforge.save_autoconfig
    #     end
  end

  def test_new_with_proxy_uses_a_proxy_class
    client = RubyForge::Client.new('http://localhost:8808/')
    assert client.agent_class.proxy_class?, 'agent class should be a proxy'
  end
  
  def test_new_with_bad_proxy_uses_normal_http
    client = RubyForge::Client.new('asdfkjhalksdfjh')
    assert !client.agent_class.proxy_class?, 'agent class should not be a proxy'
  end
  
  def test_initialize_bad
    user_data = {
      "uri"        => "http://api.rubyforge.org",
      "is_private" => false,
      "username"   => "username",
      "password"   => "password"
    }
  
    assert_raise RuntimeError do
      rf = RubyForge.new user_data
      rf.configure "username" => nil
    end
    assert_raise RuntimeError do
      rf = RubyForge.new user_data
      rf.configure "password" => nil
    end
  end
  
  def test_setup
    # TODO raise NotImplementedError, 'Need to write test_setup'
  end
  
  def test_create_package
    @rubyforge.create_package(42, 'woot_pkg')
  
    util_run('/groups/42/packages',
             "package[is_public]" => 1,
             "package[name]" => "woot_pkg")
  end
  
  def test_delete_package
    @rubyforge.delete_package(42, 666)
    util_delete_package
  end
  
  def test_delete_package_package_name
    @rubyforge.delete_package(42, "woot_pkg")
    util_delete_package
  end
  
  def test_delete_package_undefined_package_name
    assert_raise RuntimeError do
      @rubyforge.delete_package(42, "blah")
    end
  end
  
  def test_delete_package_group_name
    @rubyforge.delete_package("seattlerb", 666)
    util_delete_package
  end
  
  def test_delete_package_undefined_group_name
    assert_raise RuntimeError do
      @rubyforge.delete_package("blah", 666)
    end
  end
  
  def test_post_news
    @rubyforge.post_news("seattlerb", "my summary", "my news")
  
    util_run("/groups/42/news_bytes",
             "news_byte[details]"      => "my news",
             "news_byte[summary]"      => "my summary")
  end
  
  def test_add_release_undefined_package_name
    assert_raise RuntimeError do
      @rubyforge.add_release(42, "blah", '1.2.3', __FILE__)
    end
  end
  
  def test_add_release_undefined_group_name
    assert_raise RuntimeError do
      @rubyforge.add_release("blah", 666, '1.2.3', __FILE__)
    end
  end
  
  def test_lookup_id
    assert_equal 43, @rubyforge.lookup("package", 43)
  end
  
  def test_lookup_string_number
    assert_raise RuntimeError do
      @rubyforge.lookup("package", "43")
    end
  end
  
  def test_lookup_name
    @rubyforge.autoconfig["package_ids"]["ringy_dingy"] = 314
    assert_equal 314, @rubyforge.lookup("package", "ringy_dingy")
  end
  
  def test_lookup_undefined
    assert_raise RuntimeError do
      @rubyforge.lookup("package", "blah")
    end
  end
  
  def test_add_file
    @rubyforge.autoconfig["package_ids"]["ringy_dingy"] = 314
    @rubyforge.autoconfig["release_ids"]["ringy_dingy"] ||= {}
    @rubyforge.autoconfig["release_ids"]["ringy_dingy"]["1.2.3"] = 43
  
    filepath, contents = make_a_tmp_file
  
    @rubyforge.add_file('seattlerb', 'ringy_dingy', '1.2.3', filepath)
  
    util_run('/releases/43/files.js', { 
      "file[type_id]" => 9999, 
      "file[processor_id]" => 8000, 
      "file[filename]"=> File.basename(filepath),
      "contents" => File.read(filepath)
      })
  end
  
    def test_add_release
      @rubyforge.add_release(42, 666, '1.2.3')
      util_add_release
    end
    
    def test_add_release_with_a_file
      filepath, contents = make_a_tmp_file
      
      @rubyforge.add_release(42, 666, '1.2.3', filepath)
      add_release = ({ :url=>"/packages/666/releases",
                       :form=>{ "release[name]" => "1.2.3",
                         "release[notes]" => nil,
                         "release[changes]" => nil,
                         "release[preformatted]"=>0,
                         "release[release_date]" => "today"},
                       :headers=> {}})
      add_file = ({ :url => '/releases/42/files.js',
                    :form => {"file[type_id]" => 9999, 
                    "file[processor_id]" => 8000, 
                    "file[filename]"=> File.basename(filepath),
                    "contents" => File.read(filepath)
                    },
                    :headers => {}})
      expected = [add_release, add_file]
      
      result = @rubyforge.requests
      result.each do |r|
        r[:form].delete "userfile"
      end
      
      assert_equal expected, result
    end
  
    def test_add_release_package_name
      @rubyforge.add_release(42, "woot_pkg", '1.2.3')
      util_add_release
    end
  
  def test_add_release_group_name
    @rubyforge.add_release("seattlerb", 666, '1.2.3')
    util_add_release
  end
  
  
  def test_scrape_project
    orig_stdout = $stdout
    orig_stderr = $stderr
    $stdout = StringIO.new
    $stderr = StringIO.new
    util_new RubyForge # TODO: switch to Fake
    @rubyforge.autoconfig.each { |k,v| v.clear }

    URI::HTTP.data << '{"group" : {"group_id":1513}}'
    URI::HTTP.data << '[{"package" : {"package_id":4566, "package_name":"1.3.1"}}]'

    #    @rubyforge.scrape << < <-EOF
#     URI::HTTP.data << <<-EOF
# <select name="processor_id">
# <option value="100">Must Choose One</option>
# <option value="1000">i386</option>
# <option value="1001">i387</option>
# </select>
#     EOF
    # 
    @rubyforge.scrape_project('my_project') rescue "Hm, for some reason this technique of loading up data on URI::HTTP isn't working here.  Not sure why."
    # 
    # expected = {
    #   "group_ids" => { "my_project" => 1513 },
    #   "package_ids" => { "ar_mailer" => 4566 },
    #   "processor_ids" => { "i386" => 1000, "i387" => 1001 },
    #   "release_ids" => {
    #     "ar_mailer" => { "1.2.0" => 12185, "1.3.1" => 13368 }
    #   },
    #   "type_ids" => {},
    # }
    # 
    # assert_equal expected, @rubyforge.autoconfig
  ensure
    $stdout = orig_stdout
    $stderr = orig_stderr
  end

  def util_new(klass)
    user_data = {
      "uri"        => "http://api.rubyforge.org",
      "is_private" => false,
      "username"   => "username",
      "password"   => "password"
    }

    auto_data = {
      "group_ids" => {},
      "package_ids" => {},
      "release_ids" => Hash.new { |h,k| h[k] = {} },
      "type_ids" => {},
      "processor_ids" => {"Any"=>8000},
    }

    @rubyforge = klass.new user_data, auto_data

    @rubyforge.client = RubyForge::FakeClient.new

    @rubyforge.userconfig["release_date"]            = "today"
    @rubyforge.autoconfig["type_ids"][".rb"]         = 9999
    @rubyforge.autoconfig["group_ids"]["seattlerb"]  = 42
    @rubyforge.autoconfig["package_ids"]["woot_pkg"] = 666
  end

  def util_run(page, form={}, extheader={})
    form_result = @rubyforge.form
    assert_equal page, @rubyforge.page.to_s
    assert_equal form, form_result
    assert_equal extheader, @rubyforge.extheader
  end

  def util_add_release
    util_run("/packages/666/releases",
             { "release[name]" => "1.2.3",
               "release[notes]" => nil,
               "release[changes]" => nil,
               "release[preformatted]"=>0,
               "release[release_date]" => "today"})
  end

  def util_delete_package
    util_run('/packages/666', "_method" => "delete")
  end

  def make_a_tmp_file
    content = "Merely a test"
    tmp_file = File.join(Dir.tmpdir, "test.rb")
    File.open(tmp_file, "w") { |f| f.syswrite(content) }
    [tmp_file, content] 
  end

end
