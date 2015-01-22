/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.integration;

import org.apache.ignite.*;
import org.apache.ignite.cache.*;
import org.apache.ignite.transactions.*;
import org.gridgain.grid.util.typedef.*;

import java.util.*;

import static org.apache.ignite.transactions.IgniteTxConcurrency.*;
import static org.apache.ignite.transactions.IgniteTxIsolation.*;
import static org.apache.ignite.cache.GridCacheAtomicityMode.*;
import static org.apache.ignite.cache.GridCacheDistributionMode.*;
import static org.apache.ignite.cache.GridCacheMode.*;

/**
 *
 */
public class IgniteCacheTxStoreSessionTest extends IgniteCacheStoreSessionAbstractTest {
    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 3;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheMode cacheMode() {
        return PARTITIONED;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheAtomicityMode atomicityMode() {
        return TRANSACTIONAL;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return PARTITIONED_ONLY;
    }

    /**
     * @throws Exception If failed.
     */
    public void testStoreSessionTx() throws Exception {
        testTxPut(jcache(0), null, null);

        testTxPut(ignite(0).jcache(CACHE_NAME1), null, null);

        testTxRemove(null, null);

        testTxPutRemove(null, null);

        for (IgniteTxConcurrency concurrency : F.asList(PESSIMISTIC)) {
            for (IgniteTxIsolation isolation : F.asList(REPEATABLE_READ)) {
                testTxPut(jcache(0), concurrency, isolation);

                testTxRemove(concurrency, isolation);

                testTxPutRemove(concurrency, isolation);
            }
        }
    }

    /**
     * @param concurrency Concurrency mode.
     * @param isolation Isolation mode.
     * @throws Exception If failed.
     */
    private void testTxPutRemove(IgniteTxConcurrency concurrency, IgniteTxIsolation isolation) throws Exception {
        log.info("Test tx put/remove [concurrency=" + concurrency + ", isolation=" + isolation + ']');

        IgniteCache<Integer, Integer> cache = jcache(0);

        List<Integer> keys = testKeys(cache, 3);

        Integer key1 = keys.get(0);
        Integer key2 = keys.get(1);
        Integer key3 = keys.get(2);

        try (IgniteTx tx = startTx(concurrency, isolation)) {
            log.info("Do tx put1.");

            cache.put(key1, key1);

            log.info("Do tx put2.");

            cache.put(key2, key2);

            log.info("Do tx remove.");

            cache.remove(key3);

            expData.add(new ExpectedData(true, "writeAll", new HashMap<>(), null));
            expData.add(new ExpectedData(true, "delete", F.<Object, Object>asMap(0, "writeAll"), null));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "writeAll", 1, "delete"), null));

            log.info("Do tx commit.");

            tx.commit();
        }

        assertEquals(0, expData.size());
    }

    /**
     * @param cache Cache.
     * @param concurrency Concurrency mode.
     * @param isolation Isolation mode.
     * @throws Exception If failed.
     */
    private void testTxPut(IgniteCache<Object, Object> cache,
        IgniteTxConcurrency concurrency,
        IgniteTxIsolation isolation) throws Exception {
        log.info("Test tx put [concurrency=" + concurrency + ", isolation=" + isolation + ']');

        List<Integer> keys = testKeys(cache, 3);

        Integer key1 = keys.get(0);

        try (IgniteTx tx = startTx(concurrency, isolation)) {
            log.info("Do tx get.");

            cache.get(key1);

            log.info("Do tx put.");

            cache.put(key1, key1);

            expData.add(new ExpectedData(true, "write", new HashMap<>(), cache.getName()));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "write"), cache.getName()));

            log.info("Do tx commit.");

            tx.commit();
        }

        assertEquals(0, expData.size());

        Integer key2 = keys.get(1);
        Integer key3 = keys.get(2);

        try (IgniteTx tx = startTx(concurrency, isolation)) {
            log.info("Do tx put1.");

            cache.put(key2, key2);

            log.info("Do tx put2.");

            cache.put(key3, key3);

            expData.add(new ExpectedData(true, "writeAll", new HashMap<>(), cache.getName()));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "writeAll"), cache.getName()));

            log.info("Do tx commit.");

            tx.commit();
        }

        assertEquals(0, expData.size());
    }

    /**
     * @param concurrency Concurrency mode.
     * @param isolation Isolation mode.
     * @throws Exception If failed.
     */
    private void testTxRemove(IgniteTxConcurrency concurrency, IgniteTxIsolation isolation) throws Exception {
        log.info("Test tx remove [concurrency=" + concurrency + ", isolation=" + isolation + ']');

        IgniteCache<Integer, Integer> cache = jcache(0);

        List<Integer> keys = testKeys(cache, 3);

        Integer key1 = keys.get(0);

        try (IgniteTx tx = startTx(concurrency, isolation)) {
            log.info("Do tx get.");

            cache.get(key1);

            log.info("Do tx remove.");

            cache.remove(key1, key1);

            expData.add(new ExpectedData(true, "delete", new HashMap<>(), null));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "delete"), null));

            log.info("Do tx commit.");

            tx.commit();
        }

        assertEquals(0, expData.size());

        Integer key2 = keys.get(1);
        Integer key3 = keys.get(2);

        try (IgniteTx tx = startTx(concurrency, isolation)) {
            log.info("Do tx remove1.");

            cache.remove(key2, key2);

            log.info("Do tx remove2.");

            cache.remove(key3, key3);

            expData.add(new ExpectedData(true, "deleteAll", new HashMap<>(), null));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "deleteAll"), null));

            log.info("Do tx commit.");

            tx.commit();
        }

        assertEquals(0, expData.size());
    }

    /**
     * @param concurrency Concurrency mode.
     * @param isolation Isolation mode.
     * @return Transaction.
     */
    private IgniteTx startTx(IgniteTxConcurrency concurrency, IgniteTxIsolation isolation) {
        IgniteTransactions txs = ignite(0).transactions();

        if (concurrency == null)
            return txs.txStart();

        return txs.txStart(concurrency, isolation);
    }

    /**
     * @throws Exception If failed.
     */
    // TODO IGNITE-109: fix test when fixed.
    public void testSessionCrossCacheTx() throws Exception {
        IgniteCache<Object, Object> cache0 = ignite(0).jcache(null);

        IgniteCache<Object, Object> cache1 = ignite(0).jcache(CACHE_NAME1);

        Integer key1 = primaryKey(cache0);
        Integer key2 = primaryKeys(cache1, 1, key1 + 1).get(0);

        try (IgniteTx tx = startTx(null, null)) {
            cache0.put(key1, 1);

            cache1.put(key2, 0);

            expData.add(new ExpectedData(true, "writeAll", new HashMap<>(), null));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "writeAll"), null));

            tx.commit();
        }

        assertEquals(0, expData.size());

        try (IgniteTx tx = startTx(null, null)) {
            cache1.put(key1, 1);

            cache0.put(key2, 0);

            expData.add(new ExpectedData(true, "writeAll", new HashMap<>(), null));
            expData.add(new ExpectedData(true, "txEnd", F.<Object, Object>asMap(0, "writeAll"), null));

            tx.commit();
        }

        assertEquals(0, expData.size());
    }
}
