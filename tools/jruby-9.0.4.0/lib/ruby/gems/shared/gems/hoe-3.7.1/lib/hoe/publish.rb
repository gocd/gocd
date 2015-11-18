require "hoe/rake"

##
# Publish plugin for hoe.
#
# === Tasks Provided:
#
# announce::           Create news email file and post to rubyforge.
# debug_email::        Generate email announcement file.
# post_blog::          Post announcement to blog.
# post_news::          Post announcement to rubyforge.
# publish_docs::       Publish RDoc to RubyForge.
# ridocs::             Generate ri locally for testing.
#
# === Extra Configuration Options:
#
# publish_on_announce:: Run +publish_docs+ when you run +release+.
# blogs::               An array of hashes of blog settings.
#
# The blogs entry can either look like:
#
#    - path: ~/Work/p4/zss/www/blog.zenspider.com/releases
#      type: zenweb
#      cmd: rake sync    (optional)
#
# or:
#
#    - url: http://example.com/cgi-bin/blog.cgi
#      blog_id: 1
#      user: username
#      password: passwd
#      extra_headers:
#        blah: whatever

module Hoe::Publish
  ##
  # Optional: An array of the project's blog categories. Defaults to project
  # name.

  attr_accessor :blog_categories

  ##
  # Optional: Name of destination directory for RDoc generated files.
  # [default: doc]

  attr_accessor :local_rdoc_dir

  ##
  # Optional: Should RDoc and ri generation tasks be defined? [default: true]
  #
  # Allows you to define custom RDoc tasks then use the publish_rdoc task to
  # upload them all.  See also local_rdoc_dir

  attr_accessor :need_rdoc

  ##
  # Optional: An array of remote (rsync) paths to copy rdoc to.
  #
  # eg:
  #
  #     rdoc_locations << "user@server:Sites/rdoc/#{remote_rdoc_dir}"

  attr_accessor :rdoc_locations

  ##
  # Optional: Name of RDoc destination directory on Rubyforge. [default: +name+]

  attr_accessor :remote_rdoc_dir

  ##
  # Optional: Flags for RDoc rsync. [default: "-av --delete"]

  attr_accessor :rsync_args

  Hoe::DEFAULT_CONFIG["publish_on_announce"] = true
  Hoe::DEFAULT_CONFIG["blogs"] = [
                                  {
                                    "user"     => "user",
                                    "password" => "password",
                                    "url"      => "url",
                                    "blog_id"  => "blog_id",
                                    "extra_headers" => {
                                      "mt_convert_breaks" => "markdown"
                                    },
                                  }
                                 ]

  ##
  # Initialize variables for plugin.

  def initialize_publish
    self.blog_categories ||= [self.name]
    self.local_rdoc_dir  ||= 'doc'
    self.need_rdoc       ||= true
    self.rdoc_locations  ||= []
    self.remote_rdoc_dir ||= self.name
    self.rsync_args      ||= '-av -O --delete'
  end

  ##
  # Declare a dependency on rdoc, IF NEEDED.

  def activate_publish_deps
    dependency "rdoc", "~> 4.0", :developer if need_rdoc
  end

  ##
  # Define tasks for plugin.

  def define_publish_tasks
    if need_rdoc then
      task :isolate # ensure it exists

      desc "Generate rdoc"
      task :docs => [:clobber_docs, :isolate] do
        sh(*make_rdoc_cmd)
      end

      desc "Generate rdoc coverage report"
      task :dcov => :isolate do
        sh(*make_rdoc_cmd('-C'))
      end

      desc "Remove RDoc files"
      task :clobber_docs do
        rm_rf local_rdoc_dir
      end

      task :clobber => :clobber_docs

      desc 'Generate ri locally for testing.'
      task :ridocs => [:clean, :isolate] do
        ruby(*make_rdoc_cmd('--ri', '-o', 'ri'))
      end
    end

    desc "Publish RDoc to wherever you want."
    task :publish_docs => [:clean, :docs] do
      publish_docs_task
    end

    # no doco for this one
    task :publish_on_announce do
      publish_on_announce_task
    end

    desc 'Generate email announcement file.'
    task :debug_email do
      puts generate_email
    end

    desc 'Post announcement to blog. Uses the "blogs" array in your hoerc.'
    task :post_blog do
      post_blog_task
    end

    desc 'Announce your release.'
    task :announce => [:post_blog, :publish_on_announce ]
  end

  def publish_docs_task # :nodoc:
    warn "no rdoc_location values" if rdoc_locations.empty?
    self.rdoc_locations.each do |dest|
      sh %{rsync #{rsync_args} #{local_rdoc_dir}/ #{dest}}
    end
  end

  def publish_on_announce_task # :nodoc:
    with_config do |config, _|
      Rake::Task['publish_docs'].invoke if config["publish_on_announce"]
    end
  end

  def post_blog_task # :nodoc:
    with_config do |config, path|
      break unless config['blogs']

      config['blogs'].each do |site|
        if site['path'] then
          msg = "post_blog_#{site['type']}"
          send msg, site
          system site["cmd"] if site["cmd"]
        else
          require 'xmlrpc/client'

          _, title, body, urls = announcement
          body += "\n\n#{urls}"

          server = XMLRPC::Client.new2(site['url'])
          content = site['extra_headers'].merge(:title => title,
                                                :description => body,
                                                :categories => blog_categories)

          server.call('metaWeblog.newPost',
                      site['blog_id'],
                      site['user'],
                      site['password'],
                      content,
                      true)
        end
      end
    end
  end

  def make_rdoc_cmd(*extra_args) # :nodoc:
    title = "#{name}-#{version} Documentation"
    title = "#{group_name}'s #{title}" if group_name != name
    rdoc  = Gem.bin_wrapper "rdoc"

    %W[#{rdoc}
       --title #{title}
       -o #{local_rdoc_dir}
      ] +
      spec.rdoc_options +
      extra_args +
      spec.require_paths +
      spec.extra_rdoc_files
  end

  def post_blog_zenweb site # :nodoc:
    dir = site["path"]

    _, title, body, urls = announcement
    body += "\n\n#{urls}"

    Dir.chdir File.expand_path dir do
      time = Time.at Time.now.to_i # nukes fractions
      path = [time.strftime("%Y-%m-%d-"),
              title.sub(/\W+$/, '').gsub(/\W+/, '-'),
              ".html.md"].join

      header = {
        "title"      => title,
        "categories" => blog_categories,
        "date"       => time,
      }

      File.open path, "w" do |f|
        f.puts header.to_yaml.gsub(/\s$/, '')
        f.puts "..."
        f.puts
        f.puts body
      end
    end
  end

  def generate_email full = nil # :nodoc:
    require 'time'

    abort "No email 'to' entry. Run `rake config_hoe` to fix." unless
      !full || email_to

    from_name, from_email      = author.first, email.first
    subject, title, body, urls = announcement

    [
     full && "From: #{from_name} <#{from_email}>",
     full && "To: #{email_to.join(", ")}",
     full && "Date: #{Time.now.rfc2822}",
     "Subject: [ANN] #{subject}",
     "", title,
     "", urls,
     "", body,
    ].compact.join("\n")
  end

  def announcement # :nodoc:
    changes = self.changes.rdoc_to_markdown
    subject = "#{name} #{version} Released"
    title   = "#{name} version #{version} has been released!"
    body    = "#{description}\n\nChanges:\n\n#{changes}".rdoc_to_markdown

    urls =
      case self.urls
      when Hash then
        self.urls.map { |k,v| "* #{k}: <#{v.strip.rdoc_to_markdown}>" }
      when Array then
        self.urls.map { |s| "* <#{s.strip.rdoc_to_markdown}>" }
      else
        raise "unknown urls format: #{urls.inspect}"
      end


    return subject, title, body, urls.join("\n")
  end
end

class ::Rake::SshDirPublisher # :nodoc:
  attr_reader :host, :remote_dir, :local_dir
end

class String
  ##
  # Very basic munge from rdoc to markdown format.

  def rdoc_to_markdown
    self.gsub(/^mailto:/, '').gsub(/^(=+)/) { "#" * $1.size }
  end
end
