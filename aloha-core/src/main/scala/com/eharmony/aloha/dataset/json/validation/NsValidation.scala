package com.eharmony.aloha.dataset.json.validation

import com.eharmony.aloha.dataset.json.Namespace

trait NsValidation extends ValidationBase {
    val namespaces: Option[Seq[Namespace]]

    protected[this] final def validateNsNames: Option[String] =
        reportDuplicates("duplicate namespace names detected", namespaces.getOrElse(Seq.empty).view)(_.name)

    protected[this] final def validateNsFeatures: Option[String] = {
        val dupNsFeats = namespaces.getOrElse(Seq.empty).flatMap{ n =>
            val x = findDupicates(n.features.view)(identity)
            if (x.nonEmpty) Seq((n.name, x)) else Seq.empty
        }
        reportDuplicates("duplicate namespace features detected", dupNsFeats)
    }
}
