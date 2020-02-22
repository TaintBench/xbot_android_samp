package org.mozilla.javascript;

import java.io.Serializable;

public final class UniqueTag implements Serializable {
    public static final UniqueTag DOUBLE_MARK = new UniqueTag(3);
    private static final int ID_DOUBLE_MARK = 3;
    private static final int ID_NOT_FOUND = 1;
    private static final int ID_NULL_VALUE = 2;
    public static final UniqueTag NOT_FOUND = new UniqueTag(1);
    public static final UniqueTag NULL_VALUE = new UniqueTag(2);
    static final long serialVersionUID = -4320556826714577259L;
    private final int tagId;

    private UniqueTag(int tagId) {
        this.tagId = tagId;
    }

    public Object readResolve() {
        switch (this.tagId) {
            case 1:
                return NOT_FOUND;
            case 2:
                return NULL_VALUE;
            case 3:
                return DOUBLE_MARK;
            default:
                throw new IllegalStateException(String.valueOf(this.tagId));
        }
    }

    public String toString() {
        String name;
        switch (this.tagId) {
            case 1:
                name = "NOT_FOUND";
                break;
            case 2:
                name = "NULL_VALUE";
                break;
            case 3:
                name = "DOUBLE_MARK";
                break;
            default:
                throw Kit.codeBug();
        }
        return super.toString() + ": " + name;
    }
}
