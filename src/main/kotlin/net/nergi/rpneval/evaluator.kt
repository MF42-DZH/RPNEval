package net.nergi.rpneval

import java.lang.Math.cbrt
import java.lang.Math.pow
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

typealias BinApp = (BigDecimal, BigDecimal) -> BigDecimal

typealias UnApp = (BigDecimal) -> BigDecimal

private enum class Associativity {
    L, N, R
}

private enum class FuncType {
    ZERO, BIN, UN
}

private open class Token(val cnt: String)

private class TkValue(cnt: String) : Token(cnt) {
    fun toBigDecimal(): BigDecimal = cnt.toBigDecimal()

    override fun toString(): String = "TkValue $cnt"
}

private class TkOper(cnt: String) : Token(cnt) {
    fun getBinOper(): BinApp? {
        return when (cnt) {
            "+" -> { x, y -> x + y }
            "*" -> { x, y -> x * y }
            "/" -> { x, y -> x.divide(y, 100, RoundingMode.HALF_UP).stripTrailingZeros() }
            "-" -> { x, y -> x - y }
            "^" -> { x, y -> pow(x.toDouble(), y.toDouble()).toBigDecimal() }
            else -> null
        }
    }

    fun getUnOper(): UnApp? {
        return when (cnt) {
            "neg" -> { x -> -x }
            "sin" -> { x -> sin(x.toDouble()).toBigDecimal() }
            "cos" -> { x -> cos(x.toDouble()).toBigDecimal() }
            "tan" -> { x -> tan(x.toDouble()).toBigDecimal() }
            "asin" -> { x -> asin(x.toDouble()).toBigDecimal() }
            "acos" -> { x -> acos(x.toDouble()).toBigDecimal() }
            "atan" -> { x -> atan(x.toDouble()).toBigDecimal() }
            "sinh" -> { x -> sinh(x.toDouble()).toBigDecimal() }
            "cosh" -> { x -> cosh(x.toDouble()).toBigDecimal() }
            "tanh" -> { x -> tanh(x.toDouble()).toBigDecimal() }
            "asinh" -> { x -> asinh(x.toDouble()).toBigDecimal() }
            "acosh" -> { x -> acosh(x.toDouble()).toBigDecimal() }
            "atanh" -> { x -> atanh(x.toDouble()).toBigDecimal() }
            "sqrt" -> { x -> sqrt(x.toDouble()).toBigDecimal() }
            "cbrt" -> { x -> cbrt(x.toDouble()).toBigDecimal() }
            "abs" -> { x -> x.abs() }
            else -> null
        }
    }

    fun getZeroOper(): BigDecimal? {
        return when (cnt) {
            "pi" -> Math.PI.toBigDecimal()
            "e" -> Math.E.toBigDecimal()
            else -> null
        }
    }

    override fun toString(): String = "TkOper $cnt"
}

private open class EvToken {
    override fun toString(): String = "Undefined Token"
}

private class EvVal(val bgDec: BigDecimal) : EvToken() {
    override fun toString(): String = bgDec.toPlainString()
}

private class EvBinApp(val op: TkOper, val v1: EvToken, val v2: EvToken) : EvToken() {
    override fun toString(): String = "($v1 ${op.cnt} $v2)"
}

private class EvUnApp(val op: TkOper, val v1: EvToken) : EvToken() {
    override fun toString(): String = "(${op.cnt} $v1)"
}

private fun tokenise(str: String): List<Token> {
    return if (str.isEmpty()) {
        emptyList()
    } else {
        when {
            str[0].isDigit() -> {
                val pred = { chr: Char -> chr.isDigit() || chr == '.' }
                listOf(TkValue(str.takeWhile(pred))) + tokenise(str.dropWhile(pred))
            }
            str[0].isLetter() -> {
                val pred = Char::isLetter
                listOf(TkOper(str.takeWhile(pred).toLowerCase())) + tokenise(str.dropWhile(pred))
            }
            "${str[0]}" in precedences.keys -> listOf(TkOper("${str[0]}")) + tokenise(str.drop(1))
            else -> tokenise(str.drop(1))
        }
    }
}

private typealias PrecedenceTable = Map<String, Triple<Int, Associativity, FuncType>>

private val precedences: PrecedenceTable = mapOf(
    "(" to Triple(1, Associativity.N, FuncType.ZERO),
    ")" to Triple(1, Associativity.N, FuncType.ZERO),
    "pi" to Triple(9, Associativity.N, FuncType.ZERO),
    "e" to Triple(9, Associativity.N, FuncType.ZERO),
    "$" to Triple(0, Associativity.R, FuncType.BIN),
    "+" to Triple(6, Associativity.L, FuncType.BIN),
    "-" to Triple(6, Associativity.L, FuncType.BIN),
    "*" to Triple(7, Associativity.L, FuncType.BIN),
    "/" to Triple(7, Associativity.L, FuncType.BIN),
    "^" to Triple(8, Associativity.R, FuncType.BIN),
    "neg" to Triple(9, Associativity.R, FuncType.UN),
    "sin" to Triple(9, Associativity.R, FuncType.UN),
    "cos" to Triple(9, Associativity.R, FuncType.UN),
    "tan" to Triple(9, Associativity.R, FuncType.UN),
    "asin" to Triple(9, Associativity.R, FuncType.UN),
    "acos" to Triple(9, Associativity.R, FuncType.UN),
    "atan" to Triple(9, Associativity.R, FuncType.UN),
    "sinh" to Triple(9, Associativity.R, FuncType.UN),
    "cosh" to Triple(9, Associativity.R, FuncType.UN),
    "tanh" to Triple(9, Associativity.R, FuncType.UN),
    "asinh" to Triple(9, Associativity.R, FuncType.UN),
    "acosh" to Triple(9, Associativity.R, FuncType.UN),
    "atanh" to Triple(9, Associativity.R, FuncType.UN),
    "sqrt" to Triple(9, Associativity.R, FuncType.UN),
    "cbrt" to Triple(9, Associativity.R, FuncType.UN),
    "abs" to Triple(9, Associativity.R, FuncType.UN),
)

