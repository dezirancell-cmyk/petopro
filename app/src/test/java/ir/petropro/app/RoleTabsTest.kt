package ir.petropro.app

import ir.petropro.app.domain.RoleTabs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ساختار نقش‌ها باید با TABS/CORE_TAB_KEYS/EXTRA_TAB_KEYS در وب (varzesh510.py) یکی باشد. */
class RoleTabsTest {
    @Test fun tabCountsMatchWeb() {
        assertEquals(17, RoleTabs.allInWeb(RoleTabs.ADMIN).size)
        assertEquals(13, RoleTabs.allInWeb(RoleTabs.COACH).size)
        assertEquals(5, RoleTabs.allInWeb(RoleTabs.ATHLETE).size)
        assertEquals(6, RoleTabs.allInWeb(RoleTabs.PRO_ATHLETE).size)
    }

    @Test fun proAthleteHasRecommendationsButAthleteDoesNot() {
        assertTrue(RoleTabs.allInWeb(RoleTabs.PRO_ATHLETE).any { it.key == "recommendations" })
        assertFalse(RoleTabs.allInWeb(RoleTabs.ATHLETE).any { it.key == "recommendations" })
    }

    @Test fun managementTabsAreRoleSpecific() {
        assertTrue(RoleTabs.allInWeb(RoleTabs.ADMIN).any { it.key == "users" })
        assertFalse(RoleTabs.allInWeb(RoleTabs.COACH).any { it.key == "users" })
        assertTrue(RoleTabs.allInWeb(RoleTabs.COACH).any { it.key == "athletes" })
        assertFalse(RoleTabs.allInWeb(RoleTabs.ATHLETE).any { it.key == "athletes" })
        assertTrue(RoleTabs.allInWeb(RoleTabs.ADMIN).any { it.key == "settings" })
    }

    @Test fun readinessIsAthleteOnly() {
        assertTrue(RoleTabs.allInWeb(RoleTabs.ATHLETE).any { it.key == "readiness" })
        assertTrue(RoleTabs.allInWeb(RoleTabs.PRO_ATHLETE).any { it.key == "readiness" })
        assertFalse(RoleTabs.allInWeb(RoleTabs.COACH).any { it.key == "readiness" })
    }

    @Test fun onlyImplementedTabsAreShown() {
        for (role in listOf(RoleTabs.ADMIN, RoleTabs.COACH, RoleTabs.ATHLETE, RoleTabs.PRO_ATHLETE)) {
            assertTrue(RoleTabs.available(role).all { it.key in RoleTabs.IMPLEMENTED })
            // هر تب در منوی پایین باید از بخش core وب باشد و هر تب دیگر در extras بیاید، بدون تکرار
            val keys = RoleTabs.core(role).map { it.key } + RoleTabs.extras(role).map { it.key }
            assertEquals(keys.size, keys.toSet().size)
        }
    }

    @Test fun athleteCoreOrderMatchesWeb() {
        assertEquals(listOf("dashboard", "readiness", "myprogram"), RoleTabs.core(RoleTabs.ATHLETE).map { it.key })
        assertEquals(listOf("dashboard", "myprogram"), RoleTabs.core(RoleTabs.COACH).map { it.key })
    }

    @Test fun unknownRoleFallsBackToAthlete() {
        assertEquals(RoleTabs.allInWeb(RoleTabs.ATHLETE), RoleTabs.allInWeb("something"))
        assertEquals(RoleTabs.allInWeb(RoleTabs.ATHLETE), RoleTabs.allInWeb(null))
    }
}
