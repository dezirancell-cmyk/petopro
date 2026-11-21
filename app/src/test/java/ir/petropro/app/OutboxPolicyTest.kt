package ir.petropro.app

import ir.petropro.app.domain.OutboxAction
import ir.petropro.app.domain.OutboxPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxPolicyTest {
    @Test fun successCodes() {
        assertEquals(OutboxAction.SUCCESS, OutboxPolicy.decide(200, null, false))
        assertEquals(OutboxAction.SUCCESS, OutboxPolicy.decide(204, null, false))
    }

    @Test fun authRequired() = assertEquals(OutboxAction.AUTH_REQUIRED, OutboxPolicy.decide(401, null, false))

    @Test fun expiredSubscriptionIsBlockedNotLost() {
        assertEquals(OutboxAction.BLOCKED, OutboxPolicy.decide(403, null, true))
        assertEquals(OutboxAction.REJECTED, OutboxPolicy.decide(403, null, false))
    }

    @Test fun conflictVsInProgress() {
        assertEquals(OutboxAction.CONFLICT, OutboxPolicy.decide(409, "item_removed", false))
        // request_in_progress (بدون فیلد conflict) باید دوباره با همان کلید تلاش شود
        assertEquals(OutboxAction.RETRY, OutboxPolicy.decide(409, null, false))
    }

    @Test fun transientErrorsRetry() {
        for (code in listOf(408, 425, 429, 500, 502, 503)) assertEquals("code $code", OutboxAction.RETRY, OutboxPolicy.decide(code, null, false))
    }

    @Test fun deterministicClientErrorsAreRejected() {
        for (code in listOf(400, 404, 422)) assertEquals("code $code", OutboxAction.REJECTED, OutboxPolicy.decide(code, null, false))
    }

    @Test fun backoffGrowsAndIsCapped() {
        assertEquals(5_000L, OutboxPolicy.backoffMillis(0))
        assertEquals(10_000L, OutboxPolicy.backoffMillis(1))
        assertTrue(OutboxPolicy.backoffMillis(3) > OutboxPolicy.backoffMillis(2))
        assertEquals(15 * 60_000L, OutboxPolicy.backoffMillis(50))
    }
}
