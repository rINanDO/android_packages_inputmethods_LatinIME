/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.latin.personalization;

import com.android.inputmethod.latin.utils.CollectionUtils;

import android.content.Context;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class PersonalizationHelper {
    private static final String TAG = PersonalizationHelper.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final ConcurrentHashMap<String, SoftReference<UserHistoryDictionary>>
            sLangUserHistoryDictCache = CollectionUtils.newConcurrentHashMap();

    private static final ConcurrentHashMap<String,
            SoftReference<PersonalizationPredictionDictionary>>
                    sLangPersonalizationPredictionDictCache =
                            CollectionUtils.newConcurrentHashMap();

    public static UserHistoryDictionary getUserHistoryDictionary(
            final Context context, final Locale locale) {
        final String localeStr = locale.toString();
        synchronized (sLangUserHistoryDictCache) {
            if (sLangUserHistoryDictCache.containsKey(localeStr)) {
                final SoftReference<UserHistoryDictionary> ref =
                        sLangUserHistoryDictCache.get(localeStr);
                final UserHistoryDictionary dict = ref == null ? null : ref.get();
                if (dict != null) {
                    if (DEBUG) {
                        Log.w(TAG, "Use cached UserHistoryDictionary for " + locale);
                    }
                    dict.reloadDictionaryIfRequired();
                    return dict;
                }
            }
            final UserHistoryDictionary dict = new UserHistoryDictionary(context, locale);
            sLangUserHistoryDictCache.put(localeStr,
                    new SoftReference<UserHistoryDictionary>(dict));
            return dict;
        }
    }

    public static void tryDecayingAllOpeningUserHistoryDictionary() {
        for (final ConcurrentHashMap.Entry<String, SoftReference<UserHistoryDictionary>> entry
                : sLangUserHistoryDictCache.entrySet()) {
            if (entry.getValue() != null) {
                final UserHistoryDictionary dict = entry.getValue().get();
                if (dict != null) {
                    dict.decayIfNeeded();
                }
            }
        }
    }

    public static void registerPersonalizationDictionaryUpdateSession(final Context context,
            final PersonalizationDictionaryUpdateSession session, Locale locale) {
        final PersonalizationPredictionDictionary predictionDictionary =
                getPersonalizationPredictionDictionary(context, locale);
        predictionDictionary.registerUpdateSession(session);
    }

    public static PersonalizationPredictionDictionary getPersonalizationPredictionDictionary(
            final Context context, final Locale locale) {
        final String localeStr = locale.toString();
        synchronized (sLangPersonalizationPredictionDictCache) {
            if (sLangPersonalizationPredictionDictCache.containsKey(localeStr)) {
                final SoftReference<PersonalizationPredictionDictionary> ref =
                        sLangPersonalizationPredictionDictCache.get(localeStr);
                final PersonalizationPredictionDictionary dict = ref == null ? null : ref.get();
                if (dict != null) {
                    if (DEBUG) {
                        Log.w(TAG, "Use cached PersonalizationPredictionDictionary for " + locale);
                    }
                    return dict;
                }
            }
            final PersonalizationPredictionDictionary dict =
                    new PersonalizationPredictionDictionary(context, locale);
            sLangPersonalizationPredictionDictCache.put(
                    localeStr, new SoftReference<PersonalizationPredictionDictionary>(dict));
            return dict;
        }
    }
}
