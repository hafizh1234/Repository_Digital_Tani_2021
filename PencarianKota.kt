package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import id.ac.ipb.mobile.digitani.adapter.HasilPencarianDaerahAdapter
import id.ac.ipb.mobile.digitani.api.ApiClientDedication
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentPencarianKotaBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.DaerahIndonesia
import id.ac.ipb.mobile.digitani.response.KecamatanResponse
import id.ac.ipb.mobile.digitani.response.KotaResponse
import id.ac.ipb.mobile.digitani.response.ProvinsiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PencarianKota : DialogFragment() {
    var hasilSemuaQueryDaerah: ArrayList<DaerahIndonesia>? = null
    private lateinit var sessionManager: SessionManager
    private var provinsiId: String? = null
    private var cityId: String? = null
    var daerahYangTerklik:DaerahIndonesia?=null
    private var _binding: FragmentPencarianKotaBinding? = null
    private var tingkatanDaerah: String? = null
    private lateinit var apiService: ApiInterface
    private val binding get() = _binding!!
    private lateinit var adapter: HasilPencarianDaerahAdapter
    companion object {
        var DaerahYangInginDicari = "daerah tercari"
        var KotaId = "kota id"
        var ProvinsiId = "provinsi id"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val fragment = parentFragment
        if (fragment is PengabdianPakar) {
            Toast.makeText(requireActivity(),"Membuka jendela pencarian",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasilSemuaQueryDaerah = arrayListOf()
        sessionManager = SessionManager(requireActivity())
        binding.rvHasilCari.layoutManager = LinearLayoutManager(requireActivity())
        tingkatanDaerah=""

        if (arguments != null) {
            tingkatanDaerah = requireArguments().getString(DaerahYangInginDicari)
            provinsiId= requireArguments().getString(ProvinsiId)
            cityId= requireArguments().getString(KotaId)
            adapter = HasilPencarianDaerahAdapter(requireActivity(),tingkatanDaerah!!)
            binding.rvHasilCari.adapter = adapter

            if (tingkatanDaerah == "provinsi") {
                cariProvinsi()
            }

            if (tingkatanDaerah == "kota") {
                if (provinsiId == null) {
                    cariKota(null)
                } else {
                    cariKota(provinsiId)
                }
            } else if (tingkatanDaerah == "kecamatan") {
                if (provinsiId == null) {
                    if (cityId == null) {
                        cariKecamatan(null, null)
                    } else {
                        cariKecamatan(null, cityId)
                    }
                } else {
                    if (cityId == null) {
                        cariKecamatan(provinsiId, null)
                    } else {
                        cariKecamatan(provinsiId, cityId)
                    }
                }
            }
        }
        adapter = HasilPencarianDaerahAdapter(requireActivity(),tingkatanDaerah!!)
        binding.rvHasilCari.adapter = adapter

        adapter.setOnItemClickCallback(object : HasilPencarianDaerahAdapter.OnItemClickCallback{

            override fun onItemClicked(daerahYangTerikat: DaerahIndonesia,tingkatanDaerah:String) {
                Toast.makeText(requireActivity(),"${daerahYangTerikat.name}",Toast.LENGTH_SHORT).show()
                when (tingkatanDaerah) {
                    "provinsi" -> {
                        daerahYangTerklik = DaerahIndonesia(daerahYangTerikat.id,daerahYangTerikat.id,null,null,daerahYangTerikat.name,daerahYangTerikat.created_at,daerahYangTerikat.updated_at)
                    }
                    "kota" -> {
                        daerahYangTerklik = DaerahIndonesia(daerahYangTerikat.id,daerahYangTerikat.province_id,null,daerahYangTerikat.id,daerahYangTerikat.name,daerahYangTerikat.created_at,daerahYangTerikat.updated_at)
                    }
                    "kecamatan" -> {
                        daerahYangTerklik=DaerahIndonesia(daerahYangTerikat.id,daerahYangTerikat.province_id,daerahYangTerikat.id,daerahYangTerikat.city_id,daerahYangTerikat.name,daerahYangTerikat.created_at,daerahYangTerikat.updated_at)
                    }
                }
                parentFragmentManager.setFragmentResult(PengabdianPakar.REQUEST_WILAYAH_TERPILIH,
                bundleOf(PengabdianPakar.HASIL_DAERAH_TERPILIH to daerahYangTerklik,PengabdianPakar.TINGKATAN_DAERAH to tingkatanDaerah))
                dialog?.dismiss()
            }

        })

    }

    private fun cariKecamatan(provinsi: String?, kota: String?) {
        //cari yang pertama
        // tidak tergantung oleh city dan kecakatan
        var searchId:String?=null
        if (provinsi == null) {
            if (kota == null) {
                searchId="11"
            } else {
                searchId="$kota"
            }
        } else {
            if (kota == null) {
                searchId="$provinsi"
            } else {
                searchId="$kota"
            }
        }

        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var semuaYangAdaDiKota:ArrayList<DaerahIndonesia> = arrayListOf()
        apiService = ApiClientDedication.addressApiClient
        apiService.getDistricts("Bearer $token", searchId)
            .enqueue(object : Callback<KecamatanResponse> {
                override fun onResponse(
                    call: Call<KecamatanResponse>,
                    response: Response<KecamatanResponse>
                ) {
                    if (response.isSuccessful) {
                        //Toast.makeText(requireActivity(),"berhasil mendapat provinsi ",Toast.LENGTH_SHORT).show()
                        hasilSemuaQueryDaerah = response.body()?.districts
                        //always load 34 provinsi
                        //Toast.makeText(requireActivity(),"$hasilSemuaQueryDaerah ",Toast.LENGTH_SHORT).show()

                        for(kecamatan in hasilSemuaQueryDaerah!!) {
                            var kecamatanId = kecamatan.id
                            var empatDigitAwalKecamatan =
                                "${kecamatanId!![0]}${kecamatanId[1]}${kecamatanId[2]}${kecamatanId[3]}"
                            var duaDigitAwal = "${kecamatanId[0]}${kecamatanId[1]}"
                            if (provinsi == null) {
                                if (kota != null && empatDigitAwalKecamatan == searchId) {
                                    semuaYangAdaDiKota.add(kecamatan)
                                }else{
                                    semuaYangAdaDiKota.add(kecamatan)
                                }
                            } else {
                                if (kota == null && searchId== duaDigitAwal) {
                                    semuaYangAdaDiKota.add(kecamatan)
                                } else {
                                    semuaYangAdaDiKota.add(kecamatan)
                                }
                            }
                        }
                        adapter.arrayListOfDaerahYangTerquery = semuaYangAdaDiKota
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<KecamatanResponse>, t: Throwable) {

                }

            })

    }

    private fun cariKota(provinsi: String?) {
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var searchId:String?=null
        if(provinsi!=null){
            searchId=provinsi
        }else{
            searchId="11"
        }
        //Toast.makeText(requireActivity(),"$searchId",Toast.LENGTH_SHORT).show()
        apiService = ApiClientDedication.addressApiClient
        apiService.getCity("Bearer $token", searchId)
            .enqueue(object : Callback<KotaResponse> {
                override fun onResponse(
                    call: Call<KotaResponse>,
                    response: Response<KotaResponse>
                ) {
                    if (response.isSuccessful) {
                        hasilSemuaQueryDaerah = response.body()?.cities
                        var semuaYangAdaDiProv:ArrayList<DaerahIndonesia> = arrayListOf()
                        for(kota in hasilSemuaQueryDaerah!!){
                            var provinsiId=kota.id
                            var duaDigitAwal="${provinsiId!![0]}${provinsiId[1]}"
                            if(provinsi!=null){
                                if(searchId==duaDigitAwal) {
                                    semuaYangAdaDiProv.add(kota)
                                }
                            }else{
                                if(searchId==duaDigitAwal) {
                                    semuaYangAdaDiProv.add(kota)
                                }
                            }
                        }
                        adapter.arrayListOfDaerahYangTerquery = semuaYangAdaDiProv
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<KotaResponse>, t: Throwable) {

                }

            })


    }

    private fun cariProvinsi() {
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientDedication.addressApiClient
        apiService.getProvinces("Bearer $token", "")
            .enqueue(object : Callback<ProvinsiResponse> {
                override fun onResponse(
                    call: Call<ProvinsiResponse>,
                    response: Response<ProvinsiResponse>
                ) {
                    if (response.isSuccessful) {
                        hasilSemuaQueryDaerah = response.body()?.provinces
                        adapter.arrayListOfDaerahYangTerquery = hasilSemuaQueryDaerah!!
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<ProvinsiResponse>, t: Throwable) {

                }

            })

    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPencarianKotaBinding.inflate(inflater)
        return binding.root
    }
}
