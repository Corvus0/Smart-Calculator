package calculator

import java.math.BigInteger
import kotlin.math.pow

fun main() {
    val calc = Calculator()
    var running = true
    while (running) {
        val input = readln().trim()
        when {
            input.isEmpty() -> continue
            Regex("^/\\w+").matches(input) -> running = parseCommand(input)
            input.contains("=") -> calc.setVariable(input)
            else -> calc.parseInput(input)
        }
    }
}

fun parseCommand(input: String): Boolean {
    when (input) {
        "/exit" -> {
            println("Bye!")
            return false
        }
        "/help" -> {
            println("The program performs basic arithmetic and can store variables")
        }
        else -> println("Unknown command")
    }
    return true
}

fun parseOperator(token: String): Operator? {
    var sub = false
    for (c in token) {
        when (c) {
            '+' -> continue
            '-' -> sub = !sub
            '*' -> return if (token.length == 1) Operator.MUL else null
            '/' -> return if (token.length == 1) Operator.DIV else null
            '^' -> return if (token.length == 1) Operator.EXP else null
            '(' -> return Operator.OPEN_PAR
            ')' -> return Operator.CLOSE_PAR
            else -> return null
        }
    }
    return if (sub) Operator.SUB else Operator.ADD
}

fun operatorToString(op: Operator): String {
    return when (op) {
        Operator.SUB -> "-"
        Operator.ADD -> "+"
        Operator.DIV -> "/"
        Operator.MUL -> "*"
        Operator.EXP -> "^"
        Operator.OPEN_PAR -> "("
        Operator.CLOSE_PAR -> ")"
        else -> ""
    }
}

fun performOperation(left: BigInteger, right: BigInteger, operation: String): BigInteger {
    return when (operation) {
        "+" -> left + right
        "-" -> left - right
        "*" -> left * right
        "/" -> left / right
        "^" -> left.toBigDecimal().pow(right.intValueExact()).toBigInteger()
        else -> left
    }
}

fun validateKey(key: String): Boolean {
    return if (Regex("[a-zA-Z]+").matches(key)) {
        true
    } else {
        println("Invalid identifier")
        false
    }
}

fun validateNumber(num: String): Boolean {
    return Regex("-?\\d+").matches(num)
}

fun validateOperator(op: String): Boolean {
    return Regex("""[()^*/]|[+\-]+""").matches(op)
}

enum class Operator(val priority: Int) {
    SUB(0), ADD(0), DIV(1), MUL(1), EXP(2), OPEN_PAR(3), CLOSE_PAR(3)
}

class Calculator {
    private val variables = mutableMapOf<String, BigInteger>()
    private val operators = ArrayDeque<Operator>()
    private val result = ArrayDeque<String>()

    private fun resetStacks() {
        operators.clear()
        result.clear()
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        var curr = ""
        var isOp = false
        var prevOp = false
        for (c in input) {
            val c = c.toString()
            isOp = when {
                validateNumber(c) || (c == "-" && curr.isEmpty() && !prevOp) -> false
                validateOperator(c) -> true
                Regex("\\w+").matches(c) -> false
                else -> continue
            }
            if (isOp) {
                if (!prevOp && curr.isNotEmpty()) tokens.add(curr)
                if (prevOp && Regex("[*/+\\-^]+").matches(tokens.last() + c)) {
                    tokens.add(tokens.removeLast() + c)
                } else {
                    tokens.add(c)
                    curr = ""
                }
            } else {
                curr += c
            }
            prevOp = isOp
        }
        if (curr.isNotEmpty()) tokens.add(curr)
        return tokens
    }

    private fun parseExpr(expr: String): BigInteger? {
        resetStacks()
        val tokens = tokenize(expr)
        for (token in tokens) {
            when {
                validateNumber(token) -> {
                    result.addLast(token)
                }
                Regex("\\w+").matches(token) -> {
                    val num = getVariable(token) ?: return null
                    result.addLast(num.toString())
                }
                validateOperator(token) -> {
                    if (!convertToPostfix(token)) return null
                }
                else -> {
                    println("Invalid expression")
                    return null
                }
            }
        }
        while (operators.isNotEmpty()) {
            val op = operatorToString(operators.removeLast())
            if (op == "(" || op == ")") {
                println("Invalid expression")
                return null
            }
            result.addLast(op)
        }
        return calculateResult()
    }

    fun parseInput(input: String) {
        val res = parseExpr(input)
        if (res != null) println(res)
    }

    private fun convertToPostfix(token: String): Boolean {
        val op = parseOperator(token)
        if (op == null) {
            println("Invalid expression")
            return false
        }
        if (operators.isEmpty()) {
            operators.addLast(op)
            return true
        }
        val prevOp = operators.last()
        if (op == Operator.CLOSE_PAR) {
            var curr = operators.removeLast()
            while(curr != Operator.OPEN_PAR) {
                result.addLast(operatorToString(curr))
                try {
                    curr = operators.removeLast()
                } catch (e: Exception) {
                    println("Invalid expression")
                    return false
                }
            }
        } else if (prevOp == Operator.OPEN_PAR || op.priority > prevOp.priority ||op == Operator.OPEN_PAR) {
            operators.addLast(op)
        } else {
            do {
                var curr = operators.removeLast()
                result.addLast(operatorToString(curr))
                if (operators.isEmpty()) break
                curr = operators.last()
            }
            while(curr != Operator.OPEN_PAR && op < curr)
            operators.addLast(op)
        }
        return true
    }o

    private fun calculateResult(): BigInteger? {
        val stack = ArrayDeque<String>()
        for (token in result) {
            when {
                validateNumber(token) -> stack.addLast(token)
                Regex("\\w+").matches(token) -> {
                    val value = getVariable(token)
                    if (value != null) stack.addLast(value.toString()) else return null
                }
                validateOperator(token) -> {
                    val right = stack.removeLast().toBigInteger()
                    val left = stack.removeLast().toBigInteger()
                    val result = performOperation(left, right, token).toString()
                    stack.addLast(result)
                }
            }
        }
        return stack.removeLast().toBigInteger()
    }

    private fun getVariable(input: String): BigInteger? {
        if (validateKey(input)) {
            val value = variables[input]
            if (value != null) return value else println("Unknown variable")
        }
        return null
    }

    fun setVariable(input: String) {
        if (!Regex("""\w+\W*=[^=]+""").matches(input)) {
            println("Invalid expression")
            return
        }
        val tokens = input.split("=").map { it.trim() }
        val key = tokens[0]
        val valueExpr = tokens[1]
        if (!validateKey(key)) return
        val value = parseExpr(valueExpr)
        if (value != null) variables[key] = value
    }
}
