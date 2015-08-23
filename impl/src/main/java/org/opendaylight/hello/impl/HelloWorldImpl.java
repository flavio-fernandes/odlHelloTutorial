/*
 * Copyright(c) Yoyodyne, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.hello.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.greeting.registry.GreetingRegistryEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.greeting.registry.GreetingRegistryEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.greeting.registry.GreetingRegistryEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class HelloWorldImpl implements HelloService, TransactionChainListener {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldImpl.class);
    // private DataBroker db;
    private BindingTransactionChain tc;

    public HelloWorldImpl(DataBroker db) {
        // this.db = db;
        this.tc = db.createTransactionChain(this);
        initializeDataTree();
    }

    @Override
    public Future<RpcResult<HelloWorldOutput>> helloWorld(HelloWorldInput input) {
        HelloWorldOutput output = new HelloWorldOutputBuilder()
                .setGreeting(readFromGreetingRegistry(input))
                .build();
        writeToGreetingRegistry(input, output);
        return RpcResultBuilder.success(output).buildFuture();
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

    private void writeToGreetingRegistry(HelloWorldInput input, HelloWorldOutput output) {
        WriteTransaction transaction = tc.newWriteOnlyTransaction();
        InstanceIdentifier<GreetingRegistryEntry> iid = toInstanceIdentifier(input);
        GreetingRegistryEntry greeting = new GreetingRegistryEntryBuilder()
                .setGreeting(output.getGreeting())
                .setName(input.getName())
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, greeting);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>("Failed to write greeting to greeting registry", LOG));
    }

    private String readFromGreetingRegistry(HelloWorldInput input) {
        String result = "Hello " + input.getName();
        ReadOnlyTransaction transaction = tc.newReadOnlyTransaction();
        InstanceIdentifier<GreetingRegistryEntry> iid = toInstanceIdentifier(input);
        CheckedFuture<Optional<GreetingRegistryEntry>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
        Optional<GreetingRegistryEntry> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            LOG.warn("Reading greeting failed:",e);
        }
        if(optional.isPresent()) {
            result = optional.get().getGreeting();
        }
        return result;
    }

    private InstanceIdentifier<GreetingRegistryEntry> toInstanceIdentifier(HelloWorldInput input) {
        InstanceIdentifier<GreetingRegistryEntry> iid = InstanceIdentifier.create(GreetingRegistry.class)
                .child(GreetingRegistryEntry.class, new GreetingRegistryEntryKey(input.getName()));
        return iid;
    }

    @Override
    public void onTransactionChainFailed(TransactionChain<?, ?> transactionChain, AsyncTransaction<?, ?> asyncTransaction, Throwable throwable) {
        
    }

    @Override
    public void onTransactionChainSuccessful(TransactionChain<?, ?> transactionChain) {

    }
}

