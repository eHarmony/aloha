package com.eharmony.aloha.cli.dataset

import java.io.{ByteArrayOutputStream, IOException}
import java.util.Arrays

import com.eharmony.aloha.test.proto.Testing.{PhotoProto, UserProto}
import com.eharmony.aloha.test.proto.Testing.GenderProto.{FEMALE, MALE}
import com.google.protobuf.GeneratedMessage
import org.apache.commons.codec.binary.Base64
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.{Ignore, Test}

/**
 * Prints:
 * {{{
 * CAESBEFsYW4YASUAALhBKg0IARABGQAAAAAAAPA/Kg0IAhACGQAAAAAAAABA
 * CAESBEthdGUYAioNCAMQAxkAAAAAAAAIQA==
 * }}}
 *
 * Used for testing the CLI.
 */
@RunWith(classOf[BlockJUnit4ClassRunner])
@Ignore
class PrintProtosTest {
    @Test def testPrintProtos(): Unit = {
        System.out.println(alan)
        System.out.println(kate)
    }

    @throws(classOf[IOException])
    def alan: String = {
        val t = UserProto.newBuilder.
            setId(1).
            setName("Alan").
            setGender(MALE).
            setBmi(23).
            addAllPhotos(Arrays.asList(
                PhotoProto.newBuilder.
                    setId(1).
                    setAspectRatio(1).
                    setHeight(1).
                    build,
                PhotoProto.newBuilder.
                    setId(2).
                    setAspectRatio(2).
                    setHeight(2).build
            )).build
        b64(t)
    }

    def kate: String = {
        val t = UserProto.newBuilder.
            setId(1).
            setName("Kate").
            setGender(FEMALE).
            addAllPhotos(Arrays.asList(
                PhotoProto.newBuilder.
                    setId(3).
                    setAspectRatio(3).
                    setHeight(3).
                    build
            )).build
        b64(t)
    }

    def b64[M <: GeneratedMessage](p: M): String = {
        val baos: ByteArrayOutputStream = new ByteArrayOutputStream
        p.writeTo(baos)
        new String(Base64.encodeBase64(baos.toByteArray))
    }
}
