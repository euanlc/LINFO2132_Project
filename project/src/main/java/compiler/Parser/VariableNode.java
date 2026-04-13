package compiler.Parser;

public class VariableNode extends ASTNode {
    public String name;

    public VariableNode(String name) {
        this.name = name;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Variable, " + name);
    }
}