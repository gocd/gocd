/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2012:
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

import java.util.HashMap;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

/**
 * XPath variable support
 * 
 * @author Ken Bloom <kbloom@gmail.com>
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class NokogiriXPathVariableResolver implements XPathVariableResolver {
    private static NokogiriXPathVariableResolver resolver;
    private HashMap<QName,String> variables = new HashMap<QName,String>();

    public static NokogiriXPathVariableResolver create() {
        if (resolver == null) resolver = new NokogiriXPathVariableResolver();
        try {
            NokogiriXPathVariableResolver clone = (NokogiriXPathVariableResolver) resolver.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            NokogiriXPathVariableResolver freshResolver = new NokogiriXPathVariableResolver();
            return freshResolver;
        }
    }
    
    private NokogiriXPathVariableResolver() {}
    
    public Object resolveVariable(QName variableName){
        return variables.get(variableName);
    }
    public void registerVariable(String name,String value){
        variables.put(new QName(name),value);
    }
}
