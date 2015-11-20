/**
 * (The MIT License)
 *
 * Copyright (c) 2008 - 2014:
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nokogiri.HtmlDocument;
import nokogiri.NokogiriService;
import nokogiri.XmlAttr;
import nokogiri.XmlCdata;
import nokogiri.XmlComment;
import nokogiri.XmlDocument;
import nokogiri.XmlDtd;
import nokogiri.XmlElement;
import nokogiri.XmlEntityReference;
import nokogiri.XmlNamespace;
import nokogiri.XmlNode;
import nokogiri.XmlProcessingInstruction;
import nokogiri.XmlText;
import nokogiri.XmlXpathContext;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyString;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

/**
 * A class for various utility methods.
 * 
 * @author serabe
 * @author Patrick Mahoney <pat@polycrystal.org>
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class NokogiriHelpers {
    public static final String CACHED_NODE = "NOKOGIRI_CACHED_NODE";
    public static final String VALID_ROOT_NODE = "NOKOGIRI_VALIDE_ROOT_NODE";
    public static final String ENCODED_STRING = "NOKOGIRI_ENCODED_STRING";

    public static XmlNode getCachedNode(Node node) {
        return (XmlNode) node.getUserData(CACHED_NODE);
    }

    public static void clearCachedNode(Node node) {
        node.setUserData(CACHED_NODE, null, null);
    }

    public static void clearXpathContext(Node node) {
        if (node == null) return;

        Node ownerDocument = node.getOwnerDocument();
        if (ownerDocument == null) {
            ownerDocument = node;
        }
        ownerDocument.setUserData(XmlXpathContext.XPATH_CONTEXT, null, null);
    }

    /**
     * Get the XmlNode associated with the underlying
     * <code>node</code>. Creates a new XmlNode (or appropriate subclass)
     * or XmlNamespace wrapping <code>node</code> if there is no cached
     * value.
     */
    public static IRubyObject getCachedNodeOrCreate(Ruby ruby, Node node) {
        if(node == null) return ruby.getNil();
        if (node.getNodeType() == Node.ATTRIBUTE_NODE && isNamespace(node.getNodeName())) {
            XmlDocument xmlDocument = (XmlDocument)node.getOwnerDocument().getUserData(CACHED_NODE);
            if (!(xmlDocument instanceof HtmlDocument)) {
            String prefix = getLocalNameForNamespace(((Attr)node).getName());
            prefix = prefix != null ? prefix : "";
            String href = ((Attr)node).getValue();
            XmlNamespace xmlNamespace = xmlDocument.getNamespaceCache().get(prefix, href);
            if (xmlNamespace != null) return xmlNamespace;
            else return XmlNamespace.createFromAttr(ruby, (Attr)node);
            }
        }
        XmlNode xmlNode = getCachedNode(node);
        if(xmlNode == null) {
            xmlNode = (XmlNode)constructNode(ruby, node);
            node.setUserData(CACHED_NODE, xmlNode, null);
        }
        return xmlNode;
    }
    
    /**
     * Construct a new XmlNode wrapping <code>node</code>.  The proper
     * subclass of XmlNode is chosen based on the type of
     * <code>node</code>.
     */
    public static IRubyObject constructNode(Ruby runtime, Node node) {
        if (node == null) return runtime.getNil();
        // this is slow; need a way to cache nokogiri classes/modules somewhere
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                XmlElement xmlElement = (XmlElement) NokogiriService.XML_ELEMENT_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::Element"));
                xmlElement.setNode(runtime.getCurrentContext(), node);
                return xmlElement;
            case Node.ATTRIBUTE_NODE:
                XmlAttr xmlAttr = (XmlAttr) NokogiriService.XML_ATTR_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::Attr"));
                xmlAttr.setNode(runtime.getCurrentContext(), node);
                return xmlAttr;
            case Node.TEXT_NODE:
                XmlText xmlText = (XmlText) NokogiriService.XML_TEXT_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::Text"));
                xmlText.setNode(runtime.getCurrentContext(), node);
                return xmlText;
            case Node.COMMENT_NODE:
                XmlComment xmlComment = (XmlComment) NokogiriService.XML_COMMENT_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::Comment"));
                xmlComment.setNode(runtime.getCurrentContext(), node);
                return xmlComment;
            case Node.ENTITY_NODE:
                return new XmlNode(runtime, getNokogiriClass(runtime, "Nokogiri::XML::EntityDecl"), node);
            case Node.ENTITY_REFERENCE_NODE:
                XmlEntityReference xmlEntityRef = (XmlEntityReference) NokogiriService.XML_ENTITY_REFERENCE_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::EntityReference"));
                xmlEntityRef.setNode(runtime.getCurrentContext(), node);
                return xmlEntityRef;
            case Node.PROCESSING_INSTRUCTION_NODE:
                XmlProcessingInstruction xmlProcessingInstruction = (XmlProcessingInstruction) NokogiriService.XML_PROCESSING_INSTRUCTION_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::ProcessingInstruction"));
                xmlProcessingInstruction.setNode(runtime.getCurrentContext(), node);
                return xmlProcessingInstruction;
            case Node.CDATA_SECTION_NODE:
                XmlCdata xmlCdata = (XmlCdata) NokogiriService.XML_CDATA_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::CDATA"));
                xmlCdata.setNode(runtime.getCurrentContext(), node);
                return xmlCdata;
            case Node.DOCUMENT_NODE:
                XmlDocument xmlDocument = (XmlDocument) NokogiriService.XML_DOCUMENT_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::Document"));
                xmlDocument.setDocumentNode(runtime.getCurrentContext(), node);
                return xmlDocument;
            case Node.DOCUMENT_TYPE_NODE:
                XmlDtd xmlDtd = (XmlDtd) NokogiriService.XML_DTD_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::DTD"));
                xmlDtd.setNode(runtime, node);
                return xmlDtd;
            default:
                XmlNode xmlNode = (XmlNode) NokogiriService.XML_NODE_ALLOCATOR.allocate(runtime, getNokogiriClass(runtime, "Nokogiri::XML::Node"));
                xmlNode.setNode(runtime.getCurrentContext(), node);
                return xmlNode;
        }
    }
    
    public static RubyClass getNokogiriClass(Ruby ruby, String name) {
        return NokogiriService.getNokogiriClassCache(ruby).get(name);
    }

    public static IRubyObject stringOrNil(Ruby runtime, String s) {
        if (s == null) return runtime.getNil();
        return convertJavaStringToRuby(runtime, s);
    }
    
    public static IRubyObject stringOrNil(Ruby runtime, byte[] bytes) {
        if (bytes == null) return runtime.getNil();
        return RubyString.newString(runtime, bytes);
    }
    
    public static IRubyObject stringOrBlank(Ruby runtime, String s) {
        if (s == null) return runtime.newString();
        return convertJavaStringToRuby(runtime, s);
    }
    
    private static IRubyObject convertJavaStringToRuby(Ruby runtime, String str) {
        if (runtime.is1_9()) {
            ByteList bytes = new ByteList(str.getBytes(RubyEncoding.UTF8), UTF8Encoding.INSTANCE);
            return RubyString.newString(runtime, bytes);
        } else {
            return RubyString.newString(runtime, str);
        }
    }

    /**
     * Convert <code>s</code> to a RubyString, or if s is null or
     * empty return RubyNil.
     */
    public static IRubyObject nonEmptyStringOrNil(Ruby runtime, String s) {
        if (s == null || s.length() == 0) return runtime.getNil();
        return RubyString.newString(runtime, s);
    }

    /**
     * Return the prefix of a qualified name like "prefix:local".
     * Returns null if there is no prefix.
     */
    public static String getPrefix(String qName) {
        if (qName == null) return null;

        int pos = qName.indexOf(':');
        if (pos > 0)
            return qName.substring(0, pos);
        else
            return null;
    }

    /**
     * Return the local part of a qualified name like "prefix:local".
     * Returns <code>qName</code> if there is no prefix.
     */
    public static String getLocalPart(String qName) {
        if (qName == null) return null;

        int pos = qName.indexOf(':');
        if (pos > 0)
            return qName.substring(pos + 1);
        else
            return qName;
    }

    public static String getLocalNameForNamespace(String name) {
        String localName = getLocalPart(name);
        return ("xmlns".equals(localName)) ? null : localName;
    }

    private static Charset utf8 = null;
    
    private static Charset getCharsetUTF8() {
        if (utf8 == null) utf8 = Charset.forName("UTF-8");
        return utf8;
    }

    /**
     * Converts a RubyString in to a Java String.  Assumes the
     * RubyString is encoded as UTF-8.  This is generally the case for
     * RubyStrings created with getRuntime().newString("java string").
     * It also seems to be the case for strings created within Ruby
     * where $KCODE has not been set.
     *
     * Note that RubyString#toString() decodes the string data as
     * ISO-8859-1 (See org.jruby.util.ByteList.java).  This is not
     * what you want if you have any multibyte characters in your
     * UTF-8 string.
     *
     * FIXME: This really needs to be more robust in terms of
     * detecting the encoding and properly converting to a Java
     * String.  It's unfortunate that RubyString#toString() doesn't do
     * this for us.
     */
    public static String rubyStringToString(IRubyObject str) {
        if (str.isNil()) return null;
        //return rubyStringToString(str.convertToString());
        return toJavaString(str.convertToString());
    }
    
    private static String toJavaString(RubyString str) {
        ByteList value = str.getByteList();
        try {
            if (str.getRuntime().is1_9()) {
                return new String(value.getUnsafeBytes(), value.begin(), value.length(), str.getEncoding().toString());
            }
            return RubyEncoding.decodeUTF8(value.getUnsafeBytes(), value.begin(), value.length());
        } catch (UnsupportedEncodingException uee) {
            return str.toString();
        }
    }

    public static String rubyStringToString(RubyString str) {
        ByteList byteList = str.getByteList();
        byte[] data = byteList.unsafeBytes();
        int offset = byteList.begin();
        int len = byteList.length();
        ByteBuffer buf = ByteBuffer.wrap(data, offset, len);
        return getCharsetUTF8().decode(buf).toString();
    }
    
    public static List<String> rubyStringArrayToJavaList(RubyArray ary) {
        List<String> list = new ArrayList<String>();
        for (int i=0; i < ary.getLength(); i++) {
            Object obj = ary.get(i);
            if (obj != null) list.add(obj.toString());
        }
        return list;
    }

    public static String getNodeCompletePath(Node node) {

        Node cur, tmp, next;

        // TODO: Rename buffer to path.
        String buffer = "";
        String sep;
        String name;

        int occur = 0;
        boolean generic;

        cur = node;

        do {
            name = "";
            sep = "?";
            occur = 0;
            generic = false;

            if(cur.getNodeType() == Node.DOCUMENT_NODE) {
                if(buffer.startsWith("/")) break;

                sep = "/";
                next = null;
            } else if(cur.getNodeType() == Node.ELEMENT_NODE) {
                generic = false;
                sep = "/";

                name = cur.getLocalName();
                if (name == null) name = cur.getNodeName();
                if(cur.getNamespaceURI() != null) {
                    if(cur.getPrefix() != null) {
                        name = cur.getPrefix() + ":" + name;
                    } else {
                        generic = true;
                        name = "*";
                    }
                }

                next = cur.getParentNode();

                /*
                 * Thumbler index computation
                 */

                tmp = cur.getPreviousSibling();

                while(tmp != null) {
                    if((tmp.getNodeType() == Node.ELEMENT_NODE) &&
                       (generic || fullNamesMatch(tmp, cur))) {
                        occur++;
                    }
                    tmp = tmp.getPreviousSibling();
                }

                if(occur == 0) {
                    tmp = cur.getNextSibling();

                    while(tmp != null && occur == 0) {
                        if((tmp.getNodeType() == Node.ELEMENT_NODE) &&
                            (generic || fullNamesMatch(tmp,cur))) {
                            occur++;
                        }
                        tmp = tmp.getNextSibling();
                    }

                    if(occur != 0) occur = 1;

                } else {
                    occur++;
                }
            } else if(cur.getNodeType() == Node.COMMENT_NODE) {
                sep = "/";
                name = "comment()";
                next = cur.getParentNode();

                /*
                 * Thumbler index computation.
                 */

                tmp = cur.getPreviousSibling();

                while(tmp != null) {
                    if(tmp.getNodeType() == Node.COMMENT_NODE) {
                        occur++;
                    }
                    tmp = tmp.getPreviousSibling();
                }

                if(occur == 0) {
                    tmp = cur.getNextSibling();
                    while(tmp != null && occur == 0) {
                        if(tmp.getNodeType() == Node.COMMENT_NODE) {
                            occur++;
                        }
                        tmp = tmp.getNextSibling();
                    }
                    if(occur != 0) occur = 1;
                } else {
                    occur = 1;
                }

            } else if(cur.getNodeType() == Node.TEXT_NODE ||
                cur.getNodeType() == Node.CDATA_SECTION_NODE) {
                    // I'm here. gist:129
                    // http://gist.github.com/144923

                sep = "/";
                name = "text()";
                next = cur.getParentNode();

                /*
                 * Thumbler index computation.
                 */

                tmp = cur.getPreviousSibling();
                while(tmp != null) {
                    if(tmp.getNodeType() == Node.TEXT_NODE ||
                            tmp.getNodeType() == Node.CDATA_SECTION_NODE) {
                        occur++;
                    }
                    tmp = tmp.getPreviousSibling();
                }

                if(occur == 0) {
                    tmp = cur.getNextSibling();

                    while(tmp != null && occur == 0) {
                        if(tmp.getNodeType() == Node.TEXT_NODE ||
                                tmp.getNodeType() == Node.CDATA_SECTION_NODE) {
                            occur++;
                        }
                        tmp = tmp.getNextSibling();
                    }
                } else {
                    occur++;
                }

            } else if(cur.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                sep = "/";
                name = "processing-instruction('"+cur.getLocalName()+"')";
                next = cur.getParentNode();

                /*
                 * Thumbler index computation.
                 */

                tmp = cur.getParentNode();

                while(tmp != null) {
                    if(tmp.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE &&
                            tmp.getLocalName().equals(cur.getLocalName())) {
                        occur++;
                    }
                    tmp = tmp.getPreviousSibling();
                }

                if(occur == 0) {
                    tmp = cur.getNextSibling();

                    while(tmp != null && occur == 0) {
                        if(tmp.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE &&
                                tmp.getLocalName().equals(cur.getLocalName())){
                            occur++;
                        }
                        tmp = tmp.getNextSibling();
                    }

                    if(occur != 0) {
                        occur = 1;
                    }

                } else {
                    occur++;
                }

            } else if(cur.getNodeType() == Node.ATTRIBUTE_NODE) {
                sep = "/@";
                name = cur.getLocalName();

                if(cur.getNamespaceURI() != null) {
                    if(cur.getPrefix() != null) {
                        name = cur.getPrefix() + ":" + name;
                    }
                }

                next = ((Attr) cur).getOwnerElement();

            } else {
                next = cur.getParentNode();
            }

            if(occur == 0){
                buffer = sep+name+buffer;
            } else {
                buffer = sep+name+"["+occur+"]"+buffer;
            }

            cur = next;

        } while(cur != null);

        return buffer;
    }

    protected static boolean compareTwoNodes(Node m, Node n) {
        return nodesAreEqual(m.getLocalName(), n.getLocalName()) &&
               nodesAreEqual(m.getPrefix(), n.getPrefix());
    }

    protected static boolean fullNamesMatch(Node a, Node b) {
        return a.getNodeName().equals(b.getNodeName());
    }

    protected static String getFullName(Node n) {
        String lname = n.getLocalName();
        String prefix = n.getPrefix();
        if (lname != null) {
            if (prefix != null)
                return prefix + ":" + lname;
            else
                return lname;
        } else {
            return n.getNodeName();
        }
    }

    private static boolean nodesAreEqual(Object a, Object b) {
      return (((a == null) && (a == null)) ||
                (a != null) && (b != null) &&
                (b.equals(a)));
    }

    private static Pattern encoded_pattern = Pattern.compile("&amp;|&gt;|&lt;|&#13;");
    private static Pattern decoded_pattern = Pattern.compile("&|>|<|\r");
    private static String[] encoded = {"&amp;", "&gt;", "&lt;", "&#13;"};
    private static String[] decoded = {"&", ">", "<", "\r"};

    private static String convert(Pattern ptn, String input, String[] oldChars, String[] newChars)  {
        Matcher matcher = ptn.matcher(input);
        boolean result = matcher.find();
        StringBuffer sb = new StringBuffer();
        while(result) {
            String matched = matcher.group();
            String replacement = "";
            for (int i=0; i<oldChars.length; i++) {
                if (matched.contains(oldChars[i])) {
                    replacement = matched.replace(oldChars[i], newChars[i]);
                    break;
                }
            }
            matcher.appendReplacement(sb, replacement);
            result = matcher.find();
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String encodeJavaString(String s) {
        return convert(decoded_pattern, s, decoded, encoded);
    }

    public static String decodeJavaString(String s) {
        return convert(encoded_pattern, s, encoded, decoded);
    }

    public static String getNodeName(Node node) {
        if(node == null) { System.out.println("node is null"); return ""; }
        String name = node.getNodeName();
        if(name == null) { System.out.println("name is null"); return ""; }
        if(name.equals("#document")) {
            return "document";
        } else if(name.equals("#text")) {
            return "text";
        } else {
            name = getLocalPart(name);
            return (name == null) ? "" : name;
        }
    }

    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    public static boolean isNamespace(Node node) {
        return (XMLNS_URI.equals(node.getNamespaceURI()) ||
                isNamespace(node.getNodeName()));
    }

    public static boolean isNamespace(String nodeName) {
        return (nodeName.startsWith("xmlns"));
    }

    public static boolean isNonDefaultNamespace(Node node) {
        return (isNamespace(node) && ! "xmlns".equals(node.getNodeName()));
    }

    public static boolean isXmlBase(String attrName) {
        return "xml:base".equals(attrName) || "xlink:href".equals(attrName);
    }

    public static boolean isWhitespaceText(ThreadContext context, IRubyObject obj) {
        if (obj == null || obj.isNil()) return false;

        XmlNode node = (XmlNode) obj;
        if (!(node instanceof XmlText))
            return false;

        String content = rubyStringToString(node.content(context));
        return content.trim().length() == 0;
    }

    public static boolean isWhitespaceText(String s) {
        return s.trim().length() == 0;
    }

    public static String canonicalizeWhitespce(String s) {
        StringBuilder sb = new StringBuilder();
        char[] chars = s.toCharArray();
        boolean newline_added = false;
        for (int i=0; i<chars.length; i++) {
            if (chars[i] == '\n') {
                if (!newline_added) {
                    sb.append(chars[i]);
                    newline_added = true;
                }
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    public static String newQName(String newPrefix, Node node) {
        String tagName = getLocalPart(node.getNodeName());
        if(newPrefix == null) {
            return tagName;
        } else {
            return newPrefix + ":" + tagName;
        }
    }

    public static RubyArray nodeListToRubyArray(Ruby ruby, NodeList nodes) {
        RubyArray array = RubyArray.newArray(ruby, nodes.getLength());
        return nodeListToRubyArray(ruby, nodes, array);
    }

    public static RubyArray nodeListToRubyArray(Ruby ruby, NodeList nodes, RubyArray array) {
        for(int i = 0; i < nodes.getLength(); i++) {
            array.append(NokogiriHelpers.getCachedNodeOrCreate(ruby, nodes.item(i)));
        }
        return array;
    }

    public static RubyArray nodeArrayToRubyArray(Ruby ruby, Node[] nodes) {
        RubyArray n = RubyArray.newArray(ruby, nodes.length);
        for(int i = 0; i < nodes.length; i++) {
            n.append(NokogiriHelpers.getCachedNodeOrCreate(ruby, nodes[i]));
        }
        return n;
    }

    public static RubyArray namedNodeMapToRubyArray(Ruby ruby, NamedNodeMap map) {
        RubyArray n = RubyArray.newArray(ruby, map.getLength());
        for(int i = 0; i < map.getLength(); i++) {
            n.append(NokogiriHelpers.getCachedNodeOrCreate(ruby, map.item(i)));
        }
        return n;
    }

    public static String getValidEncoding(Ruby runtime, IRubyObject encoding) {
        if (encoding.isNil()) {
            return guessEncoding();
        } else {
            return ignoreInvalidEncoding(runtime, encoding);
        }
    }

    private static String guessEncoding() {
        String name = null;
        if (name == null) name = System.getProperty("file.encoding");
        if (name == null) name = "UTF-8";
        return name;
    }

    private static Set<String> charsetNames = Charset.availableCharsets().keySet();

    private static String ignoreInvalidEncoding(Ruby runtime, IRubyObject encoding) {
        String givenEncoding = rubyStringToString(encoding);
        if (charsetNames.contains(givenEncoding)) return givenEncoding;
        else return guessEncoding();
    }

    public static String adjustSystemIdIfNecessary(String currentDir, String scriptFileName, String baseURI, String systemId) {
        if (systemId == null) return systemId;
        File file = new File(systemId);
        if (file.isAbsolute()) return systemId;
        String path = resolveSystemId(baseURI, systemId);
        if (path != null) return path;
        path = resolveSystemId(currentDir, systemId);
        if (path != null) return path;
        return resolveSystemId(scriptFileName, systemId);
    }

    private static String resolveSystemId(String baseName, String systemId) {
        if (baseName == null || baseName.length() < 1) return null;
        String parentName = null;
        baseName = baseName.replaceAll("%20", " ");
        File base = new File(baseName);
        if (base.isDirectory()) parentName = baseName;
        else parentName = base.getParent();
        if (parentName == null) return null;
        if (parentName.toLowerCase().startsWith("file:")) parentName = parentName.substring("file:".length());
        File dtdFile = new File(parentName + "/" + systemId);
        if (dtdFile.exists()) return dtdFile.getPath();
        return null;
    }

    public static boolean isUTF8(String encoding) {
        if (encoding == null) return true;   // no need to convert encoding
        int ret = Charset.forName(encoding).compareTo(Charset.forName("UTF-8"));
        return ret == 0;
    }

    public static byte[] convertEncoding(Charset output_charset, String input_string) throws CharacterCodingException {
        CharsetEncoder encoder = output_charset.newEncoder();
        CharBuffer charBuffer = CharBuffer.wrap(input_string);
        ByteBuffer byteBuffer = encoder.encode(charBuffer);
        byte[] buffer = new byte[byteBuffer.remaining()];
        byteBuffer.get(buffer);
        return buffer;
    }

    public static String convertEncodingByNKFIfNecessary(Ruby runtime, XmlDocument doc, String thing) {
        if (!(doc instanceof HtmlDocument)) return thing;
        String parsed_encoding = ((HtmlDocument)doc).getPraedEncoding();
        if (parsed_encoding == null) return thing;
        String ruby_encoding = rubyStringToString(doc.getEncoding());
        if (ruby_encoding == null) return thing;
        if (Charset.forName(parsed_encoding).compareTo(Charset.forName(ruby_encoding)) == 0) {
            return thing;
        } else {
            return NokogiriHelpers.nkf(runtime, ruby_encoding, thing);
        }
    }

    // This method is used from HTML documents. HTML meta tag with encoding specification
    // might appear after non-ascii characters are used. For example, a title tag before
    // a meta tag. In such a case, Xerces encodes characters in UTF-8 without seeing meta tag.
    // Nokogiri uses NKF library to convert characters correct encoding. This means the method
    // works only for JIS/Shift_JIS/EUC-JP.
    public static String nkf(Ruby runtime, String ruby_encoding, String thing) {
        StringBuffer sb = new StringBuffer("-");
        Charset that = Charset.forName(ruby_encoding);
        if (NokogiriHelpers.shift_jis.compareTo(that) == 0) {
            sb.append("S");
        } else if (NokogiriHelpers.jis.compareTo(that) == 0) {
            sb.append("J");
        } else if (NokogiriHelpers.euc_jp.compareTo(that) == 0) {
            sb.append("E");
        } else {
            // should not come here. should be treated before this method.
            sb.append("W");
        }
        sb.append("w");
        Class nkfClass = null;
        try {
            // JRuby 1.7 and later
            nkfClass = runtime.getClassLoader().loadClass("org.jruby.ext.nkf.RubyNKF");
        } catch (ClassNotFoundException e1) {
            try {
                // Before JRuby 1.7
                nkfClass = runtime.getClassLoader().loadClass("org.jruby.RubyNKF");
            } catch (ClassNotFoundException e2) {
                return thing;
            }
        }
        Method nkf_method;
        try {
            nkf_method = nkfClass.getMethod("nkf", ThreadContext.class, IRubyObject.class, IRubyObject.class, IRubyObject.class);
            RubyString r_str = 
                (RubyString)nkf_method.invoke(null, runtime.getCurrentContext(), null, runtime.newString(new String(sb)), runtime.newString(thing));
            return NokogiriHelpers.rubyStringToString(r_str);
        } catch (SecurityException e) {
            return thing;
        } catch (NoSuchMethodException e) {
            return thing;
        } catch (IllegalArgumentException e) {
            return thing;
        } catch (IllegalAccessException e) {
            return thing;
        } catch (InvocationTargetException e) {
            return thing;
        }
    }

    private static Charset shift_jis = Charset.forName("Shift_JIS");
    private static Charset jis = Charset.forName("ISO-2022-JP");
    private static Charset euc_jp = Charset.forName("EUC-JP");

    public static boolean shouldEncode(Node text) {
      return text.getUserData(NokogiriHelpers.ENCODED_STRING) == null ||
          !((Boolean)text.getUserData(NokogiriHelpers.ENCODED_STRING));
    }

    public static boolean shouldDecode(Node text) {
      return !shouldEncode(text);
    }

    public static NokogiriNamespaceCache getNamespaceCacheFormNode(Node n) {
        XmlDocument xmlDoc = (XmlDocument)getCachedNode(n.getOwnerDocument());
        return xmlDoc.getNamespaceCache();
    }

    public static Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
        Document doc = n.getOwnerDocument();
        NokogiriNamespaceCache nsCache = getNamespaceCacheFormNode(n);
        Node result = doc.renameNode(n, namespaceURI, qualifiedName);
        if (result != n) {
            nsCache.replaceNode(n, result);
        }
        return result;
    }
}
