package bassamalim.hidaya.core.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.coroutines.cancellation.CancellationException

/**
 * Records a caught exception in Crashlytics as a non-fatal.
 * Coroutine cancellation is normal control flow, so it's skipped.
 */
fun Throwable.report() {
    if (this is CancellationException) return
    FirebaseCrashlytics.getInstance().recordException(this)
}
