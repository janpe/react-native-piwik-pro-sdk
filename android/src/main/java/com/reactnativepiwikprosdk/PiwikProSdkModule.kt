package com.reactnativepiwikprosdk

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.Promise

import pro.piwik.sdk.Piwik
import pro.piwik.sdk.Tracker
import pro.piwik.sdk.TrackerConfig
import pro.piwik.sdk.extra.TrackHelper

class PiwikProSdkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private var tracker: Tracker? = null

    override fun getName(): String {
        return "PiwikProSdk"
    }

    @ReactMethod
    fun init(baseUrl: String, siteId: String, options: ReadableMap, promise: Promise) {
        if (this.tracker != null) {
            promise.reject(Error("Tracker already initialized"));
            return;
        }

        try {
            var tracker = Piwik.getInstance(this.reactApplicationContext)
                .newTracker(TrackerConfig.createDefault(baseUrl, siteId))

            if (options.hasKey("applicationDomain")) {
                tracker.setApplicationDomain(options.getString("applicationDomain"))
            } else {
                tracker.setApplicationDomain(reactApplicationContext.packageName)
            }

            if (options.hasKey("dispatchInterval")) {
                tracker.dispatchInterval = options.getInt("dispatchInterval").toLong()
            }

            if (options.hasKey("isPrefixingEnabled")) {
              tracker.isPrefixing = options.getBoolean("isPrefixingEnabled")
            }

            this.tracker = tracker
            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(error)
        }
    }

    @ReactMethod
    fun trackScreen(path: String, optionalArgs: ReadableMap, promise: Promise) {
        try {
            var tracker = this.tracker ?: throw Exception("Tracker is not initialized")
            getTrackHelperWithDimensions(optionalArgs)
                .screen(path)
                .with(tracker)

            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(error)
        }
    }

    @ReactMethod
    fun trackEvent(category: String, action: String, optionalArgs: ReadableMap, promise: Promise) {
        try {
            var tracker = this.tracker ?: throw Exception("Tracker is not initialized")
            var track = getTrackHelperWithDimensions(optionalArgs)
                .event(category, action)

            if (optionalArgs.hasKey("name")) {
                track.name(optionalArgs.getString("name"));
            }

            if (optionalArgs.hasKey("value")) {
                track.value(optionalArgs.getDouble("value").toFloat());
            }

            track.with(tracker)

            promise.resolve(null)
        } catch (error: Exception) {
            promise.reject(error)
        }
    }

    @ReactMethod
    fun dispatch(promise: Promise) {
        try {
            (this.tracker ?: throw Exception("Tracker is not initialized")).dispatch()
            promise.resolve(null);
        } catch (error: Exception) {
            promise.reject(error)
        }
    }

    private fun getTrackHelperWithDimensions(optionalArgs: ReadableMap): TrackHelper {
        var trackHelper = TrackHelper.track()

        if (optionalArgs.hasKey("customDimensionIndex") && optionalArgs.hasKey("customDimensionValue")) {
            trackHelper.dimension(optionalArgs.getInt("customDimensionIndex"), optionalArgs.getString("customDimensionValue"))
        }

        return trackHelper
    }
}
