package compiler.Parser;
public class FloatNode extends ASTNode {
    double value;
    public FloatNode(double value) { this.value = value; }
    @Override
    public void print(String prefix) {
        System.out.println(prefix + "Float, " + value);
    }
}