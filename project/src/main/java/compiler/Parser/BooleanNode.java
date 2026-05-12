package compiler.Parser;
public class BooleanNode extends ASTNode {
    public boolean value;
    public BooleanNode(boolean value) { this.value = value; }
    @Override
    public void print(String prefix) {
        System.out.println(prefix + "Boolean, " + value);
    }
}