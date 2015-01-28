/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.taskurotta.hazelcast.queue.impl;

import com.hazelcast.core.HazelcastException;
import com.hazelcast.nio.BufferObjectDataOutput;
import com.hazelcast.nio.ClassLoaderUtil;
import com.hazelcast.nio.IOUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.nio.serialization.SerializationService;
import com.hazelcast.util.EmptyStatement;
import com.hazelcast.util.QuickMath;
import ru.taskurotta.hazelcast.queue.config.CachedQueueStoreConfig;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStore;
import ru.taskurotta.hazelcast.queue.store.CachedQueueStoreFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.hazelcast.util.ValidationUtil.checkNotNull;

/**
 * Wrapper for the Queue Store.
 */
@SuppressWarnings("unchecked")
public final class QueueStoreWrapper implements CachedQueueStore<Data> {

    private static final int OUTPUT_SIZE = 1024;
    private static final int BUFFER_SIZE_FACTOR = 8;

    private boolean enabled;

    private boolean binary;

    private CachedQueueStore store;

    private SerializationService serializationService;

    private QueueStoreWrapper() {
    }

    /**
     * Factory method that creates a {@link QueueStoreWrapper}
     *
     * @param name                 queue name
     * @param storeConfig          store config of queue
     * @param serializationService serialization service.
     * @return returns a new instance of {@link QueueStoreWrapper}
     */
    public static QueueStoreWrapper create(String name, CachedQueueStoreConfig storeConfig, SerializationService
            serializationService) {
        checkNotNull(name, "name should not be null");
        checkNotNull(serializationService, "serializationService should not be null");

        final QueueStoreWrapper storeWrapper = new QueueStoreWrapper();
        storeWrapper.setSerializationService(serializationService);
        if (storeConfig == null || !storeConfig.isEnabled()) {
            return storeWrapper;
        }
        // create queue store.
        final ClassLoader classLoader = serializationService.getClassLoader();
        final CachedQueueStore queueStore = createQueueStore(name, storeConfig, classLoader);
        if (queueStore != null) {
            storeWrapper.setEnabled(storeConfig.isEnabled());
            storeWrapper.setBinary(storeConfig.isBinary());
            storeWrapper.setStore(queueStore);
        }
        return storeWrapper;
    }

    private static CachedQueueStore createQueueStore(String name, CachedQueueStoreConfig storeConfig, ClassLoader
            classLoader) {
        // 1. Try to create store from `store impl.` class.
        CachedQueueStore store = getQueueStore(storeConfig, classLoader);
        // 2. Try to create store from `store factory impl.` class.
        if (store == null) {
            store = getQueueStoreFactory(name, storeConfig, classLoader);
        }
        return store;
    }

    private static CachedQueueStore getQueueStore(CachedQueueStoreConfig storeConfig, ClassLoader classLoader) {
        if (storeConfig == null) {
            return null;
        }
        CachedQueueStore store = storeConfig.getStoreImplementation();
        if (store != null) {
            return store;
        }
        try {
            store = ClassLoaderUtil.newInstance(classLoader, storeConfig.getClassName());
        } catch (Exception ignored) {
            EmptyStatement.ignore(ignored);
        }
        return store;

    }

    private static CachedQueueStore getQueueStoreFactory(String name, CachedQueueStoreConfig storeConfig, ClassLoader
            classLoader) {
        if (storeConfig == null) {
            return null;
        }
        CachedQueueStoreFactory factory = storeConfig.getFactoryImplementation();
        if (factory == null) {
            try {
                factory = ClassLoaderUtil.newInstance(classLoader,
                        storeConfig.getFactoryClassName());
            } catch (Exception ignored) {
                EmptyStatement.ignore(ignored);
            }
        }
        return factory == null ? null : factory.newQueueStore(name, storeConfig);
    }

