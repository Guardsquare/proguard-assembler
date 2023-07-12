package com.guardsquare.proguard

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class PartialEvaluatorErrorsTest : FreeSpec({
    "Ahh, surprise" {
        10 * 2 shouldBe 20
    }
})
