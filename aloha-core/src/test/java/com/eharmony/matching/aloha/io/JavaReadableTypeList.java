package com.eharmony.matching.aloha.io;

import com.eharmony.matching.aloha.io.sources.ReadableSource;
import com.eharmony.matching.aloha.io.sources.ReadableSourceConverters;
import com.eharmony.matching.aloha.util.ICList;
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
        assertEquals("List(FileReadableType(" + FILE_LOC + "), UrlReadableType(" + URL_LOC + "))", readableSourceList.toList().toString());
    }

    @Test
    public void appendTest() throws MalformedURLException {
        final List<ReadableSource> readables =
                ICList
                .<ReadableSource>empty()
                .append(new URL(URL_LOC), ReadableSourceConverters.urlReadableConverter())
                .append(new File(FILE_LOC), ReadableSourceConverters.fileReadableConverter())
                .toList();

        assertEquals("List(UrlReadableType(" + URL_LOC + "), FileReadableType(" + FILE_LOC + "))", readables.toString());
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
}
