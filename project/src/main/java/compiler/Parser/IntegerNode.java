package compiler.Parser;

public class IntegerNode extends ASTNode {
    int value;

    public IntegerNode(int value) {
        this.value = value;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Integer, " + value);
    }
}