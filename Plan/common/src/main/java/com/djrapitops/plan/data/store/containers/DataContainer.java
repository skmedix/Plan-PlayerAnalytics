/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.data.store.containers;

import com.djrapitops.plan.data.store.Key;
import com.djrapitops.plan.utilities.formatting.Formatter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Interface for an object that can store arbitrary data referenced via {@link Key} objects.
 * <p>
 * Implementations should mainly be concerned on how the data given to it is stored.
 * Retrieval has some details that should be followed.
 *
 * @author Rsl1122
 */
public interface DataContainer {

    /**
     * Place your data inside the container.
     * <p>
     * What the container does with the object depends on the implementation.
     *
     * @param key Key of type T that identifies the data and will be used later when the data needs to be fetched.
     * @param obj object to store
     * @param <T> Type of the object
     */
    <T> void putRawData(Key<T> key, T obj);

    /**
     * Place a data supplier inside the container.
     * <p>
     * What the container does with the supplier depends on the implementation.
     *
     * @param key      Key of type T that identifies the data and will be used later when the data needs to be fetched.
     * @param supplier Supplier to store
     * @param <T>      Type of the object
     */
    <T> void putSupplier(Key<T> key, Supplier<T> supplier);

    /**
     * Place a caching data supplier inside the container.
     * <p>
     * If the supplier is called the value is cached according to the implementation of the container.
     * What the container does with the supplier depends on the implementation.
     *
     * @param key      Key of type T that identifies the data and will be used later when the data needs to be fetched.
     * @param supplier Supplier to store
     * @param <T>      Type of the object
     */
    <T> void putCachingSupplier(Key<T> key, Supplier<T> supplier);

    /**
     * Check if a Value with the given Key has been placed into the container.
     *
     * @param key Key that identifies the data.
     * @param <T> Type of the object returned by the Value if it is present.
     * @return true if found, false if not.
     */
    <T> boolean supports(Key<T> key);

    /**
     * Get an Optional of the Value identified by the Key.
     * <p>
     * It is recommended to check if the Optional is present as null values will be empty.
     *
     * @param key Key that identifies the Value
     * @param <T> Type of the object returned by Value
     * @return Optional of the object if the key is registered and key matches the type of the object. Otherwise empty.
     */
    <T> Optional<T> getValue(Key<T> key);

    /**
     * Get data identified by the Key, or throw an exception.
     * <p>
     * It is recommended to use {@link DataContainer#supports(Key)} before using this method.
     *
     * @param key Key that identifies the Value
     * @param <T> Type of the object returned by Value
     * @return the value
     * @throws IllegalArgumentException If the key is not supported.
     */
    <T> T getUnsafe(Key<T> key);

    /**
     * Get formatted Value identified by the Key.
     * <p>
     *
     * @param key       Key that identifies the Value
     * @param formatter Formatter for the Optional returned by {@link DataContainer#getValue(Key)}
     * @param <T>       Type of the object returned by Value
     * @return Optional of the object if the key is registered and key matches the type of the object. Otherwise empty.
     */
    default <T> String getFormatted(Key<T> key, Formatter<Optional<T>> formatter) {
        Optional<T> value = getValue(key);
        return formatter.apply(value);
    }

    /**
     * Get formatted Value identified by the Key, or throw an exception.
     * <p>
     * It is recommended to use {@link DataContainer#supports(Key)} before using this method.
     *
     * @param key       Key that identifies the Value
     * @param formatter Formatter for the value
     * @param <T>       Type of the object returned by Value
     * @return the value
     * @throws IllegalArgumentException If the key is not supported.
     */
    default <T> String getFormattedUnsafe(Key<T> key, Formatter<T> formatter) {
        T value = getUnsafe(key);
        return formatter.apply(value);
    }

    /**
     * Place all values from given DataContainer into this container.
     *
     * @param dataContainer Container with values.
     */
    void putAll(DataContainer dataContainer);

    /**
     * Clear the container of all data.
     */
    void clear();

    /**
     * Return a Key - Value Map of the data in the container.
     * <p>
     * This method may call blocking methods if underlying implementation uses the given Suppliers.
     *
     * @return Map: Key - Object
     */
    Map<Key, Object> getMap();
}
