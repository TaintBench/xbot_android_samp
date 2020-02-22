package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjToIntMap implements Serializable {
    private static final int A = -1640531527;
    /* access modifiers changed from: private|static|final */
    public static final Object DELETED = new Object();
    private static final boolean check = false;
    static final long serialVersionUID = -1542220580748809402L;
    private int keyCount;
    private transient Object[] keys;
    private transient int occupiedCount;
    private int power;
    private transient int[] values;

    public static class Iterator {
        private int cursor;
        private Object[] keys;
        ObjToIntMap master;
        private int remaining;
        private int[] values;

        Iterator(ObjToIntMap master) {
            this.master = master;
        }

        /* access modifiers changed from: final */
        public final void init(Object[] keys, int[] values, int keyCount) {
            this.keys = keys;
            this.values = values;
            this.cursor = -1;
            this.remaining = keyCount;
        }

        public void start() {
            this.master.initIterator(this);
            next();
        }

        public boolean done() {
            return this.remaining < 0;
        }

        public void next() {
            if (this.remaining == -1) {
                Kit.codeBug();
            }
            if (this.remaining == 0) {
                this.remaining = -1;
                this.cursor = -1;
                return;
            }
            this.cursor++;
            while (true) {
                Object key = this.keys[this.cursor];
                if (key == null || key == ObjToIntMap.DELETED) {
                    this.cursor++;
                } else {
                    this.remaining--;
                    return;
                }
            }
        }

        public Object getKey() {
            UniqueTag key = this.keys[this.cursor];
            if (key == UniqueTag.NULL_VALUE) {
                return null;
            }
            return key;
        }

        public int getValue() {
            return this.values[this.cursor];
        }

        public void setValue(int value) {
            this.values[this.cursor] = value;
        }
    }

    public ObjToIntMap() {
        this(4);
    }

    public ObjToIntMap(int keyCountHint) {
        if (keyCountHint < 0) {
            Kit.codeBug();
        }
        int i = 2;
        while ((1 << i) < (keyCountHint * 4) / 3) {
            i++;
        }
        this.power = i;
    }

    public boolean isEmpty() {
        return this.keyCount == 0;
    }

    public int size() {
        return this.keyCount;
    }

    public boolean has(Object key) {
        if (key == null) {
            key = UniqueTag.NULL_VALUE;
        }
        return findIndex(key) >= 0;
    }

    public int get(Object key, int defaultValue) {
        if (key == null) {
            key = UniqueTag.NULL_VALUE;
        }
        int index = findIndex(key);
        if (index >= 0) {
            return this.values[index];
        }
        return defaultValue;
    }

    public int getExisting(Object key) {
        if (key == null) {
            key = UniqueTag.NULL_VALUE;
        }
        int index = findIndex(key);
        if (index >= 0) {
            return this.values[index];
        }
        Kit.codeBug();
        return 0;
    }

    public void put(Object key, int value) {
        if (key == null) {
            key = UniqueTag.NULL_VALUE;
        }
        this.values[ensureIndex(key)] = value;
    }

    public Object intern(Object keyArg) {
        boolean nullKey = false;
        if (keyArg == null) {
            nullKey = true;
            keyArg = UniqueTag.NULL_VALUE;
        }
        int index = ensureIndex(keyArg);
        this.values[index] = 0;
        return nullKey ? null : this.keys[index];
    }

    public void remove(Object key) {
        if (key == null) {
            key = UniqueTag.NULL_VALUE;
        }
        int index = findIndex(key);
        if (index >= 0) {
            this.keys[index] = DELETED;
            this.keyCount--;
        }
    }

    public void clear() {
        int i = this.keys.length;
        while (i != 0) {
            i--;
            this.keys[i] = null;
        }
        this.keyCount = 0;
        this.occupiedCount = 0;
    }

    public Iterator newIterator() {
        return new Iterator(this);
    }

    /* access modifiers changed from: final */
    public final void initIterator(Iterator i) {
        i.init(this.keys, this.values, this.keyCount);
    }

    public Object[] getKeys() {
        Object[] array = new Object[this.keyCount];
        getKeys(array, 0);
        return array;
    }

    public void getKeys(Object[] array, int offset) {
        int count = this.keyCount;
        int i = 0;
        while (count != 0) {
            Object key = this.keys[i];
            if (!(key == null || key == DELETED)) {
                if (key == UniqueTag.NULL_VALUE) {
                    key = null;
                }
                array[offset] = key;
                offset++;
                count--;
            }
            i++;
        }
    }

    private static int tableLookupStep(int fraction, int mask, int power) {
        int shift = 32 - (power * 2);
        if (shift >= 0) {
            return ((fraction >>> shift) & mask) | 1;
        }
        return ((mask >>> (-shift)) & fraction) | 1;
    }

    private int findIndex(Object key) {
        if (this.keys != null) {
            int hash = key.hashCode();
            int fraction = hash * A;
            int index = fraction >>> (32 - this.power);
            Object test = this.keys[index];
            if (test != null) {
                int N = 1 << this.power;
                if (test == key) {
                    return index;
                }
                if (this.values[N + index] == hash && test.equals(key)) {
                    return index;
                }
                int mask = N - 1;
                int step = tableLookupStep(fraction, mask, this.power);
                while (true) {
                    index = (index + step) & mask;
                    test = this.keys[index];
                    if (test == null) {
                        break;
                    } else if (test == key) {
                        return index;
                    } else {
                        if (this.values[N + index] == hash && test.equals(key)) {
                            return index;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private int insertNewKey(Object key, int hash) {
        int fraction = hash * A;
        int index = fraction >>> (32 - this.power);
        int N = 1 << this.power;
        if (this.keys[index] != null) {
            int mask = N - 1;
            int step = tableLookupStep(fraction, mask, this.power);
            int firstIndex = index;
            do {
                index = (index + step) & mask;
            } while (this.keys[index] != null);
        }
        this.keys[index] = key;
        this.values[N + index] = hash;
        this.occupiedCount++;
        this.keyCount++;
        return index;
    }

    private void rehashTable() {
        int N;
        if (this.keys == null) {
            N = 1 << this.power;
            this.keys = new Object[N];
            this.values = new int[(N * 2)];
            return;
        }
        if (this.keyCount * 2 >= this.occupiedCount) {
            this.power++;
        }
        N = 1 << this.power;
        Object[] oldKeys = this.keys;
        int[] oldValues = this.values;
        int oldN = oldKeys.length;
        this.keys = new Object[N];
        this.values = new int[(N * 2)];
        int remaining = this.keyCount;
        this.keyCount = 0;
        this.occupiedCount = 0;
        int i = 0;
        while (remaining != 0) {
            Object key = oldKeys[i];
            if (!(key == null || key == DELETED)) {
                this.values[insertNewKey(key, oldValues[oldN + i])] = oldValues[i];
                remaining--;
            }
            i++;
        }
    }

    private int ensureIndex(Object key) {
        int hash = key.hashCode();
        int index = -1;
        int firstDeleted = -1;
        if (this.keys != null) {
            int fraction = hash * A;
            index = fraction >>> (32 - this.power);
            Object test = this.keys[index];
            if (test != null) {
                int N = 1 << this.power;
                if (test == key || (this.values[N + index] == hash && test.equals(key))) {
                    return index;
                }
                if (test == DELETED) {
                    firstDeleted = index;
                }
                int mask = N - 1;
                int step = tableLookupStep(fraction, mask, this.power);
                while (true) {
                    index = (index + step) & mask;
                    test = this.keys[index];
                    if (test == null) {
                        break;
                    } else if (test != key && (this.values[N + index] != hash || !test.equals(key))) {
                        if (test == DELETED && firstDeleted < 0) {
                            firstDeleted = index;
                        }
                    }
                }
                return index;
            }
        }
        if (firstDeleted >= 0) {
            index = firstDeleted;
        } else if (this.keys == null || this.occupiedCount * 4 >= (1 << this.power) * 3) {
            rehashTable();
            return insertNewKey(key, hash);
        } else {
            this.occupiedCount++;
        }
        this.keys[index] = key;
        this.values[(1 << this.power) + index] = hash;
        this.keyCount++;
        return index;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        int count = this.keyCount;
        int i = 0;
        while (count != 0) {
            Object key = this.keys[i];
            if (!(key == null || key == DELETED)) {
                count--;
                out.writeObject(key);
                out.writeInt(this.values[i]);
            }
            i++;
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int writtenKeyCount = this.keyCount;
        if (writtenKeyCount != 0) {
            this.keyCount = 0;
            int N = 1 << this.power;
            this.keys = new Object[N];
            this.values = new int[(N * 2)];
            for (int i = 0; i != writtenKeyCount; i++) {
                Object key = in.readObject();
                this.values[insertNewKey(key, key.hashCode())] = in.readInt();
            }
        }
    }
}
