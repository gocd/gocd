##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

class TopicMaker

	attr_accessor :css_styles, :page_header, :page_footer

	def initialize(source_file, toc_source_file, output_directory)
		output_file = File.new(File.join(output_directory, (File.basename(source_file, '.xml') + '.html')), 'w')
		@html = HtmlRenderer.new(output_file)
		@root = REXML::Document.new(File.new(source_file)).root
		@body_handler = TopicHandler.new(@html, @root)
		@toc_root = REXML::Document.new(File.new(toc_source_file)).root
		@toc_handler = TocHandler.new(@html, @toc_root)
	end
		
	def title_text
		(@root.elements['//chapter'] || @root.elements['//topic'] || @root.elements['//section'] || @root.elements['//subsection']).attributes['title']
	end	
	
	def run

		render_page
		@html.close
	end

	def render_doctype
	    @html.non_close_element('!doctype html') do
	    end
	end
	def render_page
	    render_doctype
		@html.element('html', html_attrs) do
		  render_head
			@html.element('body') do
        # @html.element('div', {'id' => 'doc3', 'class' => 'yui-t3'}) do

			    @html.element('div', {'id' => 'hd'}) do
			      @html.element('h1') do
			        @html.text('Help documentation')
                    @html.element('div', {'id' => 'go_help_search'}){}
			      end
                end

          # @html.element('div', {'id' => 'bd'}) do
                @html.element('div', {'id' => 'main-container'}) do
                  @html.element('div', {'id' => 'search_results_container'}) do
                    @html.element('div', {'class' => 'action-bar', 'inner_wrapper_class' => 'action-bar-inner-wrapper'}) do
                      @html.element('div', {'class' => 'action-bar-inner-wrapper'}) do
                        @html.element('a', {'id' => 'hide_search_results'}) do
                          @html.text('&nbsp;')
                        end
                      end
                    end
                    @html.element('div', {'id' => 'branding'}) {}
                    @html.element('div', {'id' => 'search_results'}) {}
                  end
                  @html.element('div', {'id' => 'main'}) do
                    # render_under_construction
                    @body_handler.render
                  end
                end

                  @html.element('div', {'id' => "no_result_message", 'class' => "error-box", 'style' => 'display:none;'}) do
                    @html.element('div', {'class' => "box-content"}) do
                      @html.element('div', {'class' => "box"}) do
                        @html.text("Your search ")
                        @html.element('span', {'class' => "search_term"}){}
                        @html.text("did not match any help pages.")
                      end
                    end
                  end
            # @html.element('div', {'id' => 'nav', 'class' => 'yui-b'}) do
				    @html.element('div', {'id' => 'nav'}) do
				      @toc_handler.render
				    end
          # end
				  
				  @html.element('div', {'id' => 'ft', 'class' => 'footer'}) do
			      @html.text('&copy; ThoughtWorks Studios, 2010')
				  end
			  
        # end
			end
    end
	end
	
	def render_toc
	  @html.element('div', {'class' => 'toc'}) do
  		@toc_root = REXML::Document.new(File.new(source_file)).root
	  end
	end
	
	def html_attrs
			return { 'xmlns' => 'http://www.w3.org/1999/xhtml',
					'xml:lang' => "en", 
					'lang' => "en"}
    end

	def render_head
		html_head_title title_text
	end

    def html_head_title title
      id = ENV["GOOGLE_SITE_VERIFICATION_ID"] || "ALfMTxdH04YK-7c9mgrjLTkrOP55XqZtXqFk1dqCB4M"
      @html.element 'head' do
        @html.element('meta', 'http-equiv' => 'Content-Type', 'content' => 'text/html; charset=UTF-8') {}
        @html.element('meta', 'name' => 'google-site-verification', 'content' => "#{id}") {}
        @html.element('title') { @html.text title }
        @html.element('link', 'href' => 'resources/stylesheets/help.css', 'media' => 'screen', 'rel' => 'Stylesheet', 'type' => 'text/css') {}
        @html.element('link', 'href' => 'resources/stylesheets/help_search.css', 'media' => 'screen', 'rel' => 'Stylesheet', 'type' => 'text/css') {}
        @html.element('script', 'src' => '//www.google.com/jsapi', 'type' => 'text/javascript') {}
            @html.element('link', 'href' => '/go/images/cruise.ico', 'rel' => 'shortcut icon') {}
        @html.element('script', 'src' => 'resources/javascript/prototype.js', 'type' => 'text/javascript') {}
        @html.element('script', 'src' => 'resources/javascript/help.js', 'type' => 'text/javascript') {}
        @html.element('script', 'src' => 'resources/javascript/help_search.js', 'type' => 'text/javascript') {}
      end
    end

    def up_to_date input, output
		return false if not File.exists? output
		return File.stat(output).mtime > File.stat(input).mtime
	end


end
