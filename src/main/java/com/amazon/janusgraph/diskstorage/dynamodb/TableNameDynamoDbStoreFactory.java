/*
 * Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.janusgraph.diskstorage.dynamodb;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.janusgraph.diskstorage.BackendException;
import org.janusgraph.diskstorage.PermanentBackendException;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates backend store based on table name.
 *
 * @author Matthew Sowders
 * @author Alexander Patrikalakis
 *
 */
@Slf4j
public class TableNameDynamoDbStoreFactory implements DynamoDbStoreFactory {
    private final ConcurrentMap<String, AwsStore> stores = new ConcurrentHashMap<>();

    @Override
    public AwsStore create(final DynamoDBStoreManager manager, final String prefix, final String name) throws BackendException {
        final Client client = manager.getClient();

        String actualName = client.getBaseNameToActualName().get(name);
        if (actualName == null) {
            actualName = name;
        }
        log.info("Entering TableNameDynamoDbStoreFactory.create prefix:{} actualName:{}, baseName:{}", prefix, actualName, name);
        // ensure there is only one instance used per table name.

        final BackendDataModel model = client.dataModel(actualName);

        if (model == null) {
            throw new PermanentBackendException(String.format("Store name %s unknown. Set up user log / lock store in properties", actualName));
        }

        final AwsStore storeBackend = model.createStoreBackend(manager, prefix, actualName);
        final AwsStore create = new MetricStore(storeBackend);
        final AwsStore previous = stores.putIfAbsent(actualName, create);
        if (null == previous) {
            try {
                final boolean tableIsExist = create.tableIsExist();
                if (!tableIsExist) {
                    log.error("prefix:{} actualName:{}, baseName:{}; Table have not created", prefix, actualName, name);
                    client.getDelegate().shutdown();
                    throw new BackendRuntimeException(String.format("prefix:%s actualName:%s, baseName:%s; Table have not created", prefix, actualName, name));
                }
            } catch (BackendException e) {
                log.error("error getting describeTable: actualName: {}", actualName);
                client.getDelegate().shutdown();
                throw e;
            }
//            try {
//                create.ensureStore();
//            } catch (BackendException e) {
//                client.getDelegate().shutdown();
//                throw e;
//            }
        }
        final AwsStore store = stores.get(actualName);
        log.info("Exiting TableNameDynamoDbStoreFactory.create prefix:{} name:{} returning:{}", prefix, actualName, store);
        return store;
    }

    @Override
    public Iterable<AwsStore> getAllStores() {
        return stores.values();
    }

    @Override
    public AwsStore getStore(final String store) {
        return stores.get(store);
    }

}
