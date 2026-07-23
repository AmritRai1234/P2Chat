package com.p2pchat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for P2PChat.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 */
@HiltAndroidApp
class P2PChatApp : Application()
