package compiler.Parser;

public class VarDeclNode extends ASTNode {
    String type;
    String name;
    ASTNode expression;

    public VarDeclNode(String type, String name, ASTNode expression) {
        this.type = type;
        this.name = name;
        this.expression = expression;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Declaration");
        System.out.println(indent + "  Type, " + type);
        System.out.println(indent + "  Identifier, " + name);
        if (expression != null) {
            System.out.println(indent + "  Assignment");
            expression.print(indent + "    ");
        }
    }
}