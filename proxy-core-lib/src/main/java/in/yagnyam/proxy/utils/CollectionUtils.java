package in.yagnyam.proxy.utils;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CollectionUtils {

    public static <T> List<T> concat(List<T> list, T element) {
        return ImmutableList.<T>builder()
                .addAll(list)
                .add(element)
                .build();
    }

    public static <T> List<T> concat(T element, List<T> list) {
        return ImmutableList.<T>builder()
                .add(element)
                .addAll(list)
                .build();
    }
}
