package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Provider {
    val moshi: Moshi
    val sharedPreferences: SharedPreferences
}

class StorageDelegate<T : Any>(
    key: String,
    sharedPreferences: SharedPreferences,
    moshi: Moshi,
    type: Type
) : ReadWriteProperty<Provider, T?> {

    private val storage = Storage(key, sharedPreferences)
    private val adapter: JsonAdapter<T> = moshi.adapter(type)

    private val lock = Object()

    override fun getValue(thisRef: Provider, property: KProperty<*>): T? = synchronized(lock) {
        storage.value?.let {
            kotlin.runCatching {
                adapter.fromJson(it)
            }
                .getOrElse {
                    Timber.e(it)
                    null
                }
        }
    }

    override fun setValue(thisRef: Provider, property: KProperty<*>, value: T?) = synchronized(lock) {
        storage.value = if (value == null) null else adapter.toJson(value)
    }
}

class StorageDelegateWithDefault<T : Any>(
    key: String,
    sharedPreferences: SharedPreferences,
    moshi: Moshi,
    type: Type,
    private val default: T
) : ReadWriteProperty<Provider, T> {

    private val storageDelegate = StorageDelegate<T>(key, sharedPreferences, moshi, type)

    override fun getValue(thisRef: Provider, property: KProperty<*>) =
        storageDelegate.getValue(thisRef, property) ?: default

    override fun setValue(thisRef: Provider, property: KProperty<*>, value: T) {
        storageDelegate.setValue(thisRef, property, value)
    }
}

inline fun <reified T : Any> Provider.storage(key: String): ReadWriteProperty<Provider, T?> {
    return StorageDelegate(key, sharedPreferences, moshi, T::class.java)
}

inline fun <reified T : Any> Provider.storage(key: String, default: T): ReadWriteProperty<Provider, T> {
    return StorageDelegateWithDefault(key, sharedPreferences, moshi, T::class.java, default)
}

inline fun <reified T : Any> Provider.listStorage(key: String): ReadWriteProperty<Provider, List<T>?> {
    val type = Types.newParameterizedType(
        List::class.java,
        T::class.java
    )
    return StorageDelegate(key, sharedPreferences, moshi, type)
}

inline fun <reified T : Any> Provider.listStorage(key: String, default: List<T>): ReadWriteProperty<Provider, List<T>> {
    val type = Types.newParameterizedType(
        List::class.java,
        T::class.java
    )
    return StorageDelegateWithDefault(key, sharedPreferences, moshi, type, default)
}

inline fun <reified T : Any> Provider.setStorage(key: String): ReadWriteProperty<Provider, Set<T>?> {
    val type = Types.newParameterizedType(
        Set::class.java,
        T::class.java
    )
    return StorageDelegate(key, sharedPreferences, moshi, type)
}

inline fun <reified T : Any> Provider.setStorage(key: String, default: Set<T>): ReadWriteProperty<Provider, Set<T>> {
    val type = Types.newParameterizedType(
        Set::class.java,
        T::class.java
    )
    return StorageDelegateWithDefault(key, sharedPreferences, moshi, type, default)
}

inline fun <reified K : Any, reified V : Any> Provider.mapStorage(key: String): ReadWriteProperty<Provider, Map<K, V>?> {
    val type = Types.newParameterizedType(
        Map::class.java,
        K::class.java,
        V::class.java
    )
    return StorageDelegate(key, sharedPreferences, moshi, type)
}

inline fun <reified K : Any, reified V : Any> Provider.mapStorage(key: String, default: Map<K, V>): ReadWriteProperty<Provider, Map<K, V>> {
    val type = Types.newParameterizedType(
        Map::class.java,
        K::class.java,
        V::class.java
    )
    return StorageDelegateWithDefault(key, sharedPreferences, moshi, type, default)
}

class Storage(key: String, sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(key)

    var value: String? by prefs
}
