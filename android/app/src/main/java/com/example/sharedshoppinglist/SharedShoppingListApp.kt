package com.example.sharedshoppinglist

import android.app.Application

/**
 * Application entry point. Owns no state itself — it simply ensures the default
 * [AppContainer] is available to [MainActivity]. Keeping construction here lets
 * the instrumented suite swap the seam before the first Activity is created.
 */
class SharedShoppingListApp : Application()
