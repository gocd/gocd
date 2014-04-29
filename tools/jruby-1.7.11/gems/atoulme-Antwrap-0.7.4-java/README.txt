= Antwrap
    by Caleb Powell
    http://rubyforge.org/projects/antwrap/

== DESCRIPTION:

	A Ruby module that wraps the Apache Ant build tool. Antwrap can be used to invoke Ant Tasks from a Ruby or a JRuby script.

== FEATURES/PROBLEMS:

	Antwrap runs on the native Ruby interpreter via the RJB (Ruby Java Bridge gem) and on the JRuby interpreter. Antwrap is compatible with Ant versions 1.5.4, 
	1.6.5 and 1.7.0. For more information, 	see the Project Info (http://rubyforge.org/projects/antwrap/) page. 
	 
== SYNOPSIS:

	Antwrap is a Ruby library that can be used to invoke Ant tasks. It is being used in the Buildr (http://incubator.apache.org/buildr/) project to execute 
	Ant (http://ant.apache.org/) tasks in a Java project. If you are tired of fighting with Ant or Maven XML files in your Java project, take some time to 
	check out Buildr!

== USAGE:

	The Antwrap library is pretty simple to use, and should look very familiar to anyone who has written Ant tasks using XML.
	You begin by instantiating an AntProject;
	
	--------
	@ant = AntProject.new()
	--------
	
	You can pass in a Hash of project options like so;
	
	--------
	options = {:ant_home=>"/Users/fooman/tools/apache-ant-1.7.0", :name=>"FooProject", :basedir=> some_dir,
	:declarative=> true, :logger=> Logger.new(STDOUT), :loglevel=> Logger::DEBUG}
	
	@ant = AntProject.new(options)
	--------
	
	The default options for an AntProject are as follow
	
	   1. :name = The name of your AntProject. The default is ''.
	   2. :basedir = The location of your project base directory. The default is File.pwd.
	   3. :declarative = If true, the AntProject will execute the task when you invoke it. If false, it will return an instance of the task. Default value is true.
	   4. :logger = The Logger to use. The default id Logger.new(STDOUT)
	   5. :loglevel = The log level. Default is Logger::Error
	   6. :ant_home = The location of you Ant installation. If provided, Antwrap will locate and load the Ant Jar files into the CLASSPATH. It will only do this once per Ruby process, so even if you create multiple AntProject instances, it only loads the required files once. If :ant_home is not provided, it is assumed that you have added the Ant jar files to your CLASSPATH manually.
	
	Once you have an AntProject instance, you can begin invoking tasks. To do so, you simply invoke the desired task on the AntProject. You pass in 
	task attributes via a Hash, and you pass in child tasks inside a block. For example;
	
	--------
	@ant.path(:id => "other.class.path"){ |ant|                 
	    ant.pathelement(:location => "classes")                 
	    ant.pathelement(:location => "config")                  
	}                                                           
	                                                            
	@ant.path(:id => "common.class.path"){|ant|                                                                           
	    ant.fileset(:dir => "${common.dir}/lib"){               
	        ant.include(:name => "**/*.jar")                    
	    }                                                       
	    ant.pathelement(:location => "${common.classes}")       
	} 
	                                                                         
	@ant.javac(:srcdir => "test", :destdir => "classes"){|ant|  
	    ant.classpath(:refid => "common.class.path")            
	    ant.classpath(:refid => "foo.class.path")               
	}                                
	--------
	                           
	*Declarative Mode
	
	By default, Antwrap runs in declarative mode. This means that the AntProject will execute the tasks as you declare them. Alternatively, you can declare your
	Ant project to run in non-declarative mode, so that it only executes tasks upon the invocation of the execute() method (this is a more Object Oriented 
	approach, and may be useful in some circumstances). For example;
	
	--------
	@ant = AntProject.new({:name=>"FooProject", :declarative=> false})     
	                                                                       
	javac_task = @ant.javac(:srcdir => "test", :destdir => "classes"){|ant|                                                                        
	    ant.classpath(:refid => "common.class.path")                                                                                               
	    ant.classpath(:refid => "foo.class.path")                          
	}                                                                      
	                                                                       
	javac_task.execute                                                     
	--------
	
	*Reserved Words
	
	If your Ant task conflicts with a Ruby reserved word, you can prep-end an underscore. For example, Ant-Contrib 
	tasks such as 'if' and 'else' conflict with the Ruby reserved words;
	
	--------
	@ant._if(){|ant|                                     
	                                                     
	    ant._equals(:arg1 => "${bar}", :arg2 => "bar")   
	                                                     
	    ant._then(){                                     
	        ant.echo(:message => "if 1 is equal")        
	    }                                                                                          
	    ant._else(){                                     
	        ant.echo(:message => "if 1 is not equal")    
	    }                                                
	}                                                    
	--------
	
	Under most circumstances, you won't need to use these tasks because the Ruby language (OK... any general-purpose programming language) can handle 
	conditionals better than Ant. Indeed, the  awkwardness of conditional operations in Ant scripts is likely one of the 
	reasons why you want to move to a build system such as Buildr.
	
	*Content Data
	
	Content data is added via a 'pcdata' attribute:
	
	--------
	@ant.echo(:pcdata => "<foo&bar>")	
	--------
			
== REQUIREMENTS:

	rjb >= 1.0.3 (on MRI Ruby)
	hoe >= 1.3.0

== INSTALL:

	Installing Antwrap is done via the RubyGem gem command. In your OS shell type;

	$ gem install antwrap

	You will be prompted with the following options:

	$Select which gem to install for your platform (powerpc-darwin8.0)
	    1. Antwrap 0.7.0 (java)
	    2. Antwrap 0.7.0 (ruby)

	If you are using the native Ruby interpreter or running the Buildr project, then you want to select the ruby option (in this case, #2).
	If you are using Antwrap on the JRuby interpreter, select the java option. The native Ruby version of Antwrap depends on another
	gem called RJB (RubyJavaBridge) and you will be prompted to install this as part of the Antwrap installation. Do so. The RJB gem makes it possible
	for a Ruby script to instantiate Java classes via the Java Native Interface. If you chose the java gem, there are no further dependencies. Check the
	RJB site for how to get RJB running (usually, it's just a matter of setting the $JAVA_HOME and the $LD_LIBRARY_PATH environment variables).


== LICENSE:

		                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
		