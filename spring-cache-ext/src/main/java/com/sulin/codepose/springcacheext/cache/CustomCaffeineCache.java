package com.sulin.codepose.springcacheext.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.sulin.codepose.kit.json.Gsons;
import com.sulin.codepose.springcacheext.enums.ValueSerializer;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.lang.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;

public class CustomCaffeineCache extends CaffeineCache {

    private final ValueSerializer valueSerializer;

    private final Type type;


    public CustomCaffeineCache(String name, Cache<Object, Object> cache, boolean allowNullValues, ValueSerializer valueSerializer, Type type) {
        super(name, cache, allowNullValues);
        this.valueSerializer = valueSerializer;
        this.type = type;
    }

    protected Object toStoreValue(@Nullable Object userValue) {
        if (Objects.nonNull(userValue) && ValueSerializer.JSON.equals(valueSerializer)) {
            userValue = Gsons.GSON.toJson(userValue);
        }
        return super.toStoreValue(userValue);
    }

    @Nullable
    protected Object fromStoreValue(@Nullable Object storeValue) {
        if (Objects.isNull(storeValue)) return null;
        if (ValueSerializer.JSON.equals(valueSerializer) && storeValue instanceof String) {
            storeValue = Gsons.GSON.fromJson((String) storeValue, type);
        }
        return super.fromStoreValue(storeValue);
    }
}
