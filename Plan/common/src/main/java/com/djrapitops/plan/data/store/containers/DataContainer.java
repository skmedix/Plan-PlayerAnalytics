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

import com.djrapitops.plan.data.store.CachingSupplier;
import com.djrapitops.plan.data.store.Key;
import com.djrapitops.plan.utilities.formatting.Formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Abstract representation of an object that holds the Values for different Keys.
 * <p>
 * The methods in this object are used for placing and fetching the data from the container.
 * Methods to use depend on your use case.
 *
 * @author Rsl1122
 */
public class DataContainer {

    private final Map<Key, Supplier> map;
    private long timeToLive;

    public DataContainer() {
        this(TimeUnit.SECONDS.toMillis(30L));
    }

    public DataContainer(long timeToLive) {
        this.timeToLive = timeToLive;
        map = new HashMap<>();
    }

    /**
     * Place your data inside the container.
     *
     * @param key Key of type T that identifies the data and will be used later when the data needs to be fetched.
     * @param obj object to store
     * @param <T> Type of the object
     */
    public <T> void putRawData(Key<T> key, T obj) {
        putSupplier(key, () -> obj);
    }

    public <T> void putSupplier(Key<T> key, Supplier<T> supplier) {
        if (supplier == null) {
            return;
        }
        map.put(key, supplier);
    }

    public <T> void putCachingSupplier(Key<T> key, Supplier<T> supplier) {
        if (supplier == null) {
            return;
        }
        map.put(key, new CachingSupplier<>(supplier, timeToLive));
    }

    private <T> Supplier<T> getSupplier(Key<T> key) {
        return (Supplier<T>) map.get(key);
    }

    /**
     * Check if a Value with the given Key has been placed into the container.
     *
     * @param key Key that identifies the data.
     * @param <T> Type of the object returned by the Value if it is present.
     * @return true if found, false if not.
     */
    public <T> boolean supports(Key<T> key) {
        return map.containsKey(key);
    }

    /**
     * Get an Optional of the data identified by the Key.
     * <p>
     * Since Value is a functional interface, its method may call blocking methods via Value implementations,
     * It is therefore recommended to not call this method on the server thread.
     * <p>
     * It is recommended to check if the Optional is present as null values returned by plugins will be empty.
     *
     * @param key Key that identifies the Value
     * @param <T> Type of the object returned by Value
     * @return Optional of the object if the key is registered and key matches the type of the object. Otherwise empty.
     */
    public <T> Optional<T> getValue(Key<T> key) {
        Supplier<T> supplier = getSupplier(key);
        if (supplier == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(supplier.get());
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    public <T> T getUnsafe(Key<T> key) {
        Supplier supplier = map.get(key);
        if (supplier == null) {
            throw new IllegalArgumentException("Unsupported Key: " + key.getKeyName());
        }
        return (T) supplier.get();
    }

    public <T> String getFormatted(Key<T> key, Formatter<Optional<T>> formatter) {
        Optional<T> value = getValue(key);
        return formatter.apply(value);
    }

    public <T> String getFormattedUnsafe(Key<T> key, Formatter<T> formatter) {
        T value = getUnsafe(key);
        return formatter.apply(value);
    }

    public void putAll(Map<Key, Supplier> toPut) {
        map.putAll(toPut);
    }

    public void putAll(DataContainer dataContainer) {
        putAll(dataContainer.map);
    }

    public void clear() {
        map.clear();
    }

    public Map<Key, Object> getMap() {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }
}