/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2011:
 *
 * * {Aaron Patterson}[http://tenderlovemaking.com]
 * * {Mike Dalessio}[http://mike.daless.io]
 * * {Charles Nutter}[http://blog.headius.com]
 * * {Sergio Arbeo}[http://www.serabe.com]
 * * {Patrick Mahoney}[http://polycrystal.org]
 * * {Yoko Harada}[http://yokolet.blogspot.com]
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * 'Software'), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nokogiri.internals;

import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import nokogiri.XsltStylesheet;

/**
 * XSLT extension function caller. Currently, this class is not used because
 * parsing XSL file with extension function (written in Java) fails. The reason of 
 * the failure is a conflict of Java APIs. When xercesImpl.jar or jing.jar, or both
 * are on a classpath, parsing fails. Assuming parsing passes, this class will be
 * used as in below:
 * 
 * <xsl:stylesheet
 *      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 *      xmlns:f="xalan://nokogiri.internals.XsltExtensionFunction"
 *      extension-element-prefixes="f"
 *      version="1.0">
 *   <xsl:template match="text()">
 *     <xsl:copy-of select="f:call('capitalize', string(.))"/>
 *   </xsl:template>
 *   ...
 *  </xsl:stylesheet>
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class XsltExtensionFunction {
    public static Object call(String method, Object arg) {
        if (XsltStylesheet.getRegistry() == null) return null;
        ThreadContext context = (ThreadContext) XsltStylesheet.getRegistry().get("context");
        IRubyObject receiver = (IRubyObject)XsltStylesheet.getRegistry().get("receiver");
        if (context == null || receiver == null) return null;
        IRubyObject arg0 = JavaUtil.convertJavaToUsableRubyObject(context.getRuntime(), arg);
        IRubyObject result = RuntimeHelpers.invoke(context, receiver, method, arg0);
        return result.toJava(Object.class);
    }
}
