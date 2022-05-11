package uk.nhs.nhsx.covid19.android.app.util.adapters

import androidx.annotation.Nullable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonAdapter.Factory
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Annotate fields that should be serialized as null with this.
 * NOTE: Should not be used on nested objects as this will serialize nulls throughout that node.
 */

@Retention(RUNTIME)
@JsonQualifier
annotation class SerializeNulls {
    companion object {
        val jsonAdapterFactory: Factory = object : Factory {
            @Nullable
            override fun create(type: Type, annotations: Set<Annotation?>, moshi: Moshi): JsonAdapter<*>? {
                val nextAnnotations: Set<Annotation?> = Types.nextAnnotations(
                    annotations,
                    SerializeNulls::class.java
                ) ?: return null
                return moshi.nextAdapter<Any>(this, type, nextAnnotations).serializeNulls()
            }
        }
    }
}
