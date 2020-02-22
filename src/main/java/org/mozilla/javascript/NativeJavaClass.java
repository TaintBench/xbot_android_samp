package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Map;

public class NativeJavaClass extends NativeJavaObject implements Function {
    static final String javaClassPropertyName = "__javaObject__";
    static final long serialVersionUID = -6460763940409461664L;
    private Map<String, FieldAndMethods> staticFieldAndMethods;

    public NativeJavaClass(Scriptable scope, Class<?> cl) {
        this(scope, cl, false);
    }

    public NativeJavaClass(Scriptable scope, Class<?> cl, boolean isAdapter) {
        super(scope, cl, null, isAdapter);
    }

    /* access modifiers changed from: protected */
    public void initMembers() {
        Class<?> cl = this.javaObject;
        this.members = JavaMembers.lookupClass(this.parent, cl, cl, this.isAdapter);
        this.staticFieldAndMethods = this.members.getFieldAndMethodsObjects(this, cl, true);
    }

    public String getClassName() {
        return "JavaClass";
    }

    public boolean has(String name, Scriptable start) {
        return this.members.has(name, true) || javaClassPropertyName.equals(name);
    }

    public Object get(String name, Scriptable start) {
        if (name.equals("prototype")) {
            return null;
        }
        if (this.staticFieldAndMethods != null) {
            Object result = this.staticFieldAndMethods.get(name);
            if (result != null) {
                return result;
            }
        }
        if (this.members.has(name, true)) {
            return this.members.get(this, name, this.javaObject, true);
        }
        Context cx = Context.getContext();
        Scriptable scope = ScriptableObject.getTopLevelScope(start);
        WrapFactory wrapFactory = cx.getWrapFactory();
        if (javaClassPropertyName.equals(name)) {
            return wrapFactory.wrap(cx, scope, this.javaObject, ScriptRuntime.ClassClass);
        }
        Class<?> nestedClass = findNestedClass(getClassObject(), name);
        if (nestedClass != null) {
            Scriptable nestedValue = wrapFactory.wrapJavaClass(cx, scope, nestedClass);
            nestedValue.setParentScope(this);
            return nestedValue;
        }
        throw this.members.reportMemberNotFound(name);
    }

    public void put(String name, Scriptable start, Object value) {
        this.members.put(this, name, this.javaObject, value, true);
    }

    public Object[] getIds() {
        return this.members.getIds(true);
    }

    public Class<?> getClassObject() {
        return (Class) super.unwrap();
    }

    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || hint == ScriptRuntime.StringClass) {
            return toString();
        }
        if (hint == ScriptRuntime.BooleanClass) {
            return Boolean.TRUE;
        }
        if (hint == ScriptRuntime.NumberClass) {
            return ScriptRuntime.NaNobj;
        }
        return this;
    }

    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length != 1 || !(args[0] instanceof Scriptable)) {
            return construct(cx, scope, args);
        }
        Class<?> c = getClassObject();
        Scriptable p = (Scriptable) args[0];
        do {
            if ((p instanceof Wrapper) && c.isInstance(((Wrapper) p).unwrap())) {
                return p;
            }
            p = p.getPrototype();
        } while (p != null);
        return construct(cx, scope, args);
    }

    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        Class<?> classObject = getClassObject();
        int modifiers = classObject.getModifiers();
        if (!Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers)) {
            NativeJavaMethod ctors = this.members.ctors;
            int index = ctors.findCachedFunction(cx, args);
            if (index >= 0) {
                return constructSpecific(cx, scope, args, ctors.methods[index]);
            }
            throw Context.reportRuntimeError2("msg.no.java.ctor", classObject.getName(), NativeJavaMethod.scriptSignature(args));
        } else if (args.length == 0) {
            throw Context.reportRuntimeError0("msg.adapter.zero.args");
        } else {
            Scriptable topLevel = ScriptableObject.getTopLevelScope(this);
            String msg = "";
            try {
                if ("Dalvik".equals(System.getProperty("java.vm.name")) && classObject.isInterface()) {
                    return cx.getWrapFactory().wrapAsJavaObject(cx, scope, NativeJavaObject.createInterfaceAdapter(classObject, ScriptableObject.ensureScriptableObject(args[0])), null);
                }
                Object v = topLevel.get("JavaAdapter", topLevel);
                if (v != NOT_FOUND) {
                    return ((Function) v).construct(cx, topLevel, new Object[]{this, args[0]});
                }
                throw Context.reportRuntimeError2("msg.cant.instantiate", msg, classObject.getName());
            } catch (Exception ex) {
                String m = ex.getMessage();
                if (m != null) {
                    msg = m;
                }
            }
        }
    }

    static Scriptable constructSpecific(Context cx, Scriptable scope, Object[] args, MemberBox ctor) {
        Object instance = constructInternal(args, ctor);
        return cx.getWrapFactory().wrapNewObject(cx, ScriptableObject.getTopLevelScope(scope), instance);
    }

    static Object constructInternal(Object[] args, MemberBox ctor) {
        Class<?>[] argTypes = ctor.argTypes;
        int i;
        if (ctor.vararg) {
            Object varArgs;
            Object[] newArgs = new Object[argTypes.length];
            for (i = 0; i < argTypes.length - 1; i++) {
                newArgs[i] = Context.jsToJava(args[i], argTypes[i]);
            }
            if (args.length == argTypes.length && (args[args.length - 1] == null || (args[args.length - 1] instanceof NativeArray) || (args[args.length - 1] instanceof NativeJavaArray))) {
                varArgs = Context.jsToJava(args[args.length - 1], argTypes[argTypes.length - 1]);
            } else {
                Class<?> componentType = argTypes[argTypes.length - 1].getComponentType();
                varArgs = Array.newInstance(componentType, (args.length - argTypes.length) + 1);
                for (i = 0; i < Array.getLength(varArgs); i++) {
                    Array.set(varArgs, i, Context.jsToJava(args[(argTypes.length - 1) + i], componentType));
                }
            }
            newArgs[argTypes.length - 1] = varArgs;
            args = newArgs;
        } else {
            Object[] origArgs = args;
            for (i = 0; i < args.length; i++) {
                Object arg = args[i];
                Object x = Context.jsToJava(arg, argTypes[i]);
                if (x != arg) {
                    if (args == origArgs) {
                        args = (Object[]) origArgs.clone();
                    }
                    args[i] = x;
                }
            }
        }
        return ctor.newInstance(args);
    }

    public String toString() {
        return "[JavaClass " + getClassObject().getName() + "]";
    }

    public boolean hasInstance(Scriptable value) {
        if (!(value instanceof Wrapper) || (value instanceof NativeJavaClass)) {
            return false;
        }
        return getClassObject().isInstance(((Wrapper) value).unwrap());
    }

    private static Class<?> findNestedClass(Class<?> parentClass, String name) {
        String nestedClassName = parentClass.getName() + '$' + name;
        ClassLoader loader = parentClass.getClassLoader();
        if (loader == null) {
            return Kit.classOrNull(nestedClassName);
        }
        return Kit.classOrNull(loader, nestedClassName);
    }
}
