package com.sekolah.ukscare.util

import com.sekolah.ukscare.data.model.Pasien
import org.junit.Assert.assertEquals
import org.junit.Test

class PesanGeneratorTest {

    private val waktuMasuk = "Senin, 19 Okt 2026 • 09.30"

    @Test
    fun `pesan dengan obat mengikuti template AGENTS md`() {
        val pesan = PesanGenerator.buatPesan(
            nama = "Andi Saputra",
            kelas = "VII-A",
            keluhan = "Pusing, Demam",
            tindakan = "Istirahat, Diberi Obat",
            obat = "Paracetamol",
            waktuMasuk = waktuMasuk,
        )

        assertEquals(
            """
            *SURAT IZIN ISTIRAHAT - UKS*

            Kepada Yth. Wali Kelas / Guru Piket
            Kelas: VII-A

            Dengan hormat, siswa berikut sedang berada di UKS:
            Nama: Andi Saputra
            Kelas: VII-A
            Keluhan: Pusing, Demam
            Tindakan: Istirahat, Diberi Obat
            Obat diberikan: Paracetamol
            Waktu masuk: Senin, 19 Okt 2026 • 09.30

            Mohon izin agar siswa tersebut dapat beristirahat di UKS. Terima kasih.

            Petugas UKS
            """.trimIndent(),
            pesan,
        )
    }

    @Test
    fun `pesan tanpa obat memakai tanda hubung`() {
        val pesan = PesanGenerator.buatPesan(
            nama = "Budi",
            kelas = "IX-E",
            keluhan = "Luka",
            tindakan = "Kompres",
            obat = null,
            waktuMasuk = waktuMasuk,
        )

        assertEquals(
            """
            *SURAT IZIN ISTIRAHAT - UKS*

            Kepada Yth. Wali Kelas / Guru Piket
            Kelas: IX-E

            Dengan hormat, siswa berikut sedang berada di UKS:
            Nama: Budi
            Kelas: IX-E
            Keluhan: Luka
            Tindakan: Kompres
            Obat diberikan: -
            Waktu masuk: Senin, 19 Okt 2026 • 09.30

            Mohon izin agar siswa tersebut dapat beristirahat di UKS. Terima kasih.

            Petugas UKS
            """.trimIndent(),
            pesan,
        )
    }

    @Test
    fun `nama obat kosong juga memakai tanda hubung`() {
        val pesan = PesanGenerator.buatPesan(
            nama = "Citra",
            kelas = "VIII-B",
            keluhan = "Mual",
            tindakan = "Istirahat",
            obat = "   ",
            waktuMasuk = waktuMasuk,
        )
        assertEquals(true, pesan.contains("Obat diberikan: ${PesanGenerator.OBAT_KOSONG}"))
    }

    @Test
    fun `varian pasien memakai data tersimpan`() {
        val pasien = Pasien(
            id = 7,
            namaSiswa = "Dewi",
            kelas = "VII-C",
            jenisKelamin = Constants.JENIS_KELAMIN_PEREMPUAN,
            keluhan = "Sakit Kepala",
            tindakan = "Istirahat, Diberi Obat",
            obatDiberikan = "Antasida",
            waktuMasuk = 0,
            waktuKeluar = null,
            status = Pasien.STATUS_AKTIF,
        )

        val pesan = PesanGenerator.buatPesan(pasien, waktuMasuk)

        assertEquals(true, pesan.contains("Nama: Dewi"))
        assertEquals(true, pesan.contains("Kelas: VII-C"))
        assertEquals(true, pesan.contains("Obat diberikan: Antasida"))
    }
}
