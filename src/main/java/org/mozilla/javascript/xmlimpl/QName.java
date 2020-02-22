package org.mozilla.javascript.xmlimpl;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

final class QName extends IdScriptableObject {
    private static final int Id_constructor = 1;
    private static final int Id_localName = 1;
    private static final int Id_toSource = 3;
    private static final int Id_toString = 2;
    private static final int Id_uri = 2;
    private static final int MAX_INSTANCE_ID = 2;
    private static final int MAX_PROTOTYPE_ID = 3;
    private static final Object QNAME_TAG = "QName";
    static final long serialVersionUID = 416745167693026750L;
    private QName delegate;
    private XMLLibImpl lib;
    private QName prototype;

    private QName() {
    }

    static QName create(XMLLibImpl lib, Scriptable scope, QName prototype, QName delegate) {
        QName rv = new QName();
        rv.lib = lib;
        rv.setParentScope(scope);
        rv.prototype = prototype;
        rv.setPrototype(prototype);
        rv.delegate = delegate;
        return rv;
    }

    /* access modifiers changed from: 0000 */
    public void exportAsJSClass(boolean sealed) {
        exportAsJSClass(3, getParentScope(), sealed);
    }

    public String toString() {
        if (this.delegate.getNamespace() == null) {
            return "*::" + localName();
        }
        if (this.delegate.getNamespace().isGlobal()) {
            return localName();
        }
        return uri() + "::" + localName();
    }

    public String localName() {
        if (this.delegate.getLocalName() == null) {
            return "*";
        }
        return this.delegate.getLocalName();
    }

    /* access modifiers changed from: 0000 */
    public String prefix() {
        if (this.delegate.getNamespace() == null) {
            return null;
        }
        return this.delegate.getNamespace().getPrefix();
    }

    /* access modifiers changed from: 0000 */
    public String uri() {
        if (this.delegate.getNamespace() == null) {
            return null;
        }
        return this.delegate.getNamespace().getUri();
    }

    /* access modifiers changed from: final */
    @Deprecated
    public final QName toNodeQname() {
        return this.delegate;
    }

    /* access modifiers changed from: final */
    public final QName getDelegate() {
        return this.delegate;
    }

    public boolean equals(Object obj) {
        if (obj instanceof QName) {
            return equals((QName) obj);
        }
        return false;
    }

    public int hashCode() {
        return this.delegate.hashCode();
    }

    /* access modifiers changed from: protected */
    public Object equivalentValues(Object value) {
        if (value instanceof QName) {
            return equals((QName) value) ? Boolean.TRUE : Boolean.FALSE;
        } else {
            return Scriptable.NOT_FOUND;
        }
    }

    private boolean equals(QName q) {
        return this.delegate.equals(q.delegate);
    }

    public String getClassName() {
        return "QName";
    }

    public Object getDefaultValue(Class<?> cls) {
        return toString();
    }

    /* access modifiers changed from: protected */
    public int getMaxInstanceId() {
        return super.getMaxInstanceId() + 2;
    }

    /* access modifiers changed from: protected */
    public int findInstanceIdInfo(String s) {
        int id = 0;
        String X = null;
        int s_length = s.length();
        if (s_length == 3) {
            X = "uri";
            id = 2;
        } else if (s_length == 9) {
            X = "localName";
            id = 1;
        }
        if (!(X == null || X == s || X.equals(s))) {
            id = 0;
        }
        if (id == 0) {
            return super.findInstanceIdInfo(s);
        }
        switch (id) {
            case 1:
            case 2:
                return IdScriptableObject.instanceIdInfo(5, super.getMaxInstanceId() + id);
            default:
                throw new IllegalStateException();
        }
    }

    /* access modifiers changed from: protected */
    public String getInstanceIdName(int id) {
        switch (id - super.getMaxInstanceId()) {
            case 1:
                return "localName";
            case 2:
                return "uri";
            default:
                return super.getInstanceIdName(id);
        }
    }

    /* access modifiers changed from: protected */
    public Object getInstanceIdValue(int id) {
        switch (id - super.getMaxInstanceId()) {
            case 1:
                return localName();
            case 2:
                return uri();
            default:
                return super.getInstanceIdValue(id);
        }
    }

