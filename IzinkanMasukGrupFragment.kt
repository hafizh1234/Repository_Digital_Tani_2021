package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.adapter.AdapterPersetujuanGrup
import id.ac.ipb.mobile.digitani.api.ApiClient
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentIzinkanMasukGrupBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.Delete
import id.ac.ipb.mobile.digitani.model.RuangChat
import id.ac.ipb.mobile.digitani.model.User
import id.ac.ipb.mobile.digitani.model.UserCategory
import id.ac.ipb.mobile.digitani.response.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IzinkanMasukGrupFragment : Fragment() {
    private lateinit var memberOfChatRoom: MutableMap<String, ArrayList<Int>>
    private lateinit var sessionManager: SessionManager
    private lateinit var grupYangDiinginkanMasuk: ArrayList<RuangChat>
    private lateinit var apiService: ApiInterface
    private lateinit var usersAll: MutableMap<String, ArrayList<String>>
    private lateinit var adapter: AdapterPersetujuanGrup
    private lateinit var userYangPantas: ArrayList<User>
    private lateinit var idAllUser: ArrayList<Int>
    private var _binding: FragmentIzinkanMasukGrupBinding? = null
    private lateinit var allMembersNotApproved: ArrayList<ChatRoomMemberData>
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIzinkanMasukGrupBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        memberOfChatRoom = HashMap()
        sessionManager = SessionManager(requireActivity())
        usersAll = HashMap()
        userYangPantas = arrayListOf()
        idAllUser = arrayListOf()
        grupYangDiinginkanMasuk = arrayListOf()
        binding.progressbarIzinkan.visibility = View.VISIBLE
        allMembersNotApproved = arrayListOf()
        adapter = AdapterPersetujuanGrup(requireActivity())
        binding.rvAllPerizinan.layoutManager = LinearLayoutManager(requireActivity())
        binding.rvAllPerizinan.adapter = adapter
        getAllMembers()
        adapter.setOnItemClickCallback(object : AdapterPersetujuanGrup.OnItemClickCallback {
            override fun onItemClicked(member: ChatRoomMemberData) {
                showDialog(member, "terimaPerizinan")
            }

            override fun onTolakPerizinan(member: ChatRoomMemberData) {
                showDialog(member, "tolakPerizinan")
            }

        })
        //izinkan masuk grup caranya ambil setiap member of group chat yang termasuk ke dalam room_chat:group
        //lalu tampilkan semua orang yang belum approved lalu izinkan
    }

    private fun getAllMembers() {
        //mengecek semua member serta cari apakah dia approved atau tidak
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        val apiInterface = ApiClientChatNew.chatRoomApiClient
        var chatMembers: ArrayList<ChatRoomMemberData>?
        usersAll.clear()
        userYangPantas.clear()
        idAllUser.clear()
        grupYangDiinginkanMasuk.clear()
        allMembersNotApproved.clear()
        apiInterface.getAllMembers("Bearer $token")
            .enqueue(object : Callback<ChatRoomMemberResponse> {
                override fun onResponse(
                    call: Call<ChatRoomMemberResponse>,
                    response: Response<ChatRoomMemberResponse>
                ) {
                    if (response.isSuccessful) {
                        chatMembers = response.body()?.chat_members
                        //sudah dapat response, terima setiap chat room id dengan isi berupa jumlah orang dalam grup
                        //serta terima id chat group yang user yang sedang login adalah admin di dalamny
                        if (chatMembers != null) {
                            if (chatMembers!!.isEmpty()) {
                                //langsung buat group chat private
                                Toast.makeText(
                                    requireActivity(),
                                    "Belum ada member sama sekali",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //makeRoomChatPrivate()
                            } else {
                                for (chatRoomMember in chatMembers!!) {
                                    //identifikasi semua member di chat room
                                    when {
                                        memberOfChatRoom.isEmpty() -> {
                                            memberOfChatRoom[chatRoomMember.chat_room_id.toString()] =
                                                arrayListOf(chatRoomMember.user_id)
                                        }
                                        memberOfChatRoom.containsKey(chatRoomMember.chat_room_id.toString()) -> {
                                            var isiRuangChat =
                                                memberOfChatRoom[chatRoomMember.chat_room_id.toString()] as ArrayList<Int>
                                            isiRuangChat.add(chatRoomMember.user_id)
                                            memberOfChatRoom[chatRoomMember.chat_room_id.toString()] =
                                                isiRuangChat
                                        }
                                        else -> {
                                            memberOfChatRoom[chatRoomMember.chat_room_id.toString()] =
                                                arrayListOf(chatRoomMember.user_id)
                                        }
                                    }

                                    //cari semua member yang tidak approved dalam group_chat
                                    if (!chatRoomMember.is_approved) {
                                        allMembersNotApproved.add(chatRoomMember)
                                    }
                                }
                                getAllUser()

                            }
                        } else {
                            //makeRoomChatPrivate()
                            Toast.makeText(
                                requireActivity(),
                                "tidak ada member grup sama sekali",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }

                override fun onFailure(call: Call<ChatRoomMemberResponse>, t: Throwable) {
                    Toast.makeText(
                        requireActivity(),
                        "Gagal mengambil data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun getAllRoomsGroup() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var memberYangPantas: ArrayList<ChatRoomMemberData> = arrayListOf()

        var ruangChats: ArrayList<RuangChat>?
        var ruangChatPrivates: ArrayList<RuangChat>? = arrayListOf()
        grupYangDiinginkanMasuk = arrayListOf()
        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.getRooms("Bearer $token")
            .enqueue(object : Callback<ResponseChatRoom> {
                override fun onResponse(
                    call: Call<ResponseChatRoom>,
                    response: Response<ResponseChatRoom>
                ) {

                    if (response.isSuccessful) {
                        //cek terlebih dahulu apakah sudah tergabung ke dalam ruang chat pribadi, grup, dua duanya , atau nggak
                        //pisahkan dulu tergabung ke chat room nggak?
                        ruangChats = response.body()?.chat_rooms

                        if (ruangChats != null) {
                            var isiRoom: ArrayList<Int> = arrayListOf()
                            if (ruangChats!!.isEmpty()) {//anggapan setiap grup chat sudah ada satu member
                                //makeRoomChatPrivate()
                                Toast.makeText(
                                    requireActivity(),
                                    "belum ada group yang ditambahkan oleh admin",
                                    Toast.LENGTH_SHORT
                                ).show()
                                //getAllMembers()
                            } else {
                                for (item in ruangChats!!) {
                                    //harus dicari setiap user apakah punya ruang yang sama dengan user sekarang
                                    //jika dia not approved dan dia adalah ruangchat group
                                    if (item.type == "CHAT_ROOM:GROUP") {
                                        memberYangPantas.addAll(allMembersNotApproved.filter { it.chat_room_id == item.id })
                                        grupYangDiinginkanMasuk.add(item)
                                    }
                                }
                            }
                            binding.progressbarIzinkan.visibility = View.GONE
                            adapter.listMember = memberYangPantas
                            adapter.listUser = userYangPantas
                            adapter.listRuangChat = grupYangDiinginkanMasuk
                            adapter.notifyDataSetChanged()
                        }

                    }
                }


                override fun onFailure(call: Call<ResponseChatRoom>, t: Throwable) {
                    Toast.makeText(requireActivity(), "${t.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    //ini untuk tanya admin beneran nggak mau masukin dia, kalau ditolak nanti tanya lagi tapi kalau ditolak nanti dia dikeluarkan dari room
    //member sementara cardview untuk liat detail deskripsi keterangan user yang izin masuk grup
    private fun showDialog(chatRoomMemberData: ChatRoomMemberData, penjelasan: String) {
        var dialogTitle = ""
        var dialogMessage = ""
        var userId = chatRoomMemberData.user_id
        var index = userYangPantas.firstOrNull { it.id == userId }
        var name = index!!.name
        var kategoriUser = index.kategoriUser
        var grup = grupYangDiinginkanMasuk.firstOrNull { it.id == chatRoomMemberData.chat_room_id }
        var namaGrup = grup!!.name
        if (penjelasan == "terimaPerizinan") {
            dialogMessage =
                "Apakah anda ingin mengizinkan user $name ($kategoriUser) untuk masuk grup ${namaGrup}?"
            dialogTitle = "Perizinan Masuk Grup "
        } else if (penjelasan == "tolakPerizinan") {
            dialogMessage =
                "Apakah anda ingin menolak user $name ($kategoriUser) untuk masuk grup ${namaGrup}?"
            dialogTitle = "Penolakan Izin Masuk Grup "
        }
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                if (penjelasan == "terimaPerizinan") {
                    binding.progressbarIzinkan.visibility = View.VISIBLE

                    izinKanMasuk(chatRoomMemberData)

                } else if (penjelasan == "tolakPerizinan") {
                    binding.progressbarIzinkan.visibility = View.VISIBLE

                    tolakPerizinan(chatRoomMemberData)

                }
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }

    private fun tolakPerizinan(chatRoomMemberData: ChatRoomMemberData) {
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteMember("Bearer $token", chatRoomMemberData.id.toString())
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    var user = userYangPantas.firstOrNull { it.id == chatRoomMemberData.user_id }
                    var group =
                        grupYangDiinginkanMasuk.firstOrNull { it.id == chatRoomMemberData.user_id }
                    if (group != null && user != null) {
                        Toast.makeText(
                            requireActivity(),
                            "Berhasil menolak ${user.name} (${user.kategoriUser}) untuk masuk ${group.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "Berhasil menolak user",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    getAllMembers()
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    binding.progressbarIzinkan.visibility = View.GONE
                }

            })
    }

    private fun izinKanMasuk(chatRoomMemberData: ChatRoomMemberData) {
        var detail = sessionManager.userDetails()
        var token = detail[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.updateMember(
            "Bearer $token",
            chatRoomMemberData.id,
            chatRoomMemberData.user_id.toString(),
            chatRoomMemberData.chat_room_id.toString(),
            "true",
            "CHAT_MEMBER:REGULAR"
        )
            .enqueue(object : Callback<ChatMemberPostResponse> {
                override fun onResponse(
                    call: Call<ChatMemberPostResponse>,
                    response: Response<ChatMemberPostResponse>
                ) {

                    Toast.makeText(
                        requireActivity(),
                        "User sukses diizinkan masuk.",
                        Toast.LENGTH_SHORT
                    ).show()

                    getAllMembers()
                }

                override fun onFailure(call: Call<ChatMemberPostResponse>, t: Throwable) {
                    binding.progressbarIzinkan.visibility = View.GONE
                    Toast.makeText(
                        requireActivity(),
                        "Tidak berhasil menambahkan sebagai teman.\nCoba lagi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    private fun getAllUser() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var users: ArrayList<UserLoginData>
        var kategori: UserCategory? = null
        var peran: String = ""
        var user: User? = null
        userYangPantas.clear()
        var arrayListUser: ArrayList<User> = arrayListOf()

        apiService = ApiClient.userApiClientGetAllUsers
        apiService.getUserAll("Bearer $token")
            .enqueue(object : Callback<Users> {
                override fun onResponse(call: Call<Users>, response: Response<Users>) {
                    if (response.isSuccessful) {
                        users = response.body()?.users!!

                        if (users.isEmpty()) {
                            Toast.makeText(requireActivity(), "Tidak ada user", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            for (item in users) {
                                if (!item.roles?.isEmpty()!!) {
                                    kategori = item.roles!![0]
                                    peran = kategori!!.name.toString()
                                } else {
                                    peran = "Peran Not Set Yet"
                                }
                                var kategoriUser = ""
                                val stringRole = resources.getStringArray(R.array.RoleAll)
                                kategoriUser =
                                    when (peran) {
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
                                if (allMembersNotApproved.firstOrNull { it.user_id == item.id } != null) {
                                    //ambil user yang merupakan member not approved di grup untuk diambil nama sama kategorinya saja
                                    user = User(
                                        item.id,
                                        item.username,
                                        item.email,
                                        kategoriUser,
                                        item.name
                                    )
                                    var userSudahAda =
                                        userYangPantas.firstOrNull { it.id == item.id }
                                    if (userYangPantas.isEmpty()) {
                                        userYangPantas.add(user!!)
                                    } else if (userSudahAda == null) {
                                        userYangPantas.add(user!!)
                                    }
                                }
                                //cari user yang pantas, yaitu user yang tidak redundant dalam setiap grup yang belum approved di sini masih ada user yang merupakan user private group
                            }
                            getAllRoomsGroup()
                        }
                    }
                }

                override fun onFailure(call: Call<Users>, t: Throwable) {
                    Toast.makeText(
                        requireActivity(),
                        "Silahkan periksa koneksi",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }
}
