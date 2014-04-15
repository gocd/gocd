#! /usr/bin/env ruby -w

require 'enumerator'
require 'fileutils'
require 'yaml'
require 'open-uri'
require 'rubyforge/client'

$TESTING = false unless defined? $TESTING

class RubyForge

  # :stopdoc:
  VERSION     = '1.0.3'
  HOME        = ENV["HOME"] || ENV["HOMEPATH"] || File::expand_path("~")
  RUBYFORGE_D = File::join HOME, ".rubyforge"
  CONFIG_F    = File::join RUBYFORGE_D, "user-config.yml"
  COOKIE_F    = File::join RUBYFORGE_D, "cookie.dat"

  # We must use __FILE__ instead of DATA because this is now a library
  # and DATA is relative to $0, not __FILE__.
  config = File.read(__FILE__).split(/__END__/).last.gsub(/#\{(.*)\}/) {eval $1}
  CONFIG = YAML.load(config)
  # :startdoc:

  # TODO: add an autoconfig method that is self-repairing, removing key checks 
  attr_reader :userconfig, :autoconfig

  def initialize(userconfig=nil, autoconfig=nil, opts=nil)
    # def initialize(userconfig=CONFIG_F, opts={})
    @userconfig, @autoconfig = userconfig, autoconfig

    @autoconfig ||= CONFIG["rubyforge"].dup
    @userconfig.merge! opts if opts

    @client = nil
    @uri = nil
  end

  def configure opts = {}
    user_path        = CONFIG_F
    dir, file        = File.split(user_path)

    @userconfig      = if test(?e, user_path) then
                         YAML.load_file(user_path)
                       else
                         CONFIG
                       end.merge(opts)
    @autoconfig_path = File.join(dir, file.sub(/^user/, 'auto'))
    @autoconfig      = if test(?e, @autoconfig_path) then
                         YAML.load_file(@autoconfig_path)
                       else
                         CONFIG["rubyforge"].dup
                       end
    @autoconfig["type_ids"] = CONFIG['rubyforge']['type_ids'].dup

    raise "no <username>"   unless @userconfig["username"]
    raise "no <password>"   unless @userconfig["password"]
    raise "no <cookie_jar>" unless @userconfig["cookie_jar"]

    self
  end

  def cookie_store
    client.cookie_store
  end

  def uri
    @uri ||= URI.parse @userconfig['uri']
  end

  def setup
    FileUtils::mkdir_p RUBYFORGE_D, :mode => 0700 unless test ?d, RUBYFORGE_D
    test ?e, CONFIG_F and FileUtils::mv CONFIG_F, "#{CONFIG_F}.bak"
    config = CONFIG.dup
    config.delete "rubyforge"

    open(CONFIG_F, "w") { |f|
      f.write YAML.dump(config)
    }
    FileUtils::touch COOKIE_F
    edit = (ENV["EDITOR"] || ENV["EDIT"] || "vi") + " '#{CONFIG_F}'"
    system edit or puts "edit '#{CONFIG_F}'"
  end

  def save_autoconfig
    File.open(@autoconfig_path, "w") do |file|
      YAML.dump @autoconfig, file
    end
  end

  def scrape_config
    username = @userconfig['username']

    %w(group package processor release).each do |type|
      @autoconfig["#{type}_ids"].clear if @autoconfig["#{type}_ids"]
    end

    puts "Getting #{username}"
    html = URI.parse("http://rubyforge.org/users/#{username}/index.html").read

    projects = html.scan(%r%/projects/([^/]+)/%).flatten

    puts "Fetching #{projects.size} projects"
    projects.each do |project|
      next if project == "support"
      scrape_project(project)
    end
  end

  def scrape_project(project)
    data = {
      "group_ids"     => {},
      "package_ids"   => {},
      "processor_ids" => Hash.new { |h,k| h[k] = {} },
      "release_ids"   => Hash.new { |h,k| h[k] = {} },
    }

    puts "Updating #{project}"

    unless data["group_ids"].has_key? project then
      html = URI.parse("http://rubyforge.org/projects/#{project}/index.html").read
      group_id = html[/(frs|tracker|mail)\/\?group_id=\d+/][/\d+/].to_i
      data["group_ids"][project] = group_id
    end

    group_id = data["group_ids"][project]

    html = URI.parse("http://rubyforge.org/frs/?group_id=#{group_id}").read

    package = nil
    html.scan(/<h3>[^<]+|release_id=\d+">[^>]+|filemodule_id=\d+/).each do |s|
      case s
      when /<h3>([^<]+)/ then
        package = $1.strip
      when /release_id=(\d+)">([^<]+)/ then
        data["release_ids"][package][$2] = $1.to_i
      when /filemodule_id=(\d+)/ then
        data["package_ids"][package] = $1.to_i
      end
    end

    if not data['release_ids'][package].empty? and
        (@autoconfig['processor_ids'].nil? or
         @autoconfig['processor_ids'].empty?) then
      puts "Fetching processor ids"

      login

      html = client.get_content "http://rubyforge.org/frs/admin/qrs.php?package=&group_id=#{group_id}"

      html =~ /<select name="processor_id">(.*?)<\/select>/m
      processors = $1
      processors.scan(/<option value="(\d{4})">([^<]+)/) do
        data["processor_ids"][$2] = $1.to_i
      end if processors
    end

    data.each do |key, val|
      @autoconfig[key] ||= {}
      @autoconfig[key].merge! val
    end

    save_autoconfig
  end

  def logout
    cookie_store.clear "rubyforge.org"
  end

  def login
    return if cookie_store['rubyforge.org']['session_ser'] rescue false

    page = self.uri + "/account/login.php"
    page.scheme = 'https'
    page = URI.parse page.to_s # set SSL port correctly

    username = @userconfig["username"]
    password = @userconfig["password"]

    form = {
      "return_to"      => "",
      "form_loginname" => username,
      "form_pw"        => password,
      "login"          => "Login"
    }

    response = run page, form

    re = %r/personal\s+page\s+for:\s+#{ Regexp.escape username }/iom
    unless response =~ re
      warn("%s:%d: warning: potentially failed login using %s" %
           [__FILE__, __LINE__, username]) unless $TESTING
    end

    response
  end

  def create_package(group_id, package_name)
    page = "/frs/admin/index.php"

    group_id = lookup "group", group_id
    is_private = @userconfig["is_private"]
    is_public = is_private ? 0 : 1

    form = {
      "func"         => "add_package",
      "group_id"     => group_id,
      "package_name" => package_name,
      "is_public"    => is_public,
      "submit"       => "Create This Package",
    }

    run page, form

    group_name = @autoconfig["group_ids"].invert[group_id]
    scrape_project(group_name)
  end

  ##
  # Posts news item to +group_id+ (can be name) with +subject+ and +body+

  def post_news(group_id, subject, body)
    page = "/news/submit.php"
    group_id = lookup "group", group_id

    form = {
      "group_id"     => group_id,
      "post_changes" => "y",
      "summary"      => subject,
      "details"      => body,
      "submit"       => "Submit",
    }

    run page, form
  end

  def delete_package(group_id, package_id)
    page = "/frs/admin/index.php"

    group_id = lookup "group", group_id
    package_id = lookup "package", package_id

    form = {
      "func"        => "delete_package",
      "group_id"    => group_id,
      "package_id"  => package_id,
      "sure"        => "1",
      "really_sure" => "1",
      "submit"      => "Delete",
    }

    package_name = @autoconfig["package_ids"].invert[package_id]
    @autoconfig["package_ids"].delete package_name
    @autoconfig["release_ids"].delete package_name
    save_autoconfig

    run page, form
  end

  def add_release(group_id, package_id, release_name, *files)
    userfile = files.shift
    page = "/frs/admin/qrs.php"

    group_id        = lookup "group", group_id
    package_id      = lookup "package", package_id
    userfile        = open userfile, 'rb'
    release_date    = @userconfig["release_date"]
    type_id         = @userconfig["type_id"]
    processor_id    = @userconfig["processor_id"]
    release_notes   = @userconfig["release_notes"]
    release_changes = @userconfig["release_changes"]
    preformatted    = @userconfig["preformatted"]

    release_date ||= Time.now.strftime("%Y-%m-%d %H:%M")

    type_id ||= userfile.path[%r|\.[^\./]+$|]
    type_id = (lookup "type", type_id rescue lookup "type", ".oth")

    processor_id ||= "Any"
    processor_id = lookup "processor", processor_id

    release_notes = IO::read(release_notes) if
      test(?e, release_notes) if release_notes

    release_changes = IO::read(release_changes) if
      test(?e, release_changes) if release_changes

    preformatted = preformatted ? 1 : 0

    form = {
      "group_id"        => group_id,
      "package_id"      => package_id,
      "release_name"    => release_name,
      "release_date"    => release_date,
      "type_id"         => type_id,
      "processor_id"    => processor_id,
      "release_notes"   => release_notes,
      "release_changes" => release_changes,
      "preformatted"    => preformatted,
      "userfile"        => userfile,
      "submit"          => "Release File"
    }

    boundary = Array::new(8){ "%2.2d" % rand(42) }.join('__')
    boundary = "multipart/form-data; boundary=___#{ boundary }___"

    html = run(page, form, 'content-type' => boundary)
    raise "Invalid package_id #{package_id}" if html[/Invalid package_id/]
    raise "You have already released this version." if html[/That filename already exists in this project/]

    release_id = html[/release_id=\d+/][/\d+/].to_i rescue nil

    unless release_id then
      puts html if $DEBUG
      raise "Couldn't get release_id, upload failed\?"
    end

    puts "RELEASE ID = #{release_id}" if $DEBUG

    files.each do |file|
      add_file(group_id, package_id, release_id, file)
    end

    package_name = @autoconfig["package_ids"].invert[package_id]
    raise "unknown package name for #{package_id}" if package_name.nil?
    @autoconfig["release_ids"][package_name] ||= {}
    @autoconfig["release_ids"][package_name][release_name] = release_id
    save_autoconfig

    release_id
  end

  ##
  # add a file to an existing release under the specified group_id,
  # package_id, and release_id
  #
  # example :
  #   add_file("codeforpeople.com", "traits", "0.8.0", "traits-0.8.0.gem")
  #   add_file("codeforpeople.com", "traits", "0.8.0", "traits-0.8.0.tgz")
  #   add_file(1024, 1242, "0.8.0", "traits-0.8.0.gem")

  def add_file(group_name, package_name, release_name, userfile)
    page         = '/frs/admin/editrelease.php'
    type_id      = @userconfig["type_id"]
    group_id     = lookup "group", group_name
    package_id   = lookup "package", package_name
    release_id   = (Integer === release_name) ? release_name : lookup("release", package_name)[release_name]
    processor_id = @userconfig["processor_id"]

    page = "/frs/admin/editrelease.php?group_id=#{group_id}&release_id=#{release_id}&package_id=#{package_id}"

    userfile = open userfile, 'rb'

    type_id ||= userfile.path[%r|\.[^\./]+$|]
    type_id = (lookup "type", type_id rescue lookup "type", ".oth")

    processor_id ||= "Any"
    processor_id = lookup "processor", processor_id

    form = {
      "step2"        => 1,
      "type_id"      => type_id,
      "processor_id" => processor_id,
      "userfile"     => userfile,
      "submit"       => "Add This File"
      }

    boundary = Array::new(8){ "%2.2d" % rand(42) }.join('__')
    boundary = "multipart/form-data; boundary=___#{ boundary }___"

    run page, form, 'content-type' => boundary
  end

  def client
    return @client if @client

    @client = RubyForge::Client::new ENV["HTTP_PROXY"]
    @client.debug_dev = STDERR if ENV["RUBYFORGE_DEBUG"] || ENV["DEBUG"] || $DEBUG
    @client.cookie_store = @userconfig["cookie_jar"]

    @client
  end

  def run(page, form, extheader={}) # :nodoc:
    uri = self.uri + page
    if $DEBUG then
      puts "client.post_content #{uri.inspect}, #{form.inspect}, #{extheader.inspect}"
    end

    response = client.post_content uri, form, extheader

    client.save_cookie_store

    if $DEBUG then
      response.sub!(/\A.*end tabGenerator -->/m, '')
      response.gsub!(/\t/, '  ')
      response.gsub!(/\n{3,}/, "\n\n")
      puts response
    end

    return response
  end

  def lookup(type, val) # :nodoc:
    unless Fixnum === val then
      key = val.to_s
      val = @autoconfig["#{type}_ids"][key]
      raise "no <#{type}_id> configured for <#{ key }>" unless val
    end
    val
  end
end

__END__
#
# base rubyforge uri - store in #{ CONFIG_F }
#
  uri        : http://rubyforge.org
#
# this must be your username
#
  username   : username
#
# this must be your password
#
  password   : password
#
# defaults for some values
#
  cookie_jar : #{ COOKIE_F }
  is_private : false
# AUTOCONFIG:
  rubyforge :
  #
  # map your group names to their rubyforge ids
  #
    group_ids :
      codeforpeople.com : 1024
  #
  # map your package names to their rubyforge ids
  #
    package_ids :
      traits : 1241
  #
  # map your package names to their rubyforge ids
  #
    release_ids :
      traits :
        1.2.3 : 666
  #
  # mapping file exts to rubyforge ids
  #
    type_ids :
      .deb         : 1000
      .rpm         : 2000
      .zip         : 3000
      .bz2         : 3100
      .gz          : 3110
      .src.zip     : 5000
      .src.bz2     : 5010
      .src.tar.bz2 : 5010
      .src.gz      : 5020
      .src.tar.gz  : 5020
      .src.rpm     : 5100
      .src         : 5900
      .jpg         : 8000
      .txt         : 8100
      .text        : 8100
      .htm         : 8200
      .html        : 8200
      .pdf         : 8300
      .oth         : 9999
      .ebuild      : 1300
      .exe         : 1100
      .dmg         : 1200
      .tar.gz      : 5000
      .tgz         : 5000
      .gem         : 1400
      .pgp         : 8150
      .sig         : 8150
      .pem         : 1500

  #
  # map processor names to rubyforge ids
  #
    processor_ids :
      Other      : 9999
