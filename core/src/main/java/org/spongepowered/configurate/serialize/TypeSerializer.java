/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spongepowered.configurate.serialize;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.util.CheckedFunction;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Represents an object which can serialize and deserialize objects of a
 * given type.
 *
 * @param <T> the type
 * @since 4.0.0
 */
public interface TypeSerializer<T> {

    /**
     * Given the provided functions, create a new serializer for a scalar value.
     *
     * <p>The returned serializer must fulfill all the requirements of a {@link ScalarSerializer}
     *
     * @param type the type of value returned by the serializer
     * @param serializer the serialization function, implementing {@link ScalarSerializer#serialize(Object, Predicate)}
     * @param deserializer the deserialization function, implementing {@link ScalarSerializer#deserialize(Type, Object)}
     * @param <T> the type of value to deserialize
     * @return a new and unregistered type serializer
     * @since 4.0.0
     */
    static <T> ScalarSerializer<T> of(Type type, BiFunction<T, Predicate<Class<?>>, Object> serializer,
                                      CheckedFunction<Object, T, SerializationException> deserializer) {
        return new FunctionScalarSerializer<>(type, deserializer, serializer);
    }

    /**
     * Given the provided functions, create a new serializer for a scalar value.
     *
     * <p>The returned serializer must fulfill all the requirements of
     * a {@link ScalarSerializer}
     *
     * @param type the type of value. Must not be a parameterized type
     * @param serializer the serialization function, implementing {@link ScalarSerializer#serialize(Object, Predicate)}
     * @param deserializer the deserialization function, implementing {@link ScalarSerializer#deserialize(Type, Object)}
     * @param <T> the type of value to deserialize
     * @return a new and unregistered type serializer
     * @see #of(Type, BiFunction, CheckedFunction) for the version of this
     *      function that takes a parameterized type
     * @since 4.0.0
     */
    static <T> ScalarSerializer<T> of(Class<T> type,
            BiFunction<T, Predicate<Class<?>>, Object> serializer, CheckedFunction<Object, T, SerializationException> deserializer) {
        if (type.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Parameterized types must be specified using TypeTokens, not raw classes");
        }

        return new FunctionScalarSerializer<T>(type, deserializer, serializer);
    }

    /**
     * Deserialize an object (of the correct type) from the given configuration
     * node.
     *
     * @param type the type of return value required
     * @param node the node containing serialized data
     * @return an object
     * @throws SerializationException if the presented data is invalid
     * @since 4.0.0
     */
    T deserialize(Type type, ConfigurationNode node) throws SerializationException;

    /**
     * Serialize an object to the given configuration node.
     *
     * @param type the type of the input object
     * @param obj the object to be serialized
     * @param node the node to write to
     * @throws SerializationException if the object cannot be serialized
     * @since 4.0.0
     */
    void serialize(Type type, @Nullable T obj, ConfigurationNode node) throws SerializationException;

    /**
     * Create an empty value of the appropriate type.
     *
     * <p>This method is for the most part designed to create empty collection
     * types, though it may be useful for scalars in limited cases.</p>
     *
     * @param specificType specific subtype to create an empty value of
     * @param options options used from the loading node
     * @return new empty value
     * @since 4.0.0
     */
    default @Nullable T emptyValue(final Type specificType, ConfigurationOptions options) {
        return null;
    }

}
