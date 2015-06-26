package com.eharmony.aloha.semantics.compiled.plugin.proto

import scalaz.ValidationNel
import scalaz.std.list._
import scalaz.syntax.traverse._
import scalaz.syntax.validation.ToValidationV // scalaz.syntax.validation.ToValidationOps for latest scalaz

import com.google.protobuf.Descriptors.{ FieldDescriptor, Descriptor }

private[proto] object DescriptorPimpz {

  /**
   * @param d
   * @param fieldName
   * @return
   */
  def d2fd(d: Descriptor, fieldName: String): Option[FieldDescriptor] = Option(d findFieldByName fieldName)

  /**
   * @param fd
   * @param fieldName
   * @return
   */
  def fd2fd(fd: FieldDescriptor, fieldName: String): Option[FieldDescriptor] =
    if (fd.getType == FieldDescriptor.Type.MESSAGE) d2fd(fd.getMessageType, fieldName) else None

  /**
   * Continually flatMap the field String names to optional FieldDescriptors to produce a
   * FieldDescriptor in the end.
   * @param d
   * @return
   */
  implicit class PimpedDescriptor(d: Descriptor) extends (String => Option[FieldDescriptor]) {
    def apply(fieldName: String) = d2fd(d, fieldName)

    def subfields(fields: Traversable[String]): ValidationNel[String, List[FieldDescriptor]] = {
      def err(f: String, lst: List[String] = Nil): String =
        "For subfield '" + fields.mkString(".") + "' in " + d.getFullName +
          ", '" + lst.reverse.mkString(".") + "." + f + "' is not found."

      // Right project and flatMap.  Use fd2fd and convert values to Right.  For errors, construct an error
      // message and place in Left.  This will produce an Either.  In the second field of the 2-tuple, accumulate
      // the fields in reverse order.  This is OK because we only use the reversed list once on an error. If all
      // goes well we'll never use it, so guaranteed O(1) insertion time is important.  a contains the list of
      // either accumulated.  Once we sequence a, we get either an error message or a list of successes.
      fields.toList match {
        case h :: t =>
          // Force the type of z so the foldLeft doesn't need types.

          val z: (ValidationNel[String, FieldDescriptor], List[String]) =
            (d2fd(d, h).map(_.successNel).getOrElse(err(h).failNel), List(h))

          val a = t.scanLeft(z) {
            case ((z, l), f) => {
              val b = z.flatMap(fd2fd(_, f).map(_.successNel).getOrElse(err(f, l).failNel))
              (b, f :: l)
            }
          }.unzip._1

          a.sequence[({ type L[A] = ValidationNel[String, A] })#L, FieldDescriptor]
        case Nil => ("Cannot get subfield for fields=Nil in " + d.getFullName + ".").failNel
      }
    }

    def subfields(desc: String): ValidationNel[String, List[FieldDescriptor]] = subfields(desc.split("\\."))

    def subfield(fields: Traversable[String]): ValidationNel[String, FieldDescriptor] = subfields(fields).fold(_.fail, _.last.successNel)

    /**
     * Get a subfield
     *
     * {{{
     * import com.eharmony.matching.common.value.UserProtoBuffs.UserProto
     * import DescriptorPimpz.Implicits._
     * val v1 = UserProto.getDescriptor.subfield("user_activity.communication.aggregates.by_day.date")
     * assert(v1.isRight)
     * assert(v1.fold(_ => false, _.getName == "date"))
     * }}}
     *
     * @param desc
     * @return
     */
    def subfield(desc: String): ValidationNel[String, FieldDescriptor] = subfield(desc.split("\\."))

    /**
     * Get a list of all full descriptions
     * @return
     */
    def allSubfields: List[String] = {
      type FDesc = (String, FieldDescriptor)
      import collection.JavaConversions.asScalaBuffer

      def partition(fd: List[FDesc]) = fd.partition(_._2.getType == FieldDescriptor.Type.MESSAGE)

      def h(messages: List[FDesc], nonMessages: List[FDesc]): List[String] =
        if (messages.isEmpty) nonMessages.map(_._1)
        else {
          val (m, nm) = partition(for { m <- messages; f <- m._2.getMessageType.getFields } yield (m._1 + "." + f.getName, f))
          h(m, nm ::: nonMessages)
        }

      h(d.getFields.map(f => (f.getName, f)).toList, List.empty[FDesc]).sortWith(_ < _)
    }
  }

  implicit class PimpedFieldDescriptor(d: FieldDescriptor) extends (String => Option[FieldDescriptor]) {
    def apply(fieldName: String) = fd2fd(d, fieldName)
  }
}
