package com.thoughtworks.go.server;

/**
 * @understands how to run a local development mode webserver with new JRuby, so that we can develop live.
 * Set the following before running the main method:
 * Working directory: <project-path>/server
 * VM arguments: -Xms512m -Xmx1024m -XX:PermSize=400m -Djava.awt.headless=true
 * classpath: Use classpath of 'development-server-new-jruby'
 */
public class DevelopmentServerNewJRuby {
    public static void main(String[] args) throws Exception {
        DevelopmentServer.startWith(true, "./jruby_jars/jruby-1.7.11/jruby-complete-1.7.11.jar,./jruby_jars/jruby-1.7.11/jruby-rack-1.1.14.jar");
    }
}
