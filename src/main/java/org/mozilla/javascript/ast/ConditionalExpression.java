package org.mozilla.javascript.ast;

public class ConditionalExpression extends AstNode {
    private int colonPosition;
    private AstNode falseExpression;
    private int questionMarkPosition;
    private AstNode testExpression;
    private AstNode trueExpression;

    public ConditionalExpression() {
        this.questionMarkPosition = -1;
        this.colonPosition = -1;
        this.type = 102;
    }

    public ConditionalExpression(int pos) {
        super(pos);
        this.questionMarkPosition = -1;
        this.colonPosition = -1;
        this.type = 102;
    }

    public ConditionalExpression(int pos, int len) {
        super(pos, len);
        this.questionMarkPosition = -1;
        this.colonPosition = -1;
        this.type = 102;
    }

    public AstNode getTestExpression() {
        return this.testExpression;
    }

    public void setTestExpression(AstNode testExpression) {
        assertNotNull(testExpression);
        this.testExpression = testExpression;
        testExpression.setParent(this);
    }

    public AstNode getTrueExpression() {
        return this.trueExpression;
    }

    public void setTrueExpression(AstNode trueExpression) {
        assertNotNull(trueExpression);
        this.trueExpression = trueExpression;
        trueExpression.setParent(this);
    }

    public AstNode getFalseExpression() {
        return this.falseExpression;
    }

    public void setFalseExpression(AstNode falseExpression) {
        assertNotNull(falseExpression);
        this.falseExpression = falseExpression;
        falseExpression.setParent(this);
    }

    public int getQuestionMarkPosition() {
        return this.questionMarkPosition;
    }

    public void setQuestionMarkPosition(int questionMarkPosition) {
        this.questionMarkPosition = questionMarkPosition;
    }

    public int getColonPosition() {
        return this.colonPosition;
    }

    public void setColonPosition(int colonPosition) {
        this.colonPosition = colonPosition;
    }

    public boolean hasSideEffects() {
        if (this.testExpression == null || this.trueExpression == null || this.falseExpression == null) {
            AstNode.codeBug();
        }
        return this.trueExpression.hasSideEffects() && this.falseExpression.hasSideEffects();
    }

    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(makeIndent(depth));
        sb.append(this.testExpression.toSource(depth));
        sb.append(" ? ");
        sb.append(this.trueExpression.toSource(0));
        sb.append(" : ");
        sb.append(this.falseExpression.toSource(0));
        return sb.toString();
    }

    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            this.testExpression.visit(v);
            this.trueExpression.visit(v);
            this.falseExpression.visit(v);
        }
    }
}
