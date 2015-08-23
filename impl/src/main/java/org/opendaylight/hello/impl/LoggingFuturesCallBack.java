package org.opendaylight.hello.impl;

import com.google.common.util.concurrent.FutureCallback;
import org.slf4j.Logger;

public class LoggingFuturesCallBack<V> implements FutureCallback<V> {

    private Logger LOG;
    private String message;

    public LoggingFuturesCallBack(String message, Logger LOG) {
        this.message = message;
        this.LOG = LOG;
    }

    @Override
    public void onFailure(Throwable e) {
        LOG.warn(message,e);

    }

    @Override
    public void onSuccess(V arg0) {
        if (arg0 == null) {
            LOG.info("Success!");
        } else {
            LOG.info("Success! {}", arg0);
        }
    }

}
