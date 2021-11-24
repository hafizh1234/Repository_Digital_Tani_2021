package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.util.Preconditions
import androidx.fragment.app.Fragment
import id.ac.ipb.mobile.digitani.api.ApiClientDedication
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentPengabdianPakarBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.DaerahIndonesia
import id.ac.ipb.mobile.digitani.response.KotaResponse
import id.ac.ipb.mobile.digitani.response.PengabdianResponse
import id.ac.ipb.mobile.digitani.response.ProvinsiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PengabdianPakar : Fragment() {
    private lateinit var sessionManager: SessionManager
    private var penyelesaianYangDikasih: String? = null
    private var idEachWithNameKeterangan: String? = null
    private var namaTopik: String? = null
    private var isTerisiIsiPengabdian = false
    private var kotaDicariDuluan: Boolean = false
    private var kecamatanDicariDuluan: Boolean = false
    private var kecamatanDicariSebelumKota: Boolean = false
    private var namaProvinsiTerpilih: String? = null
    private var namaKotaTerpilih: String? = null
    private var namaKecamatanTerpilih: String? = null
    private var provinsiId: String? = null
    private var kotaId: String? = null
    private var kecamatanId: String? = null
    lateinit var apiService: ApiInterface

    companion object {
        var TINGKATAN_DAERAH = "tingkatan"
        var REQUEST_WILAYAH_TERPILIH = "100"
        var HASIL_DAERAH_TERPILIH = "Daerah"
    }

    private var _binding: FragmentPengabdianPakarBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengabdianPakarBinding.inflate(inflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireActivity())
        var detailUser = sessionManager.userDetails()
        childFragmentManager.setFragmentResultListener(
            REQUEST_WILAYAH_TERPILIH,
            this,
            { REQUEST_WILAYAH_TERPILIH, result: Bundle ->
                run {
                    onFragmentResult(REQUEST_WILAYAH_TERPILIH, result)
                }
            })
        binding.etTombolPencarianKecamatan.setOnClickListener {
            val mPencarianKota = PencarianKota()

            val mBundle = Bundle()
            if (provinsiId != null) {
                if (kotaId != null) {
                    mBundle.putString(PencarianKota.ProvinsiId, provinsiId)
                    mBundle.putString(PencarianKota.KotaId, kotaId)
                } else {
                    mBundle.putString(PencarianKota.ProvinsiId, provinsiId)
                    kecamatanDicariSebelumKota = true
                }
            } else {
                if (kotaId != null) {
                    mBundle.putString(PencarianKota.KotaId, kotaId)
                } else {
                    kecamatanDicariDuluan = true
                }
            }

            mBundle.putString(PencarianKota.DaerahYangInginDicari, "kecamatan")

            mPencarianKota.arguments = mBundle

            val mFragmentManager = childFragmentManager.beginTransaction().apply {
                attach(mPencarianKota)
                addToBackStack(null)
            }
            mPencarianKota.show(
                mFragmentManager,
                PencarianKota::class.java.simpleName
            )
        }
        binding.etTombolPencarianKota.setOnClickListener {
            val mPencarianKota = PencarianKota()
            val mBundle = Bundle()
            if (provinsiId != null) {
                mBundle.putString(PencarianKota.ProvinsiId, provinsiId)
            } else {
                kotaDicariDuluan = true
            }
            mBundle.putString(PencarianKota.DaerahYangInginDicari, "kota")

            mPencarianKota.arguments = mBundle

            val mFragmentManager = childFragmentManager.beginTransaction().apply {
                attach(mPencarianKota)
                addToBackStack(null)
            }
            mPencarianKota.show(
                mFragmentManager,
                PencarianKota::class.java.simpleName
            )
        }

        binding.etTombolPencarianProvinsi.setOnClickListener {
            val mPencarianKota = PencarianKota()

            val mBundle = Bundle()
            mBundle.putString(PencarianKota.DaerahYangInginDicari, "provinsi")

            mPencarianKota.arguments = mBundle

            val mFragmentManager = childFragmentManager.beginTransaction().apply {
                attach(mPencarianKota)
                addToBackStack(null)
            }
            mPencarianKota.show(
                mFragmentManager,
                PencarianKota::class.java.simpleName
            )
        }
        binding.catatPengabdian.setOnClickListener {
            isTerisiIsiPengabdian = cekIsiContent()
            if (isTerisiIsiPengabdian) {
                penyelesaianYangDikasih = binding.etPenyelesaian.text.toString()
                namaTopik = binding.etTopikPermasalahan.text.toString()
                idEachWithNameKeterangan = if (provinsiId != null) {
                    "$provinsiId:$namaProvinsiTerpilih,"
                } else {
                    "kosong,"
                }
                idEachWithNameKeterangan += if (kotaId != null) {
                    "$kotaId:$namaKotaTerpilih,"
                } else {
                    "kosong,"
                }
                idEachWithNameKeterangan += if (kecamatanId != null) {
                    "$kecamatanId:$namaKecamatanTerpilih"
                } else {
                    "kosong"
                }
                binding.progressbarCatatPengabdian.visibility=View.VISIBLE
                buatPengabdian(namaTopik!!, penyelesaianYangDikasih!!, idEachWithNameKeterangan!!)
            }else{
                binding.etPenyelesaian.text = null
                binding.etTombolPencarianKecamatan.text = null
                binding.etTombolPencarianKota.text = null
                binding.etTombolPencarianProvinsi.text = null
                binding.etTopikPermasalahan.text = null
                namaTopik = null
                penyelesaianYangDikasih = null
                idEachWithNameKeterangan = null
            }
        }
    }

    private fun buatPengabdian(title: String, content: String, cityId: String) {
        var hasilPost: PengabdianResponse
        var type: String = "FORM_DEDICATION:EXPERTISE"
        var detailUser = sessionManager.userDetails()
        var userId = detailUser[SessionManager.KEY_ID_USER].toString()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientDedication.dedicationApiClient
        apiService.postPengabdian("Bearer $token", title, content, cityId, userId, type)
            .enqueue(object : Callback<PengabdianResponse> {
                override fun onResponse(
                    call: Call<PengabdianResponse>,
                    response: Response<PengabdianResponse>
                ) {
                    if (response.isSuccessful) {
                        hasilPost = response.body()!!
                        Toast.makeText(
                            requireActivity(), "Berhasil membuat catatan pengabdian",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.etPenyelesaian.text = null
                        binding.etTombolPencarianKecamatan.text = null
                        binding.etTombolPencarianKota.text = null
                        binding.etTombolPencarianProvinsi.text = null
                        binding.etTopikPermasalahan.text = null
                        namaTopik = null
                        penyelesaianYangDikasih = null
                        idEachWithNameKeterangan = null
                        binding.progressbarCatatPengabdian.visibility=View.GONE

                    }
                }

                override fun onFailure(call: Call<PengabdianResponse>, t: Throwable) {
                    binding.progressbarCatatPengabdian.visibility=View.GONE
                    Toast.makeText(
                        requireActivity(),
                        "Gagal membuat Pengabdian",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun cekIsiContent(): Boolean {
        var isTerisi = true
        if (!binding.etTopikPermasalahan.text?.isEmpty()!!) {
            namaTopik = binding.etTopikPermasalahan.text.toString()
            if (binding.etTopikPermasalahan.text.toString().length > 255) {
                isTerisi = false
                binding.etTopikPermasalahan.setError("Topik Permasalahan tidak boleh lebih dari 255 karakter.")
            }
        } else {
            isTerisi = false
            binding.etTopikPermasalahan.setError("Silahkan isi terlebih dahulu topik permasalahan yang di alami petani.")
        }

        if (!binding.etPenyelesaian.text?.isEmpty()!!) {
            penyelesaianYangDikasih = binding.etPenyelesaian.text.toString()
        } else {
            isTerisi = false
            binding.etPenyelesaian.setError("Silahkan isi penyelesaian terhadap permasalahan petani yang sudah anda lakukan")
        }

        return isTerisi
    }

    @SuppressLint("RestrictedApi")
    private fun onFragmentResult(requestWilayahTerpilih: String, result: Bundle) {
        Preconditions.checkState(REQUEST_WILAYAH_TERPILIH == requestWilayahTerpilih)

        val hasil = result.getParcelable<DaerahIndonesia>(HASIL_DAERAH_TERPILIH)!!
        val tingkatanDaerah = result.getString(TINGKATAN_DAERAH)
        if (tingkatanDaerah == "provinsi") {
            binding.etTombolPencarianProvinsi.text = hasil.name
            provinsiId = hasil.province_id
            namaProvinsiTerpilih = hasil.name
   
            if (kotaId != null) {
                var duaDigitAwalKotaId: String? = "${kotaId!![0]}${kotaId!![1]}"
                if (duaDigitAwalKotaId != provinsiId) {
                    kotaId = null
                    namaKotaTerpilih = null
                    binding.etTombolPencarianKota.text = ""
                    binding.etTombolPencarianKota.error =
                        "Kota tidak ada di provinsi yang dipilih"
                }
                if (kecamatanId != null) {
                    var duaDigitAwalKecamatanId: String? =
                        "${kecamatanId!![0]}${kecamatanId!![1]}"
                    if (duaDigitAwalKecamatanId != provinsiId) {
                        kecamatanId = null
                        namaKecamatanTerpilih = null
                        binding.etTombolPencarianKecamatan.text = ""
                        binding.etTombolPencarianKecamatan.error =
                            "Kecamatan tidak ada di provinsi yang dipilih"
                    }
                }
            } else {
                if (kecamatanId != null) {
                    var duaDigitAwalKecamatanId: String? =
                        "${kecamatanId!![0]}${kecamatanId!![1]}"
                    if (duaDigitAwalKecamatanId != provinsiId) {
                        kecamatanId = null
                        namaKecamatanTerpilih = null
                        binding.etTombolPencarianKecamatan.text = ""
                        binding.etTombolPencarianKecamatan.error =
                            "Kecamatan tidak ada di provinsi yang dipilih"
                    }
                }
            }
        } else if (tingkatanDaerah == "kota") {
            binding.etTombolPencarianKota.text = hasil.name
            kotaId = hasil.city_id
            namaKotaTerpilih = hasil.name
            if (kotaDicariDuluan) {
                getProvince(kotaId, "kota")
            }
            if (kecamatanId != null) {
                var empatDigitAwalKecamatanId: String? =
                    "${kecamatanId!![0]}${kecamatanId!![1]}${kecamatanId!![2]}${kecamatanId!![3]}"
                if (empatDigitAwalKecamatanId != kotaId) {
                    kecamatanId = null
                    namaKecamatanTerpilih = null
                    binding.etTombolPencarianKecamatan.text = ""
                    binding.etTombolPencarianKecamatan.error =
                        "Kecamatan tidak ada di provinsi yang dipilih"
                }
            }
        } else if (tingkatanDaerah == "kecamatan") {
            binding.etTombolPencarianKecamatan.text = hasil.name
            kecamatanId = hasil.kecamatan_id
            namaKecamatanTerpilih = hasil.name
            if (kecamatanDicariDuluan) {
                getProvince(hasil.city_id, tingkatanDaerah)
            } else if (kecamatanDicariSebelumKota) {
                hasil.city_id?.let { getKota(it) }
            }
        }
    }

    private fun getProvince(kotaId: String?, tingkatanDaerah: String) {
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var hasilSemuaQueryDaerah: ArrayList<DaerahIndonesia>
        apiService = ApiClientDedication.addressApiClient
        if (kotaId != null) {
            apiService.getProvinces("Bearer $token", "${kotaId[0]}${kotaId[1]}")
                .enqueue(object : Callback<ProvinsiResponse> {
                    override fun onResponse(
                        call: Call<ProvinsiResponse>,
                        response: Response<ProvinsiResponse>
                    ) {
                        if (response.isSuccessful) {
   
                            hasilSemuaQueryDaerah = response.body()?.provinces!!
                            binding.etTombolPencarianProvinsi.text = hasilSemuaQueryDaerah[0].name
                            provinsiId = hasilSemuaQueryDaerah[0].id
                            namaProvinsiTerpilih = hasilSemuaQueryDaerah[0].name
                            if (tingkatanDaerah != "kota") {
                                getKota(kotaId)
                            }
                        }
                    }

                    override fun onFailure(call: Call<ProvinsiResponse>, t: Throwable) {

                    }

                })
        }


    }

    private fun getKota(kotaIdTerpilih: String) {
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var hasilSemuaQueryDaerah: ArrayList<DaerahIndonesia> = arrayListOf()
        apiService = ApiClientDedication.addressApiClient
        apiService.getCity("Bearer $token", kotaIdTerpilih)
            .enqueue(object : Callback<KotaResponse> {
                override fun onResponse(
                    call: Call<KotaResponse>,
                    response: Response<KotaResponse>
                ) {
                    if (response.isSuccessful) {
                        hasilSemuaQueryDaerah = response.body()?.cities!!
                        kotaId = hasilSemuaQueryDaerah[0].id
                        namaKotaTerpilih = hasilSemuaQueryDaerah[0].name
                        binding.etTombolPencarianKota.text = hasilSemuaQueryDaerah[0].name

                    }
                }

                override fun onFailure(call: Call<KotaResponse>, t: Throwable) {

                }

            })


    }
}
