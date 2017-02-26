package com.lenzhao.framework.util;

import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *LRU缓存类
 */
public class LRUMap<K, V> extends LinkedHashMap<K, V> {
	
    private static final long serialVersionUID = 1L;
    private final int maxCapacity;
    //加载因子
    private static final float  LOAD_FACTOR = 0.99f;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUMap(int maxCapacity) {
        super(maxCapacity, LOAD_FACTOR, false);
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
        return size() > maxCapacity;
    }

    @Override
    public V get(Object key) {
        try {
            lock.readLock().lock();
            return super.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        try {
            lock.writeLock().lock();
            return super.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(Object key) {
        try {
            lock.writeLock().lock();
            return super.remove(key);
        } finally {
            lock.writeLock().unlock();
        }

    }
}
