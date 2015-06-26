package com.eharmony.aloha.cli

import java.lang.reflect.Modifier

import com.eharmony.aloha.annotate.CLI
import org.reflections.Reflections

import scala.collection.JavaConversions.asScalaSet
import com.eharmony.aloha.pkgName

/**
 * Created by rdeak on 6/16/15.
 */
object Cli {
    def main(args: Array[String]): Unit = {

        if (args.isEmpty) {
            System.err.println("No arguments supplied. Supply one of: " + flagClassMap.keys.toVector.sorted.map("'" + _ + "'").mkString(", ") + ".")
        }
        else {
            val flag = args(0)
            if (!flagClassMap.contains(flag)) {
                System.err.println(s"'$flag' supplied. Supply one of: " + flagClassMap.keys.toVector.sorted.map("'" + _ + "'").mkString(", ") + ".")
            }
            else {
                flagClassMap(flag).
                    getMethod("main", classOf[Array[String]]).
                    invoke(null, args.tail)
            }
        }
    }

    private[cli] lazy val cliClasses = {
        // TODO: Change back to: 'val reflections = new Reflections(pkgName)' once featureSpecExtractor is moved.
        val reflections = new Reflections("com.eharmony.matching.featureSpecExtractor", pkgName)

        // We want to classes with the static forwarders, not the singleton (module) classes.
        reflections.getTypesAnnotatedWith(classOf[CLI]).toSet.asInstanceOf[Set[Class[Any]]].collect { case c if hasStaticMain(c) =>  c }
    }

    private[this] def hasStaticMain(c: Class[Any]): Boolean =
        !c.getName.endsWith("$") && Option(c.getMethod("main", classOf[Array[String]])).exists(m => Modifier.isStatic(m.getModifiers))


    private[this] lazy val flagClassMap = cliClasses.map{ case c => c.getAnnotation(classOf[CLI]).flag() -> c }.toMap
}
