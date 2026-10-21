package com.restor.gamereel.generator

import kotlin.random.Random

interface RandomProvider {
    fun nextInt(bound: Int): Int
}

class KotlinRandomProvider : RandomProvider {
    override fun nextInt(bound: Int) = Random.nextInt(bound)
}
