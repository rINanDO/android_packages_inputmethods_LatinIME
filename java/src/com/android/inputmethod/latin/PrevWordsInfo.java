/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.inputmethod.latin;

import android.text.TextUtils;

import com.android.inputmethod.latin.utils.StringUtils;

import java.util.Arrays;

/**
 * Class to represent information of previous words. This class is used to add n-gram entries
 * into binary dictionaries, to get predictions, and to get suggestions.
 */
public class PrevWordsInfo {
    public static final PrevWordsInfo EMPTY_PREV_WORDS_INFO =
            new PrevWordsInfo(WordInfo.EMPTY_WORD_INFO);
    public static final PrevWordsInfo BEGINNING_OF_SENTENCE =
            new PrevWordsInfo(WordInfo.BEGINNING_OF_SENTENCE);

    /**
     * Word information used to represent previous words information.
     */
    public static class WordInfo {
        public static final WordInfo EMPTY_WORD_INFO = new WordInfo(null);
        public static final WordInfo BEGINNING_OF_SENTENCE = new WordInfo();

        // This is an empty char sequence when mIsBeginningOfSentence is true.
        public final CharSequence mWord;
        // TODO: Have sentence separator.
        // Whether the current context is beginning of sentence or not. This is true when composing
        // at the beginning of an input field or composing a word after a sentence separator.
        public final boolean mIsBeginningOfSentence;

        // Beginning of sentence.
        public WordInfo() {
            mWord = "";
            mIsBeginningOfSentence = true;
        }

        public WordInfo(final CharSequence word) {
            mWord = word;
            mIsBeginningOfSentence = false;
        }

        public boolean isValid() {
            return mWord != null;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[] { mWord, mIsBeginningOfSentence } );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WordInfo)) return false;
            final WordInfo wordInfo = (WordInfo)o;
            if (mWord == null || wordInfo.mWord == null) {
                return mWord == wordInfo.mWord
                        && mIsBeginningOfSentence == wordInfo.mIsBeginningOfSentence;
            }
            return TextUtils.equals(mWord, wordInfo.mWord)
                    && mIsBeginningOfSentence == wordInfo.mIsBeginningOfSentence;
        }
    }

    // The words immediately before the considered word. EMPTY_WORD_INFO element means we don't
    // have any context for that previous word including the "beginning of sentence context" - we
    // just don't know what to predict using the information. An example of that is after a comma.
    // For simplicity of implementation, elements may also be EMPTY_WORD_INFO transiently after the
    // WordComposer was reset and before starting a new composing word, but we should never be
    // calling getSuggetions* in this situation.
    public final WordInfo[] mPrevWordsInfo;

    // Construct from the previous word information.
    public PrevWordsInfo(final WordInfo prevWordInfo) {
        mPrevWordsInfo = new WordInfo[] { prevWordInfo };
    }

    // Construct from WordInfo array. n-th element represents (n+1)-th previous word's information.
    public PrevWordsInfo(final WordInfo[] prevWordsInfo) {
        mPrevWordsInfo = prevWordsInfo;
    }

    // Create next prevWordsInfo using current prevWordsInfo.
    public PrevWordsInfo getNextPrevWordsInfo(final WordInfo wordInfo) {
        final int nextPrevWordCount = Math.min(Constants.MAX_PREV_WORD_COUNT_FOR_N_GRAM,
                mPrevWordsInfo.length + 1);
        final WordInfo[] prevWordsInfo = new WordInfo[nextPrevWordCount];
        prevWordsInfo[0] = wordInfo;
        System.arraycopy(mPrevWordsInfo, 0, prevWordsInfo, 1, prevWordsInfo.length - 1);
        return new PrevWordsInfo(prevWordsInfo);
    }

    public boolean isValid() {
        return mPrevWordsInfo.length > 0 && mPrevWordsInfo[0].isValid();
    }

    public void outputToArray(final int[][] codePointArrays,
            final boolean[] isBeginningOfSentenceArray) {
        for (int i = 0; i < mPrevWordsInfo.length; i++) {
            final WordInfo wordInfo = mPrevWordsInfo[i];
            if (wordInfo == null || !wordInfo.isValid()) {
                codePointArrays[i] = new int[0];
                isBeginningOfSentenceArray[i] = false;
                continue;
            }
            codePointArrays[i] = StringUtils.toCodePointArray(wordInfo.mWord);
            isBeginningOfSentenceArray[i] = wordInfo.mIsBeginningOfSentence;
        }
    }

    public PrevWordsInfo getTrimmedPrevWordsInfo(final int maxPrevWordCount) {
        final int newSize = Math.min(maxPrevWordCount, mPrevWordsInfo.length);
        // TODO: Quit creating a new array.
        final WordInfo[] prevWordsInfo = Arrays.copyOf(mPrevWordsInfo, newSize);
        return new PrevWordsInfo(prevWordsInfo);
    }

    public int getPrevWordCount() {
        return mPrevWordsInfo.length;
    }

    @Override
    public int hashCode() {
        // Just for having equals().
        return mPrevWordsInfo[0].hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrevWordsInfo)) return false;
        final PrevWordsInfo prevWordsInfo = (PrevWordsInfo)o;

        final int minLength = Math.min(mPrevWordsInfo.length, prevWordsInfo.mPrevWordsInfo.length);
        for (int i = 0; i < minLength; i++) {
            if (!mPrevWordsInfo[i].equals(prevWordsInfo.mPrevWordsInfo[i])) {
                return false;
            }
        }
        final WordInfo[] longerWordsInfo =
                (mPrevWordsInfo.length > prevWordsInfo.mPrevWordsInfo.length) ?
                        mPrevWordsInfo : prevWordsInfo.mPrevWordsInfo;
        for (int i = minLength; i < longerWordsInfo.length; i++) {
            if (longerWordsInfo[i] != null
                    && !WordInfo.EMPTY_WORD_INFO.equals(longerWordsInfo[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuffer builder = new StringBuffer();
        for (int i = 0; i < mPrevWordsInfo.length; i++) {
            final WordInfo wordInfo = mPrevWordsInfo[i];
            builder.append("PrevWord[");
            builder.append(i);
            builder.append("]: ");
            if (wordInfo == null) {
                builder.append("null. ");
                continue;
            }
            if (!wordInfo.isValid()) {
                builder.append("Empty. ");
                continue;
            }
            builder.append(wordInfo.mWord);
            builder.append(", isBeginningOfSentence: ");
            builder.append(wordInfo.mIsBeginningOfSentence);
            builder.append(". ");
        }
        return builder.toString();
    }
}