@Throws(IllegalArgumentException::class)
private fun parse(lst: List<Token>, precTable: PrecedenceTable = precedences): EvToken {
    fun isSuperseder(s1: String, s2: String): Boolean {
        return (precTable[s1]!!.third == FuncType.UN) || (precTable[s1]!!.first > precTable[s2]!!.first) || ((precTable[s1]!!.first == precTable[s2]!!.first) && precTable[s1]!!.second == Associativity.R)
    }

    fun getFuncType(str: String): FuncType = precTable[str]!!.third

    @Throws(IllegalArgumentException::class)
    fun innerParse(lst: List<Token>, exprStk: MutableList<EvToken>, opStk: MutableList<TkOper>): Pair<EvToken, List<Token>> {
        if (lst.isEmpty()) {
            while (opStk.size > 1) {
                val op = opStk.removeAt(0)
                when (getFuncType(op.cnt)) {
                    FuncType.UN -> {
                        exprStk.add(EvUnApp(op, exprStk.removeAt(0)))
                    }
                    FuncType.BIN -> {
                        val t1 = exprStk.removeAt(0)
                        val t2 = exprStk.removeAt(0)
                        exprStk.add(EvBinApp(op, t2, t1))
                    }
                    FuncType.ZERO -> Unit
                }
            }

            return exprStk[0] to emptyList()
        } else {
            return when (val cur = lst[0]) {
                is TkValue -> {
                    exprStk.add(0, EvVal(cur.toBigDecimal()))
                    innerParse(lst.drop(1), exprStk, opStk)
                }
                is TkOper -> {
                    if (cur.cnt !in listOf("(", ")")) {
                        when {
                            getFuncType(cur.cnt) == FuncType.ZERO -> {
                                exprStk.add(0, EvVal(cur.getZeroOper()!!))
                                innerParse(lst.drop(1), exprStk, opStk)
                            }
                            isSuperseder(cur.cnt, opStk[0].cnt) -> {
                                opStk.add(0, cur)
                                innerParse(lst.drop(1), exprStk, opStk)
                            }
                            else -> {
                                val op = opStk.removeAt(0)
                                when (getFuncType(op.cnt)) {
                                    FuncType.UN -> {
                                        val t1 = exprStk.removeAt(0)
                                        exprStk.add(0, EvUnApp(op, t1))
                                        opStk.add(0, cur)
                                        innerParse(lst.drop(1), exprStk, opStk)
                                    }
                                    FuncType.BIN -> {
                                        val t1 = exprStk.removeAt(0)
                                        val t2 = exprStk.removeAt(0)
                                        exprStk.add(0, EvBinApp(op, t2, t1))
                                        opStk.add(0, cur)
                                        innerParse(lst.drop(1), exprStk, opStk)
                                    }
                                    FuncType.ZERO -> innerParse(lst.drop(1), exprStk, opStk)
                                }
                            }
                        }
                    } else {
                        when (cur.cnt) {
                            "(" -> {
                                val (ex, rest) = innerParse(lst.drop(1), mutableListOf(), mutableListOf(TkOper("$")))
                                exprStk.add(0, ex)
                                innerParse(rest, exprStk, opStk)
                            }
                            ")" -> {
                                innerParse(emptyList(), exprStk, opStk).first to lst.drop(1)
                            }
                            else -> throw IllegalArgumentException("Invalid bracketing! [${cur.cnt}]")
                        }
                    }
                }
                else -> throw IllegalArgumentException("Undefined Operation! [${cur.cnt}]")
            }
        }
    }

    return innerParse(lst, mutableListOf(), mutableListOf(TkOper("$"))).first
}

@Throws(IllegalArgumentException::class)
private fun eval(ev: EvToken): BigDecimal {
    return when (ev) {
        is EvVal -> ev.bgDec
        is EvBinApp -> {
            val func = ev.op.getBinOper()!!
            func(eval(ev.v1), eval(ev.v2))
        }
        is EvUnApp -> {
            val func = ev.op.getUnOper()!!
            func(eval(ev.v1))
        }
        else -> throw IllegalArgumentException("Invalid type! [$ev]?")
    }
}

private fun toStringAsRPN(expr: EvToken): String {
    return when (expr) {
        is EvVal -> expr.bgDec.toString()
        is EvBinApp -> "${toStringAsRPN(expr.v1)} ${toStringAsRPN(expr.v2)} ${expr.op.cnt}"
        is EvUnApp -> "${toStringAsRPN(expr.v1)} ${expr.op.cnt}"
        else -> "Undefined Expression!"
    }
}

private fun printAsRPN(expr: EvToken) {
    println(toStringAsRPN(expr))
}

fun toRPN(str: String): String {
    return toStringAsRPN(parse(tokenise(str)))
}

fun printStringInRPN(str: String) {
    printAsRPN(parse(tokenise(str)))
}

fun evaluateString(str: String): BigDecimal {
    return eval(parse(tokenise(str)))
}
