package com.eharmony.matching.aloha.factory.ex

case class RecursiveModelDefinitionException(fileStack: List[String])
    extends AlohaFactoryException(RecursiveModelDefinitionException.msg(fileStack))

private[this] object RecursiveModelDefinitionException {
    def msg(fileStack: List[String]) = {
        fileStack.reverse.zipWithIndex.map{
            case(v, i) if i == 0 => v
            case(v, i) => Iterator.fill(i)("  ").mkString("", "", " => " + v)
        }.mkString("Illegal recursive model definition found:\n", "\n", "")
    }
}
