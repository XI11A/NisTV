package com.nis.nistv

import org.junit.Test
import org.junit.Assert.assertNotNull

class StartupTest {

    @Test
    fun testClassesLoad() {
        assertNotNull(Class.forName("com.nis.nistv.NisTvApplication"))
        assertNotNull(Class.forName("com.nis.nistv.MainActivity"))
        assertNotNull(Class.forName("com.nis.nistv.feature.home.HomeScreenKt"))
        assertNotNull(Class.forName("com.nis.nistv.feature.home.HomeViewModel"))
        assertNotNull(Class.forName("com.nis.nistv.core.designsystem.component.NisLogoKt"))
        assertNotNull(Class.forName("com.nis.nistv.core.designsystem.component.NisAnimatedSvgLogoKt"))
        assertNotNull(Class.forName("com.nis.nistv.feature.splash.SplashScreenKt"))
    }
}
