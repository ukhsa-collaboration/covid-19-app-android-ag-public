package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Provider {
    val moshi: Moshi
    val sharedPreferences: SharedPreferences
}

inline fun <reified T : Any> Provider.storage(key: String): ReadWriteProperty<Provider, T?> {
    return StorageDelegate(key, sharedPreferences, moshi, T::class.java)
}

class StorageDelegate<T : Any>(
    key: String,
    sharedPreferences: SharedPreferences,
    private val moshi: Moshi,
    private val type: Class<T>
) : ReadWriteProperty<Provider, T?> {

    private val storage = Storage(key, sharedPreferences)

    private val lock = Object()

    override fun getValue(thisRef: Provider, property: KProperty<*>): T? = synchronized(lock) {
        storage.value?.let {
            kotlin.runCatching {
                moshi.adapter(type).fromJson(it)
            }
                .getOrElse {
                    Timber.e(it)
                    null
                }
        }
    }

    override fun setValue(thisRef: Provider, property: KProperty<*>, value: T?) = synchronized(lock) {
        storage.value = if (value == null) null else moshi.adapter(type).toJson(value)
    }
}

inline fun <reified T : Any> Provider.storage(key: String, default: T): ReadWriteProperty<Provider, T> {
    return StorageDelegateWithDefault(key, sharedPreferences, moshi, T::class.java, default)
}

class StorageDelegateWithDefault<T : Any>(
    key: String,
    sharedPreferences: SharedPreferences,
    moshi: Moshi,
    type: Class<T>,
    private val default: T
) : ReadWriteProperty<Provider, T> {

    private val storageDelegate = StorageDelegate(key, sharedPreferences, moshi, type)

    override fun getValue(thisRef: Provider, property: KProperty<*>) =
        storageDelegate.getValue(thisRef, property) ?: default

    override fun setValue(thisRef: Provider, property: KProperty<*>, value: T) {
        storageDelegate.setValue(thisRef, property, value)
    }
}

class Storage(key: String, sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(key)

    var value: String? by prefs
}