    /* access modifiers changed from: protected */
    public int findPrototypeId(String s) {
        int id = 0;
        String X = null;
        int s_length = s.length();
        if (s_length == 8) {
            int c = s.charAt(3);
            if (c == 111) {
                X = "toSource";
                id = 3;
            } else if (c == 116) {
                X = "toString";
                id = 2;
            }
        } else if (s_length == 11) {
            X = "constructor";
            id = 1;
        }
        if (X == null || X == s || X.equals(s)) {
            return id;
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    public void initPrototypeId(int id) {
        int arity;
        String s;
        switch (id) {
            case 1:
                arity = 2;
                s = "constructor";
                break;
            case 2:
                arity = 0;
                s = "toString";
                break;
            case 3:
                arity = 0;
                s = "toSource";
                break;
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
        initPrototypeMethod(QNAME_TAG, id, s, arity);
    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (!f.hasTag(QNAME_TAG)) {
            return super.execIdCall(f, cx, scope, thisObj, args);
        }
        int id = f.methodId();
        switch (id) {
            case 1:
                return jsConstructor(cx, thisObj == null, args);
            case 2:
                return realThis(thisObj, f).toString();
            case 3:
                return realThis(thisObj, f).js_toSource();
            default:
                throw new IllegalArgumentException(String.valueOf(id));
        }
    }

    private QName realThis(Scriptable thisObj, IdFunctionObject f) {
        if (thisObj instanceof QName) {
            return (QName) thisObj;
        }
        throw IdScriptableObject.incompatibleCallError(f);
    }

    /* access modifiers changed from: 0000 */
    public QName newQName(XMLLibImpl lib, String q_uri, String q_localName, String q_prefix) {
        Namespace ns;
        QName prototype = this.prototype;
        if (prototype == null) {
            prototype = this;
        }
        if (q_prefix != null) {
            ns = Namespace.create(q_prefix, q_uri);
        } else if (q_uri != null) {
            ns = Namespace.create(q_uri);
        } else {
            ns = null;
        }
        if (q_localName != null && q_localName.equals("*")) {
            q_localName = null;
        }
        return create(lib, getParentScope(), prototype, QName.create(ns, q_localName));
    }

    /* access modifiers changed from: 0000 */
    public QName constructQName(XMLLibImpl lib, Context cx, Object namespace, Object name) {
        String nameString;
        String q_uri;
        String q_prefix;
        if (name instanceof QName) {
            if (namespace == Undefined.instance) {
                return (QName) name;
            }
            nameString = ((QName) name).localName();
        }
        if (name == Undefined.instance) {
            nameString = "";
        } else {
            nameString = ScriptRuntime.toString(name);
        }
        if (namespace == Undefined.instance) {
            if ("*".equals(nameString)) {
                namespace = null;
            } else {
                namespace = lib.getDefaultNamespace(cx);
            }
        }
        Namespace namespaceNamespace = null;
        if (namespace != null) {
            if (namespace instanceof Namespace) {
                namespaceNamespace = (Namespace) namespace;
            } else {
                namespaceNamespace = lib.newNamespace(ScriptRuntime.toString(namespace));
            }
        }
        String q_localName = nameString;
        if (namespace == null) {
            q_uri = null;
            q_prefix = null;
        } else {
            q_uri = namespaceNamespace.uri();
            q_prefix = namespaceNamespace.prefix();
        }
        return newQName(lib, q_uri, q_localName, q_prefix);
    }

    /* access modifiers changed from: 0000 */
    public QName constructQName(XMLLibImpl lib, Context cx, Object nameValue) {
        return constructQName(lib, cx, Undefined.instance, nameValue);
    }

    /* access modifiers changed from: 0000 */
    public QName castToQName(XMLLibImpl lib, Context cx, Object qnameValue) {
        if (qnameValue instanceof QName) {
            return (QName) qnameValue;
        }
        return constructQName(lib, cx, qnameValue);
    }

    private Object jsConstructor(Context cx, boolean inNewExpr, Object[] args) {
        if (!inNewExpr && args.length == 1) {
            return castToQName(this.lib, cx, args[0]);
        }
        if (args.length == 0) {
            return constructQName(this.lib, cx, Undefined.instance);
        }
        if (args.length == 1) {
            return constructQName(this.lib, cx, args[0]);
        }
        return constructQName(this.lib, cx, args[0], args[1]);
    }

    private String js_toSource() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        toSourceImpl(uri(), localName(), prefix(), sb);
        sb.append(')');
        return sb.toString();
    }

    private static void toSourceImpl(String uri, String localName, String prefix, StringBuilder sb) {
        sb.append("new QName(");
        if (uri != null || prefix != null) {
            Namespace.toSourceImpl(prefix, uri, sb);
            sb.append(", ");
        } else if (!"*".equals(localName)) {
            sb.append("null, ");
        }
        sb.append('\'');
        sb.append(ScriptRuntime.escapeString(localName, '\''));
        sb.append("')");
    }
}
