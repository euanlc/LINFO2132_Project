package compiler.Parser;
public class StringNode extends ASTNode {
    String value;
    public StringNode(String value) { this.value = value; }
    @Override
    public void print(String prefix) {
        System.out.println(prefix + "String, " + value);
    }
}