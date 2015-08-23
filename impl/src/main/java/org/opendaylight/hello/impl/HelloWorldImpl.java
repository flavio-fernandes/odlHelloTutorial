/*
 * Copyright(c) Yoyodyne, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.hello.impl;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class HelloWorldImpl implements HelloService, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldImpl.class);
    private BindingTransactionChain tc;

    public HelloWorldImpl(DataBroker db) {
        this.tc = db.createTransactionChain(this);
        initializeDataTree();
    }

    @Override
    public Future<RpcResult<HelloWorldOutput>> helloWorld(HelloWorldInput input) {
        return new HelloWorldFuture(tc, input);
    }

    private void initializeDataTree() {
        LOG.info("Preparing to initialize the greeting registry");
        WriteTransaction transaction = tc.newWriteOnlyTransaction();
        InstanceIdentifier<GreetingRegistry> iid = InstanceIdentifier.create(GreetingRegistry.class);
        GreetingRegistry greetingRegistry = new GreetingRegistryBuilder().build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, greetingRegistry);
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, greetingRegistry);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<>("Failed to create greeting registry", LOG));
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> transactionChain, AsyncTransaction<?, ?> asyncTransaction, Throwable throwable) {
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {
    }
}

