package compiler.Parser;
import java.util.List;

public class CollectionNode extends ASTNode {
    public String name;
    public List<ASTNode> fields;

    public CollectionNode(String name, List<ASTNode> fields) {
        this.name = name;
        this.fields = fields;
    }

    @Override
    public void print(String prefix) {
        System.out.println(prefix + "Collection, " + name);
        for (ASTNode field : fields) field.print(prefix + "  ");
    }
}