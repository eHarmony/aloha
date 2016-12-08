package com.eharmony.aloha.io;

import com.eharmony.aloha.io.sources.ReadableSource;
import com.eharmony.aloha.io.sources.ReadableSourceConverters;
import com.eharmony.aloha.util.ICList;
import scala.collection.immutable.List;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import static org.junit.Assert.*;

/**
 * Show the proper of creating ReadableTypeList.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaReadableTypeList {

    private static final String URL_LOC = "http://asdf.com";
    private static final String FILE_LOC = "/";

    @Test
    public void prependTest() throws MalformedURLException {
        final ICList<ReadableSource> readableSourceList = createPrependedList();
        final List<ReadableSource> lst = readableSourceList.toList();
        assertEquals("Size not as expected:", 2, lst.size());
        assertEquals("List head not as expected:", fileReadable(), lst.head());
        assertEquals("List last element not as expected:", urlReadable(), lst.apply(1));
    }

    @Test
    public void appendTest() throws MalformedURLException {
        final List<ReadableSource> readables =
                ICList
                .<ReadableSource>empty()
                .append(new URL(URL_LOC), ReadableSourceConverters.urlReadableConverter())
                .append(new File(FILE_LOC), ReadableSourceConverters.fileReadableConverter())
                .toList();

        final List<ReadableSource> lst = readables.toList();
        assertEquals("Size not as expected:", 2, lst.size());
        assertEquals("List head not as expected:", urlReadable(), lst.head());
        assertEquals("List last element not as expected:", fileReadable(), lst.apply(1));
    }

    /**
     * Shows the proper way to create a ReadableTypeList from Java.
     * @throws MalformedURLException
     */
    private static ICList<ReadableSource> createPrependedList() throws MalformedURLException {
        return ICList
                .<ReadableSource>empty()
                .prepend(new URL(URL_LOC), ReadableSourceConverters.urlReadableConverter())
                .prepend(new File(FILE_LOC), ReadableSourceConverters.fileReadableConverter());
    }

    private static ReadableSource fileReadable() {
        return ReadableSourceConverters.fileReadableConverter().apply(new File(FILE_LOC));
    }

    private static ReadableSource urlReadable() {
        try {
            return ReadableSourceConverters.urlReadableConverter().apply(new URL(URL_LOC));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}

