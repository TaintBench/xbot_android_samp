package org.mozilla.javascript.ast;

public class PropertyGet extends InfixExpression {
    public PropertyGet() {
        this.type = 33;
    }

    public PropertyGet(int pos) {
        super(pos);
        this.type = 33;
    }

    public PropertyGet(int pos, int len) {
        super(pos, len);
        this.type = 33;
    }

    public PropertyGet(int pos, int len, AstNode target, Name property) {
        super(pos, len, target, (AstNode) property);
        this.type = 33;
    }

    public PropertyGet(AstNode target, Name property) {
        super(target, (AstNode) property);
        this.type = 33;
    }

    public PropertyGet(AstNode target, Name property, int dotPosition) {
        super(33, target, (AstNode) property, dotPosition);
        this.type = 33;
    }

    public AstNode getTarget() {
        return getLeft();
    }

    public void setTarget(AstNode target) {
        setLeft(target);
    }

    public Name getProperty() {
        return (Name) getRight();
    }

    public void setProperty(Name property) {
        setRight(property);
    }

    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append(getLeft().toSource(0));
        sb.append(".");
        sb.append(getRight().toSource(0));
        return sb.toString();
    }

    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            getTarget().visit(v);
            getProperty().visit(v);
        }
    }
}
