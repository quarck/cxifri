package net.cxifri

import net.cxifri.utils.EntrophyCalculator
import net.cxifri.utils.PasswordComplexityEstimator
import org.junit.Test
import org.junit.Assert.*

class PasswordComplexityEstimatorUnittests {
    @Test
    fun stringWrappingIsCorrect() {
        val password1 = "abcdef"
        val p1 = PasswordComplexityEstimator.exhaustiveSearchIterations(password1)
        assertTrue(p1 >= 308900000); assertTrue(p1 <= 308920000)

        val password2 = "Abcdef"
        val p2 = PasswordComplexityEstimator.exhaustiveSearchIterations(password2)
        assertTrue(p2 >= 19770509663); assertTrue(p2 <= 19770709665)

        val password3 = "It is funny that you have mentioned it, than you"
        val isEng3 = PasswordComplexityEstimator.isEnglishText(password3)
        assertTrue(isEng3)

        val password4 = "Big horse eats pork and smokes the cats fur for fun"
        val isEng4 = PasswordComplexityEstimator.isEnglishText(password4)
        assertTrue(isEng4)

        val password5 = "inzsydoktehmsynkmhyenngfsjdisnhykfdjuskfnvcshulo"
        val isEng5 = PasswordComplexityEstimator.isEnglishText(password5)
        assertFalse(isEng5)

    }

}