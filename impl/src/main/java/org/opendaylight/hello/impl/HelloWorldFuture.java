package org.opendaylight.hello.impl;

import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.GreetingRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloWorldInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloWorldOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloWorldOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.greeting.registry.GreetingRegistryEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.greeting.registry.GreetingRegistryEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.greeting.registry.GreetingRegistryEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class HelloWorldFuture extends AbstractFuture<RpcResult<HelloWorldOutput>> implements FutureCallback<Optional<GreetingRegistryEntry>> {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldFuture.class);
    private HelloWorldInput input;
    private BindingTransactionChain tc;

    public HelloWorldFuture(BindingTransactionChain tc, HelloWorldInput input) {
        this.tc = tc;
        this.input = input;
        readFromGreetingRegistry(input);
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

    private InstanceIdentifier<GreetingRegistryEntry> toInstanceIdentifier(HelloWorldInput input) {
        InstanceIdentifier<GreetingRegistryEntry> iid = InstanceIdentifier.create(GreetingRegistry.class)
                .child(GreetingRegistryEntry.class, new GreetingRegistryEntryKey(input.getName()));
        return iid;
    }

    private void readFromGreetingRegistry(HelloWorldInput input) {
        ReadOnlyTransaction transaction = tc.newReadOnlyTransaction();
        InstanceIdentifier<GreetingRegistryEntry> iid = toInstanceIdentifier(input);
        CheckedFuture<Optional<GreetingRegistryEntry>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
        Futures.addCallback(future, this);
    }

    @Override
    public void onSuccess(Optional<GreetingRegistryEntry> result) {
        String greeting = "Hello " + this.input.getName();
        if(result.isPresent()) {
            greeting = result.get().getGreeting();
        }
        HelloWorldOutput output = new HelloWorldOutputBuilder()
                .setGreeting(greeting)
                .build();
        writeToGreetingRegistry(input, output);
        this.set(RpcResultBuilder.success(output).build());
    }

    @Override
    public void onFailure(Throwable t) {
        this.set(RpcResultBuilder.<HelloWorldOutput>failed().withError(ErrorType.RPC, "Failed", t).build());
    }
}
