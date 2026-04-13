package compiler.Parser;
import java.util.List;

public class FunctionNode extends ASTNode {
    public String returnType;
    public String name;
    public List<ASTNode> parameters;
    public BlockNode body;

    public FunctionNode(String returnType, String name, List<ASTNode> parameters, BlockNode body) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public void print(String prefix) {
        System.out.println(prefix + "Function, " + name);
        for (ASTNode param : parameters) param.print(prefix + "  ");
        if (body != null) body.print(prefix + "  ");
    }
}