package org.mozilla.classfile;

/* compiled from: ClassFileWriter */
final class TypeInfo {
    static final int DOUBLE = 3;
    static final int FLOAT = 2;
    static final int INTEGER = 1;
    static final int LONG = 4;
    static final int NULL = 5;
    static final int OBJECT_TAG = 7;
    static final int TOP = 0;
    static final int UNINITIALIZED_THIS = 6;
    static final int UNINITIALIZED_VAR_TAG = 8;

    private TypeInfo() {
    }

    static final int OBJECT(int constantPoolIndex) {
        return ((65535 & constantPoolIndex) << 8) | 7;
    }

    static final int OBJECT(String type, ConstantPool pool) {
        return OBJECT(pool.addClass(type));
    }

    static final int UNINITIALIZED_VARIABLE(int bytecodeOffset) {
        return ((65535 & bytecodeOffset) << 8) | 8;
    }

    static final int getTag(int typeInfo) {
        return typeInfo & ByteCode.IMPDEP2;
    }

    static final int getPayload(int typeInfo) {
        return typeInfo >>> 8;
    }

    static final String getPayloadAsType(int typeInfo, ConstantPool pool) {
        if (getTag(typeInfo) == 7) {
            return (String) pool.getConstantData(getPayload(typeInfo));
        }
        throw new IllegalArgumentException("expecting object type");
    }

    static final int fromType(String type, ConstantPool pool) {
        if (type.length() != 1) {
            return OBJECT(type, pool);
        }
        switch (type.charAt(0)) {
            case 'B':
            case 'C':
            case 'I':
            case 'S':
            case 'Z':
                return 1;
            case 'D':
                return 3;
            case 'F':
                return 2;
            case 'J':
                return 4;
            default:
                throw new IllegalArgumentException("bad type");
        }
    }

    static boolean isTwoWords(int type) {
        return type == 3 || type == 4;
    }

    static int merge(int current, int incoming, ConstantPool pool) {
        int currentTag = getTag(current);
        int incomingTag = getTag(incoming);
        boolean currentIsObject = currentTag == 7;
        boolean incomingIsObject = incomingTag == 7;
        if (current == incoming || (currentIsObject && incoming == 5)) {
            return current;
        }
        if (currentTag == 0 || incomingTag == 0) {
            return 0;
        }
        if (current == 5 && incomingIsObject) {
            return incoming;
        }
        if (currentIsObject && incomingIsObject) {
            String currentName = getPayloadAsType(current, pool);
            String incomingName = getPayloadAsType(incoming, pool);
            String currentlyGeneratedName = (String) pool.getConstantData(2);
            String currentlyGeneratedSuperName = (String) pool.getConstantData(4);
            if (currentName.equals(currentlyGeneratedName)) {
                currentName = currentlyGeneratedSuperName;
            }
            if (incomingName.equals(currentlyGeneratedName)) {
                incomingName = currentlyGeneratedSuperName;
            }
            Class<?> currentClass = getClassFromInternalName(currentName);
            Class<?> incomingClass = getClassFromInternalName(incomingName);
            if (currentClass.isAssignableFrom(incomingClass)) {
                return current;
            }
            if (incomingClass.isAssignableFrom(currentClass)) {
                return incoming;
            }
            if (incomingClass.isInterface() || currentClass.isInterface()) {
                return OBJECT("java/lang/Object", pool);
            }
            for (Class<?> commonClass = incomingClass.getSuperclass(); commonClass != null; commonClass = commonClass.getSuperclass()) {
                if (commonClass.isAssignableFrom(currentClass)) {
                    return OBJECT(ClassFileWriter.getSlashedForm(commonClass.getName()), pool);
                }
            }
        }
        throw new IllegalArgumentException("bad merge attempt between " + toString(current, pool) + " and " + toString(incoming, pool));
    }

    static String toString(int type, ConstantPool pool) {
        int tag = getTag(type);
        switch (tag) {
            case 0:
                return "top";
            case 1:
                return "int";
            case 2:
                return "float";
            case 3:
                return "double";
            case 4:
                return "long";
            case 5:
                return "null";
            case 6:
                return "uninitialized_this";
            default:
                if (tag == 7) {
                    return getPayloadAsType(type, pool);
                }
                if (tag == 8) {
                    return "uninitialized";
                }
                throw new IllegalArgumentException("bad type");
        }
    }

    static Class<?> getClassFromInternalName(String internalName) {
        try {
            return Class.forName(internalName.replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static String toString(int[] types, ConstantPool pool) {
        return toString(types, types.length, pool);
    }

    static String toString(int[] types, int typesTop, ConstantPool pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < typesTop; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(toString(types[i], pool));
        }
        sb.append("]");
        return sb.toString();
    }

    static void print(int[] locals, int[] stack, ConstantPool pool) {
        print(locals, locals.length, stack, stack.length, pool);
    }

    static void print(int[] locals, int localsTop, int[] stack, int stackTop, ConstantPool pool) {
        System.out.print("locals: ");
        System.out.println(toString(locals, localsTop, pool));
        System.out.print("stack: ");
        System.out.println(toString(stack, stackTop, pool));
        System.out.println();
    }
}
