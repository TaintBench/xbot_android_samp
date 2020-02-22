package com.google.gson.internal.bind;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.C$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
    private final ConstructorConstructor constructorConstructor;
    private final Excluder excluder;
    private final FieldNamingStrategy fieldNamingPolicy;

    static abstract class BoundField {
        final boolean deserialized;
        final String name;
        final boolean serialized;

        protected BoundField(String str, boolean z, boolean z2) {
            this.name = str;
            this.serialized = z;
            this.deserialized = z2;
        }

        public abstract void read(JsonReader jsonReader, Object obj) throws IOException, IllegalAccessException;

        public abstract void write(JsonWriter jsonWriter, Object obj) throws IOException, IllegalAccessException;

        public abstract boolean writeField(Object obj) throws IOException, IllegalAccessException;
    }

    public static final class Adapter<T> extends TypeAdapter<T> {
        private final Map<String, BoundField> boundFields;
        private final ObjectConstructor<T> constructor;

        private Adapter(ObjectConstructor<T> objectConstructor, Map<String, BoundField> map) {
            this.constructor = objectConstructor;
            this.boundFields = map;
        }

        /* synthetic */ Adapter(ObjectConstructor objectConstructor, Map map, AnonymousClass1 anonymousClass1) {
            this(objectConstructor, map);
        }

        public T read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            T construct = this.constructor.construct();
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    BoundField boundField = (BoundField) this.boundFields.get(jsonReader.nextName());
                    if (boundField == null || !boundField.deserialized) {
                        jsonReader.skipValue();
                    } else {
                        boundField.read(jsonReader, construct);
                    }
                }
                jsonReader.endObject();
                return construct;
            } catch (IllegalStateException e) {
                throw new JsonSyntaxException(e);
            } catch (IllegalAccessException e2) {
                throw new AssertionError(e2);
            }
        }

        public void write(JsonWriter jsonWriter, T t) throws IOException {
            if (t == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.beginObject();
            try {
                for (BoundField boundField : this.boundFields.values()) {
                    if (boundField.writeField(t)) {
                        jsonWriter.name(boundField.name);
                        boundField.write(jsonWriter, t);
                    }
                }
                jsonWriter.endObject();
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
    }

    public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, FieldNamingStrategy fieldNamingStrategy, Excluder excluder) {
        this.constructorConstructor = constructorConstructor;
        this.fieldNamingPolicy = fieldNamingStrategy;
        this.excluder = excluder;
    }

    private BoundField createBoundField(Gson gson, Field field, String str, TypeToken<?> typeToken, boolean z, boolean z2) {
        final boolean isPrimitive = Primitives.isPrimitive(typeToken.getRawType());
        final Gson gson2 = gson;
        final Field field2 = field;
        final TypeToken<?> typeToken2 = typeToken;
        return new BoundField(str, z, z2) {
            final TypeAdapter<?> typeAdapter = ReflectiveTypeAdapterFactory.this.getFieldAdapter(gson2, field2, typeToken2);

            /* access modifiers changed from: 0000 */
            public void read(JsonReader jsonReader, Object obj) throws IOException, IllegalAccessException {
                Object read = this.typeAdapter.read(jsonReader);
                if (read != null || !isPrimitive) {
                    field2.set(obj, read);
                }
            }

            /* access modifiers changed from: 0000 */
            public void write(JsonWriter jsonWriter, Object obj) throws IOException, IllegalAccessException {
                new TypeAdapterRuntimeTypeWrapper(gson2, this.typeAdapter, typeToken2.getType()).write(jsonWriter, field2.get(obj));
            }

            public boolean writeField(Object obj) throws IOException, IllegalAccessException {
                return this.serialized && field2.get(obj) != obj;
            }
        };
    }

    static boolean excludeField(Field field, boolean z, Excluder excluder) {
        return (excluder.excludeClass(field.getType(), z) || excluder.excludeField(field, z)) ? false : true;
    }

    private Map<String, BoundField> getBoundFields(Gson gson, TypeToken<?> typeToken, Class<?> cls) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        if (cls.isInterface()) {
            return linkedHashMap;
        }
        Type type = typeToken.getType();
        Class cls2;
        while (cls2 != Object.class) {
            for (Field field : cls2.getDeclaredFields()) {
                boolean excludeField = excludeField(field, true);
                boolean excludeField2 = excludeField(field, false);
                if (excludeField || excludeField2) {
                    field.setAccessible(true);
                    BoundField createBoundField = createBoundField(gson, field, getFieldName(field), TypeToken.get(C$Gson$Types.resolve(typeToken.getType(), cls2, field.getGenericType())), excludeField, excludeField2);
                    createBoundField = (BoundField) linkedHashMap.put(createBoundField.name, createBoundField);
                    if (createBoundField != null) {
                        throw new IllegalArgumentException(type + " declares multiple JSON fields named " + createBoundField.name);
                    }
                }
            }
            TypeToken typeToken2 = TypeToken.get(C$Gson$Types.resolve(typeToken2.getType(), cls2, cls2.getGenericSuperclass()));
            cls2 = typeToken2.getRawType();
        }
        return linkedHashMap;
    }

    /* access modifiers changed from: private */
    public TypeAdapter<?> getFieldAdapter(Gson gson, Field field, TypeToken<?> typeToken) {
        JsonAdapter jsonAdapter = (JsonAdapter) field.getAnnotation(JsonAdapter.class);
        if (jsonAdapter != null) {
            TypeAdapter typeAdapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(this.constructorConstructor, gson, typeToken, jsonAdapter);
            if (typeAdapter != null) {
                return typeAdapter;
            }
        }
        return gson.getAdapter((TypeToken) typeToken);
    }

    static String getFieldName(FieldNamingStrategy fieldNamingStrategy, Field field) {
        SerializedName serializedName = (SerializedName) field.getAnnotation(SerializedName.class);
        return serializedName == null ? fieldNamingStrategy.translateName(field) : serializedName.value();
    }

    private String getFieldName(Field field) {
        return getFieldName(this.fieldNamingPolicy, field);
    }

    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class rawType = typeToken.getRawType();
        return !Object.class.isAssignableFrom(rawType) ? null : new Adapter(this.constructorConstructor.get(typeToken), getBoundFields(gson, typeToken, rawType), null);
    }

    public boolean excludeField(Field field, boolean z) {
        return excludeField(field, z, this.excluder);
    }
}