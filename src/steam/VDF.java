package steam;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class VDF {

  public static class VDFException extends Exception {
    public VDFException(String msg) {
      super(msg);
    }
  }
  
  public static class VDFNode implements Iterable<VDFNode> {
    private String key;
    private String value;
    private List<VDFNode> children = new ArrayList<>(1);
    
    public VDFNode() {
      
    }
    
    public VDFNode(String key, VDFNode ... children) {
      this.key = key;
      for (VDFNode n : children) {
        this.children.add(n);
      }
    }

    public VDFNode(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public String toString() {
      String s = key + ": ";
      if (value == null) {
        s += children.size() + " subnodes";
      } else {
        s += value;
      }
      return s;
    }

    public String toVDF() throws VDFException {
      return VDF.print(this);
    }

    public VDFNode getByPath(String path) {
      String[] keys = path.split("/");
      VDFNode current = this;
      for (String key : keys) {
        VDFNode n = current.get(key);
        if (n == null) {
          return null;
        }
        current = n;
      }
      return current;
    }

    public VDFNode get(String key) {
      for (VDFNode n : children) {
        if (key.equals(n.key)) {
          return n;
        }
      }
      return null;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public Iterator<VDFNode> iterator() {
      return children.iterator();
    }

    public void add(VDFNode node) {
      children.add(node);
    }

    public void setOrCreateValue(String path, String value) {
      String[] keys = path.split("/");
      VDFNode current = this;
      for (String key : keys) {
        VDFNode n = current.get(key);
        if (n == null) {
          n = new VDFNode();
          n.key = key;
          current.children.add(n);
        }
        current = n;
      }
      current.value = value;
    }
  }

  public static VDFNode readFile(String fileName) throws IOException, VDFException {

    FileReader fileReader = new FileReader(fileName);
    StreamTokenizer tokenizer = createTokenizer(fileReader);

    Deque<VDFNode> stack = new ArrayDeque<>();
    VDFNode root = new VDFNode();
    stack.push(root);
    while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
      int t = tokenizer.ttype;
      if (t == '}') {
        stack.pop();
      } else {
        if (!isStringOrQuotedString(t)) {
          int l = tokenizer.lineno();
          throw new VDFException("VDF parse error at line " + l
              + ": expected a key (string or quoted string)");
        }
        VDFNode node = new VDFNode();
        node.key = tokenizer.sval;
        stack.peek().children.add(node);

        t = tokenizer.nextToken();
        if (isStringOrQuotedString(t)) {
          node.value = tokenizer.sval;
        } else if (t == '{') {
          stack.push(node);
        } else {
          throw new VDFException("Parse error at line " + tokenizer.lineno() + ": "
              + (char) t);
        }
      }
    }

    if (stack.size() != 1) {
      throw new VDFException("Parse error, stack size not 0");
    }

    if (root.children.size() != 1) {
      throw new VDFException("Root node has too many children");
    }
    return root.children.get(0);
  }

  private static boolean isStringOrQuotedString(int t) throws VDFException {
    if (t == StreamTokenizer.TT_NUMBER) {
      throw new VDFException("This VDF parser doesn't support number types");
    }
    return t == StreamTokenizer.TT_WORD || t == '"';
  }

  private static StreamTokenizer createTokenizer(Reader reader) {
    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    tokenizer.resetSyntax();
    tokenizer.eolIsSignificant(false);
    tokenizer.lowerCaseMode(false);
    tokenizer.slashSlashComments(true);
    tokenizer.slashStarComments(false);
    tokenizer.commentChar('/');
    tokenizer.quoteChar('"');
    tokenizer.whitespaceChars('\u0000', '\u0020');
    tokenizer.wordChars('A', 'Z');
    tokenizer.wordChars('a', 'z');
    tokenizer.wordChars('\u00A0', '\u00FF');
    return tokenizer;
  }

  private static String print(VDFNode n) throws VDFException {
    StringBuffer b = new StringBuffer();
    print(b, n, "");
    return b.toString();
  }

  private static void print(StringBuffer b, VDFNode node, String prefix) throws VDFException {
    // print tabs
    b.append(prefix);

    // print key
    b.append('"').append(node.key).append('"');

    if (node.value != null && node.children.size() == 0) {
      // leaf node
      b.append("\t\t");
      b.append('"').append(node.value).append('"');
    } else if (node.value == null) {
      b.append('\n');
      b.append(prefix);
      b.append('{');
      b.append('\n');
      for (int i = 0; i < node.children.size(); i++) {
        VDFNode n = node.children.get(i);
        print(b, n, prefix + "\t");
        if (i != node.children.size() - 1) {
          b.append('\n');
        }
      }
      b.append('\n');
      b.append(prefix);
      b.append('}');
    } else {
      throw new VDFException(String.format(
          "Invalid node: key: %s, value: %s, num children: %s", node.key,
          node.value, node.children.size()));
    }
  }
}
