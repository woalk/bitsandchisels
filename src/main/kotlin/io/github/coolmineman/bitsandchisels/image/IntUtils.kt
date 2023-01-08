package io.github.coolmineman.bitsandchisels.image

fun Int.ceilDiv(other: Int) = this.div(other) + if (this.rem(other) > 0) 1 else 0
