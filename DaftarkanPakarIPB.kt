package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import `in`.galaxyofandroid.spinerdialog.OnSpinerItemClick
import `in`.galaxyofandroid.spinerdialog.SpinnerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentDaftarkanPakarIPBBinding
import id.ac.ipb.mobile.digitani.model.UserRegister
import id.ac.ipb.mobile.digitani.response.UserRegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DaftarkanPakarIPB : Fragment() {
    private var _binding: FragmentDaftarkanPakarIPBBinding? = null
    private val binding get() = _binding!!

    var buttonRegis: Button? = null
    internal lateinit var apiService: ApiInterface
    private var spinnerDialog:SpinnerDialog?=null
    private lateinit var role: String
    private lateinit var categoryUser1:ArrayList<String>
    private lateinit var categoryUser: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDaftarkanPakarIPBBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding=null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryUser1= arrayListOf()
        categoryUser= arrayOf()
        role="Petani"
        categoryUser = resources.getStringArray(R.array.RolePakar)
        for(item in categoryUser){
            categoryUser1.add(item)
        }
        loadListUserKategori()
        binding.tvKategoriUser.setOnClickListener{
            spinnerDialog?.showSpinerDialog()
        }
        buttonRegis = binding.register
        buttonRegis!!.setOnClickListener {
            cek()
        }
    }

    private fun loadListUserKategori() {
        spinnerDialog = SpinnerDialog(
            requireActivity(),
            categoryUser1,
            "Pilih Kategori User Sesuai Status Pakar",
            R.style.DialogAnimations_SmileWindow,
            "Tutup"
        )
        spinnerDialog!!.bindOnSpinerListener(object : OnSpinerItemClick {
            override fun onClick(item: String, position: Int) {

                for (x in categoryUser.indices) {
                    if (item == categoryUser[x]) {
                        role = when (x) {
                            0->"ROLE:DOSEN_IPB"
                            1->"ROLE:FAKULTAS_DAN_DEPARTEMEN"
                            2->"ROLE:PUSAT_STUDI_PENELITIAN"

                            else -> "ROLE:DOSEN_IPB"
                        }
                        binding.tvKategoriUser.setText(categoryUser[x])
                        //Toast.makeText(requireActivity(), role, Toast.LENGTH_SHORT).show()
                        break
                    }else if (item == categoryUser[categoryUser.size-1]) {
                        role="ROLE:DOSEN_IPB"
                        binding.tvKategoriUser.text = "Pakar dan Dosen IPB"
                        //Toast.makeText(requireActivity(), role, Toast.LENGTH_SHORT).show()
                    }
                }

                //Toast.makeText(requireActivity(), role, Toast.LENGTH_SHORT).show()

            }
        })

    }
    /*   "role":
       {
           "type": "string",
           "enum": [
           "ROLE:PETANI",
           "ROLE:PENYULUH_PERTANIAN",
           "ROLE:DOSEN_MITRA_PTN_LAIN",
           "ROLE:MAHASISWA",
           "ROLE:DOSEN_IPB",
           "ROLE:PUSAT_STUDI_PENELITIAN",
           "ROLE:FAKULTAS_DAN_DEPARTEMEN",
           "ROLE:SUB_ADMIN",
           "ROLE:SUPER_ADMIN"
           ]

           */
    fun cek() {
        buttonRegis?.visibility = View.INVISIBLE
        val invalid = validate()
        if (!invalid) {
            register()
        } else {
            buttonRegis?.visibility = View.VISIBLE
        }

    }


    private fun register() {
        apiService = ApiClient.userApiClientLogin
        binding.progressbarRegistration.visibility = View.VISIBLE
        val nama = binding.nama.text.toString()
        val email = binding.email.text.toString()
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()
        val konfirmasiPassword = binding.konfirmPass.text.toString()
        val userRegister: UserRegister?

        userRegister = UserRegister(username, nama, password, konfirmasiPassword, email, role, true)

        val ucall: Call<UserRegisterResponse> = apiService.postRegister(userRegister)
        ucall.enqueue(object : Callback<UserRegisterResponse> {
            override fun onResponse(
                call: Call<UserRegisterResponse>,
                userResponse: Response<UserRegisterResponse>
            ) {
                if (userResponse.isSuccessful) {
                    binding.progressbarRegistration.visibility=View.GONE
                    buttonRegis?.visibility = View.INVISIBLE
                    //ganti ke langsung masuk dengan login lalu langsung buat chat room
                    Toast.makeText(requireActivity(),"Pendaftaran akun baru untuk pakar IPB berhasil. Akun dapat langsung digunakan.",Toast.LENGTH_LONG).show()
                    var fragment=MenuLainnyaFragment()
                    var transaction=requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment,fragment)
                    transaction.commit()
                    this@DaftarkanPakarIPB.onDestroy()

                } else {
                    binding.progressbarRegistration.visibility=View.GONE
                    buttonRegis?.visibility = View.VISIBLE
                    if (userResponse.code() == 422) {
                        Toast.makeText(
                            requireActivity(),
                            "username atau email telah dipakai. Silahkan ubah.",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "Maaf server sedang sibuk atau dalam perbaikan.\n Silahkan coba lagi beberapa saat",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<UserRegisterResponse>, t: Throwable) {
                buttonRegis?.visibility = View.VISIBLE
                Toast.makeText(
                    requireActivity(),
                    "Mohon maaf sedang terjadi gangguan",
                    Toast.LENGTH_SHORT
                ).show()
            }


        })
    }

    fun validate(): Boolean {
        var invalid = false
        var focusView: View? = null

        var cekError = 0

        binding.email.setError(null)
        binding.password.setError(null)
        binding.konfirmPass.setError(null)
        binding.username.setError(null)
        binding.nama.setError(null)

        val uemail = binding.email.text.toString()
        val upassword = binding.password.text.toString()
        val upasskonf = binding.konfirmPass.text.toString()
        val uusername = binding.username.text.toString()
        val unama = binding.nama.text.toString()

        if (cekError == 0) {
            if (unama.isEmpty()) {
                binding.nama.setError("Nama tidak boleh kosong")
                focusView = binding.nama
                invalid = true
            } else {
                binding.nama.setError(null)
                cekError = 1
            }
        }
        if (cekError == 1) {
            if (uemail.isEmpty()) {
                binding.email.setError("Email tidak boleh kosong")
                focusView = binding.email
                invalid = true
            } else if (!isEmailValid(uemail)) {
                binding.email.setError("Email tidak valid")
                focusView = binding.email
                invalid = true
            } else {
                binding.email.setError(null)
                cekError = 2
            }
        }
        if (cekError == 2) {
            if (uusername.isEmpty()) {
                binding.username.setError("Username tidak boleh kosong")
                focusView = binding.username
                invalid = true
            } else {
                binding.username.setError(null)
                cekError = 3
            }
        }
        if (cekError == 3) {
            if (upassword.isEmpty()) {
                binding.password.setError("Password tidak boleh kosong")
                focusView = binding.password
                invalid = true
            } else if (upassword.length < 8) {
                binding.password.setError("Password tidak boleh kurang dari 8 karakter")
                focusView = binding.password
                invalid = true
            } else {
                binding.password.setError(null)
                cekError = 4
            }
        }
        if (cekError == 4) {
            if (upasskonf.isEmpty()) {
                binding.konfirmPass.setError("Konfirmasi Password tidak boleh kosong")
                focusView = binding.konfirmPass
                invalid = true
            } else {
                binding.konfirmPass.setError(null)
                cekError = 5
            }
        }
        if (invalid) {
            focusView!!.requestFocus()
        }
        binding.progressbarRegistration.visibility = View.INVISIBLE
        return invalid

    }

    private fun isEmailValid(email: String): Boolean {

        return email.contains("@")
    }
}
