package com.kamsiob.meedwell

import android.app.Application

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
class MeedwellApplication : Application()
