package id.ac.ipb.mobile.digitani.fragment.konsultasi_grup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import id.ac.ipb.mobile.digitani.R
import id.ac.ipb.mobile.digitani.api.ApiClientChatNew
import id.ac.ipb.mobile.digitani.api.ApiInterface
import id.ac.ipb.mobile.digitani.databinding.FragmentGroupAddAndEditImplementationBinding
import id.ac.ipb.mobile.digitani.helper.SessionManager
import id.ac.ipb.mobile.digitani.model.*
import id.ac.ipb.mobile.digitani.response.ChatMemberPostResponse
import id.ac.ipb.mobile.digitani.response.ChatRoomMemberData
import id.ac.ipb.mobile.digitani.response.ChatRoomMemberResponse
import id.ac.ipb.mobile.digitani.response.ResponseChatRoomPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.CoroutineContext

//di kelas ini adalah tempat untuk menghapus,membuat, dan mengedit grup yang sudah ada.
//kalo mau bikin, bikin langsung bikin admin yang bikin menjadi admin langsung update jadi approve
//kalo mau ngedit, ya cuma ngedit aja taro isinya di dalam edit text nya. Kalo mau delete harus hapus seluruh message di dalam ruangannya
class GroupAddAndEditImplementation : Fragment(), CoroutineScope, View.OnClickListener {
    private var _binding: FragmentGroupAddAndEditImplementationBinding? = null
    private val binding get() = _binding!!
    private lateinit var job: Job
    private lateinit var apiService: ApiInterface
    private var addAtauEdit: String? = null
    private lateinit var messageAllInRoom: ArrayList<MessageIsi>
    private var ruangChat: RuangChat? = null
    private lateinit var arrayListOfAllRoomMembers: ArrayList<ChatRoomMemberData>
    private lateinit var userYangNgubahTerakhir: User
    private lateinit var sessionManager: SessionManager

