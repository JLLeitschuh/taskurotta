package ru.taskurotta.hazelcast.queue.delay;

import com.hazelcast.core.HazelcastInstance;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.taskurotta.hazelcast.queue.CachedQueue;
import ru.taskurotta.hazelcast.queue.delay.impl.StorageItemContainer;
import ru.taskurotta.hazelcast.queue.delay.impl.mongodb.StorageItemContainerBSerializer;
import ru.taskurotta.hazelcast.util.ClusterUtils;
import ru.taskurotta.mongodb.driver.BDecoderFactory;
import ru.taskurotta.mongodb.driver.BEncoderFactory;
import ru.taskurotta.mongodb.driver.BSerializationService;
import ru.taskurotta.mongodb.driver.BSerializationServiceFactory;
import ru.taskurotta.mongodb.driver.DBObjectCheat;
import ru.taskurotta.mongodb.driver.StreamBSerializer;
import ru.taskurotta.util.Shutdown;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: stukushin
 * Date: 10.12.13
 * Time: 16:58
 */
public class MongoStorageFactory implements StorageFactory {

    private static final Logger logger = LoggerFactory.getLogger(MongoStorageFactory.class);

    public static final String ENQUEUE_TIME_NAME = StorageItemContainerBSerializer.ENQUEUE_TIME.toString();
    public static final AtomicInteger bakedTasks = new AtomicInteger();

    private DB mongoDB;
    private String storagePrefix;

    private transient final ReentrantLock lock = new ReentrantLock();
    private ConcurrentHashMap<String, String> dbCollectionNamesMap = new ConcurrentHashMap<>();

    private int batchLoadSize;

    private BEncoderFactory encoderFactory;
    private BDecoderFactory decoderFactory;

    public MongoStorageFactory(final HazelcastInstance hazelcastInstance, final DB mongoDB,
                               String storagePrefix, long scheduleDelayMillis, int batchLoadSize,
                               BSerializationService bSerializationService, String objectClassName) {
        this.mongoDB = mongoDB;
        this.storagePrefix = storagePrefix;
        this.batchLoadSize = batchLoadSize;

        if (objectClassName != null) {
            StreamBSerializer objectStreamBSerializer = bSerializationService.getSerializer(objectClassName);
            StorageItemContainerBSerializer containerBSerializer = new StorageItemContainerBSerializer
                    (objectStreamBSerializer);

            BSerializationService mainBSerializationService = BSerializationServiceFactory.newInstance
                    (bSerializationService, containerBSerializer);

            decoderFactory = new BDecoderFactory(containerBSerializer);
            encoderFactory = new BEncoderFactory(mainBSerializationService);
        } else {
            logger.warn("Cass name of delayed item not found. Mongo delay queue stuff will work in legacy mode...");
        }

        fireStorageScanTask(hazelcastInstance, scheduleDelayMillis);

    }

