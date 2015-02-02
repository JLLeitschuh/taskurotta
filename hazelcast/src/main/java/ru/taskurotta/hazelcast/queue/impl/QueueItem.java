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

import com.hazelcast.nio.serialization.Data;
import ru.taskurotta.hazelcast.queue.impl.stats.StatsUtil;

/**
 * Queue Item.
 */
public class QueueItem {


    protected Data data;

    // calculated value for statistics
    protected long headCost;

    public QueueItem(Data data) {
        this.data = data;

        final int numberOfLongs = 1;

        headCost = StatsUtil.OBJ_REF_IN_BYTES +                   // add key references
                StatsUtil.LONG_SIZE_IN_BYTES +                    // add key cost
                StatsUtil.OBJ_REF_IN_BYTES +                      // add value references
                numberOfLongs * StatsUtil.LONG_SIZE_IN_BYTES +    // + qItem.heapCost
                data.getHeapCost();                                     // + qItem.data
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueueItem)) {
            return false;
        }

        QueueItem item = (QueueItem) o;

        if (data != null ? !data.equals(item.data) : item.data != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
