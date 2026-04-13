package compiler.Parser;

import java.util.List;

public class BlockNode extends ASTNode {
    public List<ASTNode> statements;

    public BlockNode( List<ASTNode> statements) {
        this.statements = statements;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Block");
        for (ASTNode stmt : statements) {
            if (stmt != null) {
                stmt.print(indent + "  ");
            }
        }
    }
}