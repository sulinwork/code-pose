package com.sulin.code.pose.cache;


import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

public abstract class MultiCache<P, R> {


    private Class<R> outClass;

    public MultiCache() {
        ParameterizedType genericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        this.outClass = (Class<R>) genericSuperclass.getActualTypeArguments()[1];
    }

    public List<R> getMultiCache(List<P> params) {
                return null;
    }

    public Map<P, R> getMultiCacheMap(List<P> params) {
        return null;
    }


    public abstract R load(P param);

    public abstract String generateKey(P p);
}
