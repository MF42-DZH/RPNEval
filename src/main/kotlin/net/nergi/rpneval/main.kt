package net.nergi.rpneval

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("RPNEval: Type in an expression as an argument (within \"\").")
    } else {
        try {
            when (args[0]) {
                "--rpn" -> printStringInRPN(args[1])
                "--eval" -> println(evaluateString(args[1]))
                "--rpn-eval" -> println("${toRPN(args[1])} = ${evaluateString(args[1])}")
                else -> println(evaluateString(args[0]))
            }
        } catch (e: Exception) {
            println("Invalid expression or argument combination:")
            for (i in args) {
                println("  $i")
            }
        }
    }
}
