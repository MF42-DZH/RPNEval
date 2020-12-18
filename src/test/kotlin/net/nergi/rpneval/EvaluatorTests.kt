package net.nergi.rpneval

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EvaluatorTests {
    @Test
    fun `test if basic expressions evaluate properly`() {
        assertEquals("7".toBigDecimal(), evaluateString("5 + 2"))
        assertEquals("3".toBigDecimal(), evaluateString("5 - 2"))
        assertEquals("10".toBigDecimal(), evaluateString("5 * 2"))
        assertEquals("2.5".toBigDecimal(), evaluateString("5 / 2"))
    }

    @Test
    fun `test if compound expressions evaluate properly`() {
        assertEquals("6".toBigDecimal(), evaluateString("1 + 2 + 3"))
        assertEquals("-4".toBigDecimal(), evaluateString("1 - 2 - 3"))
        assertEquals("7".toBigDecimal(), evaluateString("1 + 2 * 3"))
        assertEquals("2".toBigDecimal(), evaluateString("1 + 3 / 3"))
        assertEquals("2".toBigDecimal(), evaluateString("(1 + 3) / 2"))
        assertEquals("2".toBigDecimal(), evaluateString("1 - (2 - 3)"))
    }

    @Test
    fun `test if unary operators work alone and with binary and zeroary operators`() {
        assertEquals("-1".toBigDecimal(), evaluateString("neg 1"))
        assertEquals("5".toBigDecimal(), evaluateString("neg 1 + 6"))
        assertEquals("-7".toBigDecimal(), evaluateString("neg (1 + 6)"))
        assertEquals("-7".toBigDecimal(), evaluateString("neg neg neg (1 + 6)"))

        assertNotEquals("1".toBigDecimal(), evaluateString("sin 0"))
        assertNotEquals("1".toBigDecimal(), evaluateString("sin pi"))
    }

    @Test
    fun `test if zeroary operators work`() {
        assertEquals(Math.PI.toBigDecimal(), evaluateString("pi"))
        assertEquals(Math.E.toBigDecimal(), evaluateString("e"))
    }

    @Test
    fun `test if RPN rewriting is working`() {
        assertEquals("5 2 +", toRPN("5 + 2"))
        assertEquals("5 2 -", toRPN("5 - 2"))
        assertEquals("5 2 *", toRPN("5 * 2"))
        assertEquals("5 2 /", toRPN("5 / 2"))
        assertEquals("5 2 + 4 -", toRPN("5 + 2 - 4"))
    }
}
