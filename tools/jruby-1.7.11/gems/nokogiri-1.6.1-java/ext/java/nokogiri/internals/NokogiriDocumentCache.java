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

import java.util.Hashtable;
import nokogiri.XmlDocument;
import org.w3c.dom.Document;

/**
 * Currently, this class is not used anywhere.
 * I'm not sure what for this class was written.(Yoko)
 * 
 * @author sergio
 */
public class NokogiriDocumentCache {

    private static NokogiriDocumentCache instance;
    protected Hashtable<Document, XmlDocument> cache;

    private NokogiriDocumentCache() {
        this.cache = new Hashtable<Document, XmlDocument>();
    }

    public static NokogiriDocumentCache getInstance() {
        if(instance == null) {
            instance = new NokogiriDocumentCache();
        }
        return instance;
    }

    public XmlDocument getXmlDocument(Document doc) {
        return this.cache.get(doc);
    }

    public void putDocument(Document doc, XmlDocument xmlDoc) {
        this.cache.put(doc, xmlDoc);
    }

    public XmlDocument removeDocument(Document doc) {
        return this.cache.remove(doc);
    }

}