    companion object {
        var RUANG_CHAT = "RUANG CHAT"
        var ADD_OR_EDIT = "ADD_OR_EDIT"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGroupAddAndEditImplementationBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        messageAllInRoom = arrayListOf()
        arrayListOfAllRoomMembers = arrayListOf()
        userYangNgubahTerakhir = User(0, "", "", "", "")
        job = Job()
        var title: String? = null
        sessionManager = SessionManager(requireActivity())
        if (arguments != null) {
            addAtauEdit = arguments?.getString(ADD_OR_EDIT)
            if (addAtauEdit == "EDIT") {
                title = "Update Grup"
                ruangChat = arguments?.getParcelable(RUANG_CHAT)
                binding.etDeskripsiGrup.setText(ruangChat?.description)
                binding.deleteGroupConsultation.visibility = View.VISIBLE
                binding.etNamaGrup.setText(ruangChat?.name)
            } else {
                title = "Tambah Grup"
            }
            binding.addAndEditConsultasionGroup.text = title
        }
        binding.textView7.text=title
        binding.addAndEditConsultasionGroup.setOnClickListener(this)
        binding.deleteGroupConsultation.setOnClickListener(this)
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    //untuk ngapus all members in one room sebelumnya hapus pesan dulu disemua grup
    fun getAllMembersInGroup() {
        val detailUser = sessionManager.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        var idUser = detailUser[SessionManager.KEY_ID_USER] as Int
        arrayListOfAllRoomMembers.clear()

        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.getAllMembersInOneRoom("Bearer $token", ruangChat?.id.toString())
            .enqueue(object : Callback<ChatRoomMemberResponse> {
                override fun onResponse(
                    call: Call<ChatRoomMemberResponse>,
                    response: Response<ChatRoomMemberResponse>
                ) {
                    if (response.isSuccessful) {
                        val isi = response.body()?.chat_members
                        if (isi != null) {
                            for (item in isi) {
                                if (item.chat_room_id == ruangChat!!.id) {
                                    arrayListOfAllRoomMembers.add(item)
                                }
                                //Toast.makeText(this@RuangChatPribadi,"$arrayListOfIsiRoomChat",Toast.LENGTH_SHORT).show()

                            }
                        }
                        getAllMessagesInGroup()
                    }
                }

                override fun onFailure(
                    call: Call<ChatRoomMemberResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(
                        requireActivity(),
                        "Gagal mengambil data",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

    }

    private fun getAllMessagesInGroup() {
        //ngambil semua message di grup, lalu hapus semuanya
        val detailUser = sessionManager!!.userDetails()
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        messageAllInRoom.clear()
        //ambil semua message
        //filter room yang sesuai
        apiService.getAllMessage("Bearer $token")
            .enqueue(object : Callback<GetChatMessages> {
                override fun onResponse(
                    call: Call<GetChatMessages>,
                    response: Response<GetChatMessages>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            messageAllInRoom = response.body()?.chat_messages!!
                            //ambil semua pesan
                            for (message in messageAllInRoom) {
                                var messageDihapus =
                                    arrayListOfAllRoomMembers.firstOrNull { it.id == message.chat_member_id }
                                //semua pesan dalam suatu ruangan bisa diambil cuman pakai semua member dalam ruangan tersebut
                                if (messageDihapus != null) {
                                    hapusPesan(message.id)
                                }
                                if (message == messageAllInRoom[messageAllInRoom.size - 1]) {
                                    for (item in arrayListOfAllRoomMembers) {
                                        if (item != arrayListOfAllRoomMembers[arrayListOfAllRoomMembers.size - 1]) {
                                            hapusMember(item.id.toString(),false)
                                        }else{
                                            hapusMember(item.id.toString(),true)
                                        }

                                    }
                                }

                            }

                        }
                    }
                }

                override fun onFailure(call: Call<GetChatMessages>, t: Throwable) {

                }
            })
    }

    private fun hapusMember(idMember: String,terakhirDihapus:Boolean){
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteMember("Bearer $token", idMember)
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    Toast.makeText(
                        requireActivity(),
                        "Member Berhasil Dihapus,yaitu $idMember",
                        Toast.LENGTH_LONG
                    ).show()
                    if(terakhirDihapus){
                        deleteRoom()
                    }
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(requireActivity(), "Gagal menghapus member", Toast.LENGTH_LONG)
                        .show()
                }

            })


    }

