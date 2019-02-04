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
package com.djrapitops.plan.db.sql.parsing;

import java.util.Arrays;

/**
 * @author Fuzzlemann
 */
public class Update extends WhereParser {

    public Update(String table) {
        super("UPDATE " + table + " SET");
        addSpace();
    }

    public static Update values(String table, Column... values) {
        String[] cols = Arrays.stream(values).map(Column::get).toArray(String[]::new);
        return values(table, cols);
    }
    public static Update values(String table, String... values) {
        Update parser = new Update(table);

        int size = values.length;
        for (int i = 0; i < size; i++) {
            if (size > 1 && i > 0) {
                parser.append(", ");
            }
            parser.append(values[i] + "=?");
        }

        return parser;
    }
}
