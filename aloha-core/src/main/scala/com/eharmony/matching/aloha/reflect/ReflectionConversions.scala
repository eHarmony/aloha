package com.eharmony.matching.aloha.reflect

import scala.reflect.ManifestFactory

import scala.collection.JavaConversions.asScalaBuffer
import com.fasterxml.classmate.ResolvedType

object ReflectionConversions extends ReflectionConversions

trait ReflectionConversions {
    object Implicits {

        /** Provides conversions from ResolvedType to RefInfo.  ResolvedType is useful for providing erased types in
          * Java generics.  See the [[https://github.com/cowtowncoder/java-classmate classmate]] library.
          * @param rt a ResolvedType instance containing the proper type information to convert to RefInfo[A]
          */
        implicit class ResolvedTypeToScalaReflection(rt: ResolvedType) {
            /** Convert the ResolvedType instance to a RefInfo instance.
              * @tparam A the parametrized type of the ResolvedType and the resulting RefInfo.
              * @return
              */
            def toRefInfo[A]: RefInfo[A] = toManifest[A]

            /** Convert to the ResovledType into a Manifest.
              * @tparam A the parametrized type of the ResolvedType and the resulting Manifest.
              * @return
              */
            private[this] def toManifest[A]: Manifest[A] = {
                import com.fasterxml.classmate.ResolvedType

                def h[B](t: ResolvedType): Manifest[B] = {
                    val c = getParams(t)
                    if (c.isEmpty) ManifestFactory.classType[B](t.getErasedType)
                    else {
                        val m = c.map(h)
                        ManifestFactory.classType[B](t.getErasedType.asInstanceOf[Class[B]], m.head, m.drop(1):_*)
                    }
                }

                val man: Manifest[A] = h[A](rt)
                // debug("converted ResolvedType: " + t + " to manifest: " + man)
                man
            }

            private[this] def getParams(t: ResolvedType): Seq[ResolvedType] = t.getTypeParameters.toSeq
        }
    }
}
