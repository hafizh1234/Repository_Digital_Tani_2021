package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.adapter.PengabdianLogAdapter
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiClientDedication
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentLogPengabdianUntukAdminBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.Pengabdian
import id.ac.ipb.mobile.digitani.model.User
import id.ac.ipb.mobile.digitani.response.PengabdiansResponse
import id.ac.ipb.mobile.digitani.response.UserLoginData
import id.ac.ipb.mobile.digitani.response.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.CoroutineContext

class LogPengabdianUntukAdmin : Fragment() {
    private lateinit var rvPengabdian: RecyclerView
    private lateinit var adapter: PengabdianLogAdapter
    private lateinit var apiService: ApiInterface
    private lateinit var hasilAllPengabdianPakar: ArrayList<Pengabdian>
    private lateinit var sessionManager: SessionManager
    private lateinit var hasilPengabdian: ArrayList<Pengabdian>
    private var _binding: FragmentLogPengabdianUntukAdminBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPengabdian = binding.rvPengabdian
        hasilAllPengabdianPakar = arrayListOf()
        hasilPengabdian = arrayListOf()
        rvPengabdian.layoutManager = LinearLayoutManager(requireActivity())
        adapter = PengabdianLogAdapter(requireActivity(), "")
        rvPengabdian.adapter = adapter
        sessionManager = SessionManager(requireActivity())
        job = Job()
        binding.progressbarAdminPengabdian.visibility=View.VISIBLE
        var detailUser = sessionManager.userDetails()
        adapter.setOnItemClickCallback(object : PengabdianLogAdapter.OnItemClickCallback {
            override fun onItemClicked(pengabdian: Pengabdian, user: String) {

                val mDetail = DetailLogPengabdian()

                val mBundle = Bundle()
                mBundle.putParcelable(DetailLogPengabdian.PENGABDIAN, pengabdian)
                mBundle.putString(DetailLogPengabdian.USER, user)

                mDetail.arguments = mBundle

                val mFragmentManager = childFragmentManager.beginTransaction().apply {
                    attach(mDetail)
                    addToBackStack(null)
                }
                mDetail.show(
                    mFragmentManager,
                    DetailLogPengabdian::class.java.simpleName
                )
            }
        })

        getAllPengabdian()

    }

    private fun getAllPengabdian() {
        var detailUser = sessionManager.userDetails()
        //var idSekarang = detailUser[SessionManager.KEY_ID_USER] as Int
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientDedication.dedicationApiClient
        apiService.getAllPengabdian("Bearer $token")
            .enqueue(object : Callback<PengabdiansResponse> {
                override fun onResponse(
                    call: Call<PengabdiansResponse>,
                    response: Response<PengabdiansResponse>
                ) {
                    if (response.isSuccessful) {
                        hasilAllPengabdianPakar = response.body()!!.form_dedications
                        if (hasilAllPengabdianPakar.isNotEmpty()) {
                            //Toast.makeText(requireActivity(),"$hasilAllPengabdianPakar",Toast.LENGTH_LONG).show()
                            getAllUser()
                        }
                    }

                }

                override fun onFailure(call: Call<PengabdiansResponse>, t: Throwable) {

                }

            })
    }

    private fun getAllUser() {
        var userDetail = sessionManager.userDetails()
        val token = userDetail[SessionManager.KEY_TOKEN_JWT].toString()
        val idCurrent = userDetail[SessionManager.KEY_ID_USER].toString()
        apiService = ApiClient.userApiClientGetAllUsers
        apiService.getUserAll("Bearer $token")
            .enqueue(object : Callback<Users> {
                override fun onResponse(
                    call: Call<Users>,
                    response: Response<Users>
                ) {
                    if (response.isSuccessful) {
                        var users: ArrayList<UserLoginData> = response.body()!!.users
                        var hasil: ArrayList<User> = arrayListOf()
                        for (user in users) {
                            var kategori = ""
                            if (user.roles!!.isNotEmpty()) {
                                kategori = user.roles!![0].name.toString()
                            } else {
                                kategori = "Peran Not Set Yet"
                            }
                            var kategoriUser = ""
                            val stringRole = resources.getStringArray(R.array.RoleAll)
                            kategoriUser =
                                when (kategori) {
                                    "ROLE:PETANI" -> stringRole[0]
                                    "ROLE:PENYULUH_PERTANIAN" -> stringRole[1]
                                    "ROLE:MAHASISWA" -> stringRole[2]
                                    "ROLE:DOSEN_MITRA_PTN_LAIN" -> stringRole[3]
                                    "ROLE:DOSEN_IPB" -> stringRole[4]
                                    "ROLE:PUSAT_STUDI_PENELITIAN" -> stringRole[6]
                                    "ROLE:FAKULTAS_DAN_DEPARTEMEN" -> stringRole[5]
                                    "ROLE:SUB_ADMIN" -> stringRole[7]
                                    "ROLE:SUPER_ADMIN" -> stringRole[8]
                                    else -> "Peran Belum Diatur"
                                }
                            var userYangTerpilih: User = User(
                                user.id, user.username, user.email,
                                kategoriUser, user.name
                            )

                            hasil.add(userYangTerpilih)

                        }
                        adapter.user = hasil
                        binding.progressbarAdminPengabdian.visibility=View.GONE
                        adapter.logPengabdian = hasilAllPengabdianPakar
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {
                    binding.progressbarAdminPengabdian.visibility=View.GONE
                }
            })

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLogPengabdianUntukAdminBinding.inflate(inflater)
        return binding.root
    }
}
