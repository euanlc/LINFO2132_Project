package compiler.Parser;
public class StringNode extends ASTNode {
   public String value;
    public StringNode(String value) { this.value = value; }
    @Override
    public void print(String prefix) {
        System.out.println(prefix + "String, " + value);
    }
}