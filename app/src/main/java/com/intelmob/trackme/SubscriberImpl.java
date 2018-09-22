package com.intelmob.trackme;

import android.util.Log;

import io.reactivex.subscribers.ResourceSubscriber;

public class SubscriberImpl<T> extends ResourceSubscriber<T> {

    @Override
    public void onNext(T t) {

    }

    @Override
    public void onError(Throwable t) {
        Log.e("Error", t.getLocalizedMessage(), t);
    }

    @Override
    public void onComplete() {

    }
}