    private void fireStorageScanTask(final HazelcastInstance hazelcastInstance, final long scheduleDelayMillis) {

        Thread delayQThread = new Thread(null, new Runnable() {
            @Override
            public void run() {

                while (!Shutdown.isTrue() && !Thread.currentThread().isInterrupted()) {

                    try {
                        Set<Map.Entry<String, String>> entrySet = dbCollectionNamesMap.entrySet();

                        if (logger.isDebugEnabled()) {
                            logger.debug("MongoDB storage scan iteration start: registered stores are [{}]", getRegisteredQueues(entrySet));
                        }

                        BasicDBObject query = new BasicDBObject(ENQUEUE_TIME_NAME, new BasicDBObject("$lte", System.currentTimeMillis()));

                        for (Map.Entry<String, String> entry : entrySet) {
                            String dbCollectionName = entry.getValue();
                            String queueName = entry.getKey();

                            CachedQueue<Object> cachedQueue = hazelcastInstance.getDistributedObject(CachedQueue.class
                                    .getName(), queueName);

                            if (ClusterUtils.isLocalCachedQueue(hazelcastInstance, cachedQueue)) {//Node should serve only

                                // local queues
                                DBCollection dbCollection = mongoDB.getCollection(dbCollectionName);
                                if (encoderFactory != null) {
                                    dbCollection.setDBEncoderFactory(encoderFactory);
                                    dbCollection.setDBDecoderFactory(decoderFactory);
                                }

                                try (DBCursor dbCursor = dbCollection.find(query).batchSize(batchLoadSize)) {
                                    while (dbCursor.hasNext()) {

                                        StorageItemContainer storageItemContainer = null;
                                        DBObject dbObject = dbCursor.next();

                                        storageItemContainer = (StorageItemContainer) ((DBObjectCheat) dbObject).getObject();

                                        if (cachedQueue.offer(storageItemContainer.getObject())) {
                                            bakedTasks.incrementAndGet();
                                            dbCollection.remove(new DBObjectCheat<UUID>(storageItemContainer.getId()));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        if (Shutdown.isTrue()) {
                            return;
                        }

                        logger.error("MongoDB storage scan iteration failed. Try to resume in [" + scheduleDelayMillis + "]ms...", e);
                        // ToDo: repair index on dbCollection
                    }

                        try {
                            TimeUnit.MILLISECONDS.sleep(scheduleDelayMillis);
                        } catch (InterruptedException ignored) {
                        }
                }

            }
        }, "delayQueue-" + storagePrefix);

        delayQThread.setDaemon(true);
        delayQThread.start();
    }


    private String getRegisteredQueues(Set<Map.Entry<String, String>> entrySet) {
        StringBuilder sb = new StringBuilder();
        String prefix = "Size: 0";
        if (entrySet != null && !entrySet.isEmpty()) {
            prefix = "Size: " + String.valueOf(entrySet.size());
            for (Map.Entry<String, String> item : entrySet) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(item.getKey());
            }
        }
        return prefix + " " + sb.toString();
    }

    @Override
    public Storage createStorage(final String queueName) {

        String dbCollectionName = dbCollectionNamesMap.get(queueName);

        DBCollection dbCollection = null;

        if (dbCollectionName == null) {

            final ReentrantLock lock = this.lock;
            lock.lock();

            try {
                dbCollectionName = dbCollectionNamesMap.get(queueName);
                if (dbCollectionName == null) {
                    dbCollectionName = storagePrefix + queueName;
                }
                dbCollectionNamesMap.put(queueName, dbCollectionName);

                dbCollection = mongoDB.getCollection(dbCollectionName);

                dbCollection.ensureIndex(new BasicDBObject(ENQUEUE_TIME_NAME, 1));
            } finally {
                lock.unlock();
            }
        } else {
            dbCollection = mongoDB.getCollection(dbCollectionName);
        }

        if (encoderFactory != null) {
            dbCollection.setDBEncoderFactory(encoderFactory);
            dbCollection.setDBDecoderFactory(decoderFactory);
        }

        final String finalDbCollectionName = dbCollectionName;
        final DBCollection finalDbCollection = dbCollection;

        return new Storage() {
            @Override
            public boolean add(Object o, long delayTime, TimeUnit unit) {
                return save(o, delayTime, unit, finalDbCollection);
            }

            @Override
            public boolean remove(Object o) {
                return delete(o, finalDbCollection);
            }

            @Override
            public void clear() {
                finalDbCollection.drop();
            }

            @Override
            public void destroy() {
                String unPrefixed = removePrefix(finalDbCollectionName);
                dbCollectionNamesMap.remove(queueName);

                mongoDB.getCollection(finalDbCollectionName).drop();
                mongoDB.getCollection(unPrefixed).drop();

                logger.debug("Destroying storage collections: delayedStore[{}] and queueBackingStore[{}]", finalDbCollectionName, unPrefixed);
            }

            @Override
            public long size() {
                return finalDbCollection.count();
            }
        };
    }

    private String removePrefix(String target) {
        String result = target;
        if (target != null && storagePrefix != null && target.startsWith(storagePrefix)) {
            result = target.substring(storagePrefix.length(), target.length());
        }
        return result;
    }

    private boolean save(Object o, long delayTime, TimeUnit unit, DBCollection dbCollection) {
        long enqueueTime = System.currentTimeMillis() + unit.toMillis(delayTime);

        UUID id = UUID.randomUUID();

        final StorageItemContainer storageItemContainer = new StorageItemContainer(id, o, enqueueTime,
                null);

        DBObjectCheat dbObject = new DBObjectCheat<StorageItemContainer>(storageItemContainer);
        dbCollection.insert(dbObject);

        return true;
    }

    private boolean delete(Object o, DBCollection dbCollection) {
        // todo: we can remove only by secondary index because we don't knows actual id of document
        throw new IllegalStateException("Not implementer yet");
    }
}
