package com.vcashorg.vcashwallet.net;


import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class CommonObserver<T> implements Observer<T> {

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(T result) {
        if (result == null) {
            onError(null);
            return;
        }

        onSuccess(result);
    }

    @Override
    public void onError(Throwable e) {
        if(e != null){
            onFailure(e,RxExceptionUtil.exceptionHandler(e));
        }
    }

    @Override
    public void onComplete() {

    }


    public abstract void onSuccess(T result);


    public abstract void onFailure(Throwable e,String errorMsg);
}
