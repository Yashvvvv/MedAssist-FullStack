package com.medassist.app.core

/** Real responses captured from the backend (demo data), so model changes are checked against the server's actual JSON. */
object Fixtures {
    fun read(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fixtures/$name.json")) { "missing fixture $name" }.readText()
}
