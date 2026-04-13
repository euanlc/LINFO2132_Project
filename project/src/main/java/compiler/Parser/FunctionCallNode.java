package compiler.Parser;
import java.util.List;

public class FunctionCallNode extends ASTNode {
    public String functionName;
    public List<ASTNode> arguments;

    public FunctionCallNode(String functionName, List<ASTNode> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    @Override
    public void print(String prefix) {
        System.out.println(prefix + "FunctionCall, " + functionName);
        for (ASTNode arg : arguments) arg.print(prefix + "  ");
    }
}