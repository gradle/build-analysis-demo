package com.gradle.export.client.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.StringUtils;
import ratpack.util.Types;

import java.io.IOException;

@SuppressWarnings("rawtypes")
public final class LowerCaseEnumJacksonModule extends SimpleModule {

    {
        setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<Enum> modifyEnumDeserializer(
                DeserializationConfig config,
                JavaType type,
                BeanDescription beanDesc,
                JsonDeserializer deserializer
            ) {
                return new JsonDeserializer<Enum>() {
                    @Override
                    public Enum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                        JsonToken jsonToken = p.getCurrentToken();
                        if (jsonToken.isNumeric()) {
                            Object value = type.getRawClass().getEnumConstants()[p.getValueAsInt()];
                            return (Enum) value;
                        } else {
                            String value = p.getValueAsString();
                            if (StringUtils.isBlank(value)) {
                                return null;
                            } else {
                                return toEnum(type.getRawClass(), value);
                            }
                        }
                    }

                    private <T extends Enum<T>> Enum<T> toEnum(Class<?> rawClass, String value) {
                        Class<T> cast = Types.cast(rawClass);
                        return Enum.valueOf(cast, value.toUpperCase());
                    }
                };
            }
        });

        setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifyEnumSerializer(SerializationConfig config, JavaType valueType, BeanDescription beanDesc, JsonSerializer<?> serializer) {
                // Only use custom serializer if @JsonValue isn't specified
                if (beanDesc.findJsonValueAccessor() == null) {
                    return new StdSerializer<Enum>(Enum.class) {
                        @Override
                        public void serialize(Enum value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                            jgen.writeString(value.name().toLowerCase());
                        }
                    };
                }

                return super.modifyEnumSerializer(config, valueType, beanDesc, serializer);
            }
        });
    }
}