    private fun hapusPesan(messageId: Int) {
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientChatNew.chatRoomApiClient

        apiService.deleteMessage("Bearer $token", messageId)
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    var result = response.body()?.result
                    if (result == 1) {/*
                        Toast.makeText(
                            requireActivity(),
                            "Berhasil menghapus pesan, yaitu $messageId",
                            Toast.LENGTH_SHORT
                        ).show()
                        */

                    } else {

                        Toast.makeText(
                            requireActivity(),
                            "Gagal menghapus Pesan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    Toast.makeText(requireActivity(), "Gagal mengambil data", Toast.LENGTH_LONG)
                        .show()
                }

            })
    }

    private fun showDialog(keterangan:String,namaGrup:String?,deskripsiGrup:String?) {
        var dialogMessage: String? = null
        var dialogTitle: String? = null
        when (keterangan) {
            "tambahGrup" -> {
                dialogTitle="Tambah Grup Konsultasi"
                dialogMessage="Apakah anda yakin ingin menambahkan grup konsultasi baru? Anda akan langsung menjadi admin di grup tersebut."
            }
            "updateGrup" -> {
                dialogTitle="Update Grup Konsultasi"
                dialogMessage="Apakah anda ingin mengubah data grup konsultasi?"
            }
            "hapusGrup" -> {
                dialogTitle="Hapus Grup Konsultasi"
                dialogMessage="Apakah anda ingin menghapus grup Konsultasi ini? Semua anggota grup ini akan dikeluarkan dan semua pesan akan terhapus."
            }
        }
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setTitle(dialogTitle)
        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton("Ya") { _, _ ->
                binding.progressbarGroup.visibility=View.VISIBLE
                //ngapus semua pesan di grup dan member

                if(keterangan=="tambahGrup"){
                    tambahRoom(namaGrup!!,deskripsiGrup!!)
                }else if(keterangan=="updateGrup"){
                    updateRoom(namaGrup!!,deskripsiGrup!!)
                }else if(keterangan=="hapusGrup") {
                    getAllMembersInGroup()
                }
                    //postPermissionToAdmin(ruangChat)
            }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onClick(v: View?) {
        if (v != null) {
            if (v.id == R.id.add_and_edit_consultasion_group) {

                if (binding.etNamaGrup.text?.isEmpty() == true) {
                    binding.etNamaGrup.error = "Nama Grup Tidak Boleh Kosong"
                    return
                } else if (binding.etDeskripsiGrup.text?.isEmpty() == true) {
                    binding.etDeskripsiGrup.error = "Deskripsi Grup Tidak Boleh Kosong"
                    return
                }
                var namaGrup = binding.etNamaGrup.text.toString()
                var deskripsiGrup = binding.etDeskripsiGrup.text.toString()

                if (addAtauEdit == "EDIT") {
                    showDialog("updateGrup",namaGrup,deskripsiGrup)
                } else {
                    showDialog("tambahGrup",namaGrup,deskripsiGrup)
                    //add room langsung assign dia sendiri sebagai member di ruangan itu langsung approved
                }

            } else if (v.id == R.id.delete_group_consultation) {
                //harus dialog
                //cari semua pesan di ruangan ini
                //ini ngambil semua pesan dan hapus semua pesan di dalam grup dan ambil semua member dan hapus
                showDialog("hapusGrup",null,null)
            }
        }
    }

    private fun tambahRoom(nama: String, deskripsi: String) {
        var idRuanganYangBerhasilDitambah: String? = null
        var detailUser = sessionManager.userDetails()
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.createRoom("Bearer $token", nama, "CHAT_ROOM:GROUP", deskripsi)
            .enqueue(object : Callback<ResponseChatRoomPost> {
                override fun onResponse(
                    call: Call<ResponseChatRoomPost>,
                    response: Response<ResponseChatRoomPost>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireActivity(),
                            "Ruangan berhasil ditambahkan",
                            Toast.LENGTH_LONG
                        ).show()
                        var grupYangTerbentuk = response.body()?.chat_room
                        if (grupYangTerbentuk != null) {
                            idRuanganYangBerhasilDitambah = grupYangTerbentuk.id.toString()
                            assignUserAsMember(idRuanganYangBerhasilDitambah!!)
                        }
                    }

                }

                override fun onFailure(call: Call<ResponseChatRoomPost>, t: Throwable) {

                }

            })
    }

    private fun assignUserAsMember(idChatRoom: String) {
        var memberResponse: ChatRoomMemberData
        var detailUser = sessionManager.userDetails()
        val idUser = detailUser[SessionManager.KEY_ID_USER] as Int
        var idMemberUser: Int = -1
        //jika id==idUser type=CHAT_MEMBER:REGULAR
        val token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.postMember(
            "Bearer $token",
            idUser.toString(),
            idChatRoom,
            "CHAT_MEMBER:ADMIN"
        )
            .enqueue(object : Callback<ChatMemberPostResponse> {
                override fun onResponse(
                    call: Call<ChatMemberPostResponse>,
                    response: Response<ChatMemberPostResponse>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            memberResponse = response.body()!!.chat_member

                            idMemberUser = memberResponse.id
                            updateMemberIsApproved(idMemberUser, idUser, idChatRoom)
                        }
                    }
                }

                override fun onFailure(call: Call<ChatMemberPostResponse>, t: Throwable) {
                    Toast.makeText(
                        requireActivity(),
                        "gagal membuat chat room members",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun updateMemberIsApproved(id: Int, idUser: Int, idChatRoom: String) {
        var detail = sessionManager.userDetails()
        var token = detail[SessionManager.KEY_TOKEN_JWT].toString()

        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.updateMember(
            "Bearer $token",
            id,
            idUser.toString(),
            idChatRoom,
            "true",
            "CHAT_MEMBER:ADMIN"
        )
            .enqueue(object : Callback<ChatMemberPostResponse> {
                override fun onResponse(
                    call: Call<ChatMemberPostResponse>,
                    response: Response<ChatMemberPostResponse>
                ) {

                    Toast.makeText(
                        requireActivity(),
                        "Anda sekarang adalah admin.",
                        Toast.LENGTH_SHORT
                    ).show()
                    var fragment = GrupAddAndEditFragment()
                    binding.progressbarGroup.visibility=View.GONE
                    var mBundle = Bundle()
                    mBundle.putString(GrupAddAndEditFragment.Refresh, "refresh")
                    fragment.arguments = mBundle
                    var transaction = requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment, fragment)
                    transaction.commit()
                    //getAllRoomsMember()
                }

                override fun onFailure(call: Call<ChatMemberPostResponse>, t: Throwable) {

                    binding.progressbarGroup.visibility=View.GONE
                    Toast.makeText(
                        requireActivity(),
                        "Tidak berhasil menjadikan anda Admin dalam grup. Silahkan coba lagi.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

    }

    private fun deleteRoom() {
        var detailUser = sessionManager.userDetails()
        var result: Int = 0
        var token = detailUser[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientChatNew.chatRoomApiClient
        apiService.deleteRoom("Bearer $token", ruangChat?.id.toString())
            .enqueue(object : Callback<Delete> {
                override fun onResponse(call: Call<Delete>, response: Response<Delete>) {
                    if (response.isSuccessful) {
                        result = response.body()!!.result
                        if (result == 1) {
                            Toast.makeText(
                                requireActivity(),
                                "Grup berhasil dihapus.",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.progressbarGroup.visibility=View.GONE
                            var mBundle = Bundle()
                            var fragment = GrupAddAndEditFragment()
                            mBundle.putString(GrupAddAndEditFragment.Refresh, "refresh")
                            fragment.arguments = mBundle
                            var transaction =
                                requireActivity().supportFragmentManager.beginTransaction()
                            transaction.replace(R.id.nav_host_fragment, fragment)
                            transaction.commit()
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Gagal menghapus grup",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }

                override fun onFailure(call: Call<Delete>, t: Throwable) {
                    binding.progressbarGroup.visibility=View.GONE
                    Toast.makeText(
                        requireActivity(),
                        "Gagal menghapus grup",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun updateRoom(name: String?, description: String?) {
        var userDetails = sessionManager.userDetails()
        var chatRoomId = ruangChat?.id
        var token = userDetails[SessionManager.KEY_TOKEN_JWT].toString()
        apiService = ApiClientChatNew.chatRoomApiClient
        if (name != null && description != null && chatRoomId != null) {
            apiService.updateRoom("Bearer $token", chatRoomId, name, description, "CHAT_ROOM:GROUP")
                .enqueue(object : Callback<ResponseChatRoomPost> {
                    override fun onResponse(
                        call: Call<ResponseChatRoomPost>,
                        response: Response<ResponseChatRoomPost>
                    ) {
                        Toast.makeText(
                            requireActivity(),
                            "berhasil mengupdate grup",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressbarGroup.visibility=View.GONE
                        var mBundle = Bundle()
                        mBundle.putString(GrupAddAndEditFragment.Refresh, "refresh")
                        var fragment = GrupAddAndEditFragment()
                        fragment.arguments = mBundle
                        var transaction =
                            requireActivity().supportFragmentManager.beginTransaction()
                        transaction.replace(R.id.nav_host_fragment, fragment)
                        transaction.commit()
                    }

                    override fun onFailure(call: Call<ResponseChatRoomPost>, t: Throwable) {
                        binding.progressbarGroup.visibility=View.GONE
                        Toast.makeText(
                            requireActivity(),
                            "Gagal mengupdate grup",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
        }
    }
}
