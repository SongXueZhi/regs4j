package utils;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {
    /* Converts from List to List<T> to solve conversion issues */
    public static <T> List<T> castList(Class<? extends T> c, List<?> collection) {
        // adapted from https://stackoverflow.com/a/2848268
        List<T> r = new ArrayList<>(collection.size());
        for (Object obj: collection) {
            r.add(c.cast(obj));
        }
        return r;
    }
}
