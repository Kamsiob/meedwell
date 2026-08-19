package com.kamsiob.meedwell

import android.app.Application
import android.content.Context

/**
 * The application object.
 *
 * Deliberately almost empty, and it stays that way. There is no analytics
 * initialiser, no crash auto-reporter, no identifier generated at first launch,
 * and nothing that phones anywhere on startup. That absence is the product, so
 * anything added here needs to justify itself against `MASTER_SPEC.md`
 * section 2.
 *
 * Crash handling arrives in Phase 5 as ACRA in local-only mode: the report is
 * written to this phone, shown to the user in full, and goes nowhere unless
 * they choose to send it themselves.
 */
class MeedwellApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}

/**
 * The one container, from anywhere with a `Context`.
 *
 * **Services must use this rather than building their own.** `AppContainer`
 * holds lazily created singletons, so a service that constructs its own gets a
 * second copy of every one of them. That was harmless while the container held
 * only repositories and stores, and it stopped being harmless the moment it
 * started holding the Surroundings player: a service with its own instance
 * would faithfully pause a player nobody could hear, while the real one carried
 * on.
 */
val Context.meedwell: AppContainer
    get() = (applicationContext as MeedwellApplication).container
