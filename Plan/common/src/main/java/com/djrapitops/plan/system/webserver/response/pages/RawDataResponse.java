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
package com.djrapitops.plan.system.webserver.response.pages;

import com.djrapitops.plan.data.store.containers.DataContainer;
import com.djrapitops.plan.system.webserver.response.Response;
import com.djrapitops.plan.system.webserver.response.ResponseType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response for sending raw data as JSON when it is inside a DataContainer.
 * <p>
 * This transform class is required to remove Key-Supplier object pollution in the resulting JSON, as well as to remove
 * the effects of the caching layer.
 *
 * @author Rsl1122
 */
public class RawDataResponse extends Response {

    public RawDataResponse(DataContainer dataContainer) {
        super(ResponseType.JSON);

        Map<String, Object> values = mapToNormalMap(dataContainer);

        super.setHeader("HTTP/1.1 200 OK");

        try {
            Class<?> gsonClass = Class.forName("com.google.gson.Gson");
            Object gson = gsonClass.getConstructor().newInstance();
            Object json = gsonClass.getMethod("toJson", Object.class).invoke(gson, values);

            super.setContent(json.toString());
        } catch (ReflectiveOperationException e) {
            super.setContent("{\"error\":\"Gson for raw json responses not available on this server: " + e.toString() + "\"}");
        }
    }

    private Map<String, Object> mapToNormalMap(DataContainer player) {
        Map<String, Object> values = new HashMap<>();
        player.getMap().forEach((key, value) ->
                {
                    if (value instanceof DataContainer) {
                        value = mapToNormalMap((DataContainer) value);
                    }
                    if (value instanceof Map) {
                        value = handleMap((Map) value);
                    }
                    if (value instanceof List) {
                        value = handleList((List) value);
                    }
                    values.put(key.getKeyName(), value);
                }
        );
        return values;
    }

    private List handleList(List list) {
        if (list.stream().findAny().orElse(null) instanceof DataContainer) {
            return (List) list.stream().map((obj) -> mapToNormalMap((DataContainer) obj)).collect(Collectors.toList());
        }
        return list;
    }

    private Map handleMap(Map map) {
        if (map.values().stream().findAny().orElse(null) instanceof DataContainer) {
            Map<Object, Object> newMap = new HashMap<>();
            map.forEach((key, value) -> newMap.put(key, mapToNormalMap((DataContainer) value)));
            return newMap;
        }
        return map;
    }
}