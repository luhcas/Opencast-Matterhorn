/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *  
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Place for tool operations which cannot find any other suitable home.
 *
 * @author Christoph E. Driessen <ced@neopoly.de>
 */
public class Tool {

    private Tool() {
    }

    /**
     * Use this method if you want to get rid of the annoying "Unchecked cast" warnings.
     *  
     * @param o the object to cast
     * @return the cast object
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o) {
        return (T) o;
    }

    /** Returns the first non null argument. */
    public static <T> T use(T... o) {
        for (T b : o)
            if (b != null)
                return b;
        return null;
    }

    /**
     * Checks if <var>o</var> is empty. Supports simple objects, strings, {@link java.util.Map}s,
     * {@link java.util.Collection}s, arrays
     * {@link java.util.Iterator}s and {@link Iterable}s.
     * <p/>
     * Arrays are considered empty if they have zero length or if all elements are null.
     * <p/>
     * Collections are considered empty if {@link java.util.Collection#isEmpty()} returns true.
     * <p/>
     * Iterators and Iterables are considered empty if they cannot provide a next element
     * ({@link java.util.Iterator#hasNext()}). So be careful if you pass in an already used Iterator!
     */
    public static boolean empty(Object o) {
        if (o == null)
            return true;
        if (o instanceof String)
            return ((String) o).length() == 0;
        if (o instanceof Collection)
            return ((Collection) o).isEmpty();
        if (o instanceof Map)
            return ((Map) o).isEmpty();
        if (o.getClass().isArray()) {
            int len = Array.getLength(o);
            if (len == 0)
                return true;
            for (int i = 0; i < len; i++)
                if (Array.get(o, i) != null)
                    return false;
            return true;
        }
        if (o instanceof Iterable)
            return !((Iterable) o).iterator().hasNext();
        if (o instanceof Iterator)
            return !((Iterator) o).hasNext();
        return false;
    }
}
