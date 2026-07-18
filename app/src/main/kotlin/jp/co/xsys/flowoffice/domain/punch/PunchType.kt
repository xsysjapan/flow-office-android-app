package jp.co.xsys.flowoffice.domain.punch

enum class PunchType(val apiValue: String) {
    CLOCK_IN("clock_in"),
    CLOCK_OUT("clock_out"),
    BREAK_START("break_start"),
    BREAK_END("break_end"),
}
