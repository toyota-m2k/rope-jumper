package io.github.toyota32k.ropejumper.system

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.github.toyota32k.bindit.Command
import io.github.toyota32k.utils.IDisposable
import io.reactivex.rxjava3.disposables.Disposable

//fun IDisposable.toDisposable():Disposable {
//    class IDisposableImpl(val x:IDisposable):Disposable {
//        override fun dispose() {
//            x.dispose()
//        }
//
//        override fun isDisposed(): Boolean {
//            return x.isDisposed()
//        }
//    }
//    return IDisposableImpl(this)
//}

class DisposableEternalObserver<T>(target: LiveData<T>, val fn:(value:T)->Unit): Observer<T>, IDisposable {
    var liveData:LiveData<T>? = target
    init {
        target.observeForever(this)
    }
    override fun onChanged(t: T) {
        fn(t)
    }

    override fun dispose() {
        liveData?.removeObserver(this)
        liveData = null
    }

    override fun isDisposed(): Boolean {
        return liveData==null
    }
}

class DisposablePool : IDisposable {
    private var list:MutableList<IDisposable>? = mutableListOf<IDisposable>()

    fun add(v:IDisposable) {
        list?.add(v)
    }

    operator fun plus(v:IDisposable) {
        list?.add(v)
    }

    operator fun minus(v:IDisposable) {
        if(list?.remove(v)?:false) {
            v.dispose()
        }
    }

    override fun dispose() {
        list?.apply {
            forEach {
                it.dispose()
            }
            clear()
        }
        list = null
    }

    override fun isDisposed(): Boolean {
        return list != null
    }

    fun <T> observeForever(target:LiveData<T>, fn:(T)->Unit):DisposablePool {
        add(DisposableEternalObserver(target,fn))
        return this
    }

    fun bindForever(target: Command, fn:()->Unit) :DisposablePool {
        add(target.bindForever(fn))
        return this
    }
}