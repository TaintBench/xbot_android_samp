package org.mozilla.javascript.ast;

public class NumberLiteral extends AstNode {
    private double number;
    private String value;

    public NumberLiteral() {
        this.type = 40;
    }

    public NumberLiteral(int pos) {
        super(pos);
        this.type = 40;
    }

    public NumberLiteral(int pos, int len) {
        super(pos, len);
        this.type = 40;
    }

    public NumberLiteral(int pos, String value) {
        super(pos);
        this.type = 40;
        setValue(value);
        setLength(value.length());
    }

    public NumberLiteral(int pos, String value, double number) {
        this(pos, value);
        setDouble(number);
    }

    public NumberLiteral(double number) {
        this.type = 40;
        setDouble(number);
        setValue(Double.toString(number));
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        assertNotNull(value);
        this.value = value;
    }

    public double getNumber() {
        return this.number;
    }

    public void setNumber(double value) {
        this.number = value;
    }

    public String toSource(int depth) {
        return makeIndent(depth) + (this.value == null ? "<null>" : this.value);
    }

    public void visit(NodeVisitor v) {
        v.visit(this);
    }
}