    @Override
    public void store(Long key, Data value) {
        if (!enabled) {
            return;
        }
        final Object actualValue;
        if (binary) {
            // WARNING: we can't pass original Data to the user
            int size = QuickMath.normalize(value.dataSize(), BUFFER_SIZE_FACTOR);
            BufferObjectDataOutput out = serializationService.createObjectDataOutput(size);
            try {
                out.writeData(value);
                actualValue = out.toByteArray();
            } catch (IOException e) {
                throw new HazelcastException(e);
            } finally {
                IOUtil.closeResource(out);
            }
        } else {
            actualValue = serializationService.toObject(value);
        }
        store.store(key, actualValue);
    }

    @Override
    public void storeAll(Map<Long, Data> map) {
        if (!enabled) {
            return;
        }

        final Map<Long, Object> objectMap = new HashMap<Long, Object>(map.size());
        if (binary) {
            // WARNING: we can't pass original Data to the user
            // TODO: @mm - is there really an advantage of using binary storeAll?
            // since we need to do array copy for each item.
            BufferObjectDataOutput out = serializationService.createObjectDataOutput(OUTPUT_SIZE);
            try {
                for (Map.Entry<Long, Data> entry : map.entrySet()) {
                    out.writeData(entry.getValue());
                    objectMap.put(entry.getKey(), out.toByteArray());
                    out.clear();
                }
            } catch (IOException e) {
                throw new HazelcastException(e);
            } finally {
                IOUtil.closeResource(out);
            }
        } else {
            for (Map.Entry<Long, Data> entry : map.entrySet()) {
                objectMap.put(entry.getKey(), serializationService.toObject(entry.getValue()));
            }
        }
        store.storeAll(objectMap);
    }

    @Override
    public void delete(Long key) {
        if (enabled) {
            store.delete(key);
        }
    }

    @Override
    public void deleteAll(Collection<Long> keys) {
        if (enabled) {
            store.deleteAll(keys);
        }
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public Data load(Long key) {
        if (!enabled) {
            return null;
        }

        final Object val = store.load(key);
        if (binary) {
            byte[] dataBuffer = (byte[]) val;
            ObjectDataInput in = serializationService.createObjectDataInput(dataBuffer);
            Data data;
            try {
                data = in.readData();
            } catch (IOException e) {
                throw new HazelcastException(e);
            }
            return data;
        }
        return serializationService.toData(val);
    }

    @Override
    public Map<Long, Data> loadAll(Collection<Long> keys) {
        if (!enabled) {
            return null;
        }

        final Map<Long, ?> map = store.loadAll(keys);

        return serializeToData(map);
    }

    @Override
    public Map<Long, Data> loadAll(long from, long to) {

        if (!enabled) {
            return null;
        }

        final Map<Long, ?> map = store.loadAll(from, to);

        return serializeToData(map);
    }

    private Map<Long, Data> serializeToData(Map<Long, ?> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        final Map<Long, Data> dataMap = new HashMap<Long, Data>(map.size());
        if (binary) {
            for (Map.Entry<Long, ?> entry : map.entrySet()) {
                byte[] dataBuffer = (byte[]) entry.getValue();
                ObjectDataInput in = serializationService.createObjectDataInput(dataBuffer);
                Data data;
                try {
                    data = in.readData();
                } catch (IOException e) {
                    throw new HazelcastException(e);
                }
                dataMap.put(entry.getKey(), data);
            }
        } else {
            for (Map.Entry<Long, ?> entry : map.entrySet()) {
                dataMap.put(entry.getKey(), serializationService.toData(entry.getValue()));
            }
        }
        return dataMap;
    }

    @Override
    public Set<Long> loadAllKeys() {
        if (enabled) {
            return store.loadAllKeys();
        }
        return null;
    }

    @Override
    public long getMinItemId() {
        return store.getMinItemId();
    }

    @Override
    public long getMaxItemId() {
        return store.getMaxItemId();
    }

    private static int parseInt(String name, int defaultValue, CachedQueueStoreConfig storeConfig) {
        final String val = storeConfig.getProperty(name);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBinary() {
        return binary;
    }

    void setSerializationService(SerializationService serializationService) {
        this.serializationService = serializationService;
    }

    void setStore(CachedQueueStore store) {
        this.store = store;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setBinary(boolean binary) {
        this.binary = binary;
    }
}
