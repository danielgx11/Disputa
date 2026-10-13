package disputa.domain.rules

fun isPowerOfTwo(n: Int): Boolean = n >= 2 && (n and (n - 1)) == 0

fun nextPowerOfTwo(n: Int): Int {
    if (n <= 2) return 2
    var p = 1
    while (p < n) p = p shl 1
    return p
}
