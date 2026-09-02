package com.myrace

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * A19 백테스트 하네스 자리.
 * 절차(05_logic.md §6): 12개월 결과 적재 → 경주일 이전 데이터만으로 score → HitEvaluationService.summary
 * 게이트: winRate >= 0.25, placeRate >= 0.45
 */
@Disabled("실 KRA 데이터 적재 후 활성화")
class BacktestTest {
    @Test
    fun backtest12Months() { /* TODO A19 */ }
}
