package com.sekolah.ukscare.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Daftar kelas dipakai dropdown form (dan didokumentasikan di README).
 * Jumlah dan urutannya dijaga di sini karena data pasien lama tetap berupa teks bebas.
 */
class ConstantsTest {

    @Test
    fun `daftar kelas SMK berisi 42 entri unik dengan urutan yang benar`() {
        val daftar = Constants.DAFTAR_KELAS

        assertEquals("Jumlah kelas harus 42", 42, daftar.size)
        assertEquals("Tidak boleh ada kelas duplikat", daftar.size, daftar.toSet().size)
        assertEquals("X AKL 1", daftar.first())
        assertEquals("XII AM", daftar.last())
        assertTrue("Setiap kelas tidak boleh kosong", daftar.none { it.isBlank() })
        assertTrue(
            "Hanya tingkat X, XI, dan XII yang dipakai",
            daftar.all { it.startsWith("X ") || it.startsWith("XI ") || it.startsWith("XII ") },
        )
    }
}
